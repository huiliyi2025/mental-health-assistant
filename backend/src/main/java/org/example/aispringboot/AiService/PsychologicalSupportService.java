package org.example.aispringboot.AiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.service.ConsultationSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 心理疏导服务
 * 负责创建咨询会话、调用大模型进行流式对话，并持久化聊天记录。
 * 对话上下文由 Spring AI 的 MessageChatMemoryAdvisor 基于 ChatMemory 自动维护，
 * 通过传入 conversationId 区分不同会话的上下文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PsychologicalSupportService {

    private static final String SESSION_PREFIX = "session_";
    private static final long SESSION_EXPIRE_MILLIS = 86400000L; // 24 小时
    private static final String AI_MODEL_NAME = "openai";

    @Qualifier("open-ai")
    private final ChatClient chatClient;

    private final ConsultationSessionService consultationSessionService;
    private final ConsultationMessageService consultationMessageService;

    /**
     * 创建咨询会话，并保存首条用户消息
     */
    public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        ConsultationSession session = consultationSessionService.createSession(userId, createDTO);

        // 将初始用户消息保存到消息表
        consultationMessageService.saveUserMessage(session.getId(), createDTO.getInitialMessage(), null);

        return new StructOutPut.StreamChatSession(
                SESSION_PREFIX + session.getId(),
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + SESSION_EXPIRE_MILLIS,
                1,
                "ACTIVE"
        );
    }

    /**
     * 流式心理对话
     *
     * @param sessionId   形如 "session_123" 的会话标识
     * @param userMessage 用户本次输入的消息
     * @param userId      当前登录用户 ID
     * @param isAdmin     当前用户是否为管理员
     * @return 逐段返回 AI 回复内容的响应式流
     */
    public Flux<String> streamPsychologicalChat(String sessionId, String userMessage, Long userId, boolean isAdmin) {
        Long dbSessionId = extractSessionId(sessionId);
        if (dbSessionId == null) {
            return Flux.error(new BusinessException("会话ID格式错误"));
        }

        // 会话必须存在且当前用户有访问权限，否则直接返回明确错误
        try {
            consultationSessionService.getAccessibleSession(dbSessionId, userId, isAdmin);
        } catch (BusinessException e) {
            return Flux.error(e);
        }

        // 首条消息已在 startSession 中保存，这里避免重复入库
        if (!isInitialMessage(dbSessionId, userMessage)) {
            consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
        }

        String conversationId = "conversation_" + sessionId;
        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .system(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT)
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // 保存 AI 回复；即使保存失败也不应影响已展示给用户的回复
                    try {
                        consultationMessageService.saveAimessage(dbSessionId, fullResponse.toString(), AI_MODEL_NAME);
                    } catch (Exception e) {
                        log.error("保存 AI 回复失败, sessionId={}", sessionId, e);
                    }
                })
                .doOnError(error -> log.error("AI 对话流式响应失败, sessionId={}", sessionId, error));
    }

    /**
     * 判断当前消息是否为首条消息（避免与 startSession 重复保存）
     */
    private boolean isInitialMessage(Long dbSessionId, String userMessage) {
        if (consultationMessageService.getMessageCountBySessionId(dbSessionId) != 1) {
            return false;
        }
        ConsultationMessageResponseDTO lastMessage =
                consultationMessageService.getLastMessageBySessionId(dbSessionId);
        return lastMessage != null
                && lastMessage.getSenderType() != null
                && lastMessage.getSenderType() == 1
                && userMessage.equals(lastMessage.getContent());
    }

    /**
     * 从 "session_123" 中解析出数据库会话 ID
     */
    public Long extractSessionId(String sessionId) {
        if (sessionId == null || !sessionId.startsWith(SESSION_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(sessionId.substring(SESSION_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
