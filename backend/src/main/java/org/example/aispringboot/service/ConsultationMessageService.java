package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.entity.ConsultationMessage;
import org.example.aispringboot.mapper.ConsultationMessageMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 咨询消息服务：消息的保存与查询
 */
@Service
@RequiredArgsConstructor
public class ConsultationMessageService {

    /** 发送者类型：用户 */
    private static final int SENDER_USER = 1;
    /** 发送者类型：AI 助手 */
    private static final int SENDER_AI = 2;
    /** 消息类型：文本 */
    private static final int TYPE_TEXT = 1;

    private final ConsultationMessageMapper consultationMessageMapper;

    public ConsultationMessage saveUserMessage(Long sessionId, String content, String emotionTag) {
        ConsultationMessage userMessage = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(SENDER_USER)
                .messageType(TYPE_TEXT)
                .content(content)
                .emotionTag(emotionTag)
                .createdAt(LocalDateTime.now())
                .build();
        consultationMessageMapper.insert(userMessage);
        return userMessage;
    }

    public ConsultationMessage saveAimessage(Long sessionId, String content, String aiModel) {
        ConsultationMessage message = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(SENDER_AI)
                .messageType(TYPE_TEXT)
                .content(content)
                .aiModel(aiModel)
                .createdAt(LocalDateTime.now())
                .build();
        consultationMessageMapper.insert(message);
        return message;
    }

    public Integer getMessageCountBySessionId(Long sessionId) {
        Long count = consultationMessageMapper.selectCount(new LambdaQueryWrapper<ConsultationMessage>()
                .eq(ConsultationMessage::getSessionId, sessionId));
        return count.intValue();
    }

    // 获取会话的最后一条消息
    public ConsultationMessageResponseDTO getLastMessageBySessionId(Long sessionId) {
        ConsultationMessage lastMessage = consultationMessageMapper.selectOne(
                new LambdaQueryWrapper<ConsultationMessage>()
                        .eq(ConsultationMessage::getSessionId, sessionId)
                        .orderByDesc(ConsultationMessage::getCreatedAt)
                        .last("limit 1")
        );
        return lastMessage != null ? convertToResponseDTO(lastMessage) : null;
    }

    // 按时间顺序获取某个会话的全部消息
    public List<ConsultationMessageResponseDTO> listMessagesBySessionId(Long sessionId) {
        List<ConsultationMessage> messages = consultationMessageMapper.selectList(
                new LambdaQueryWrapper<ConsultationMessage>()
                        .eq(ConsultationMessage::getSessionId, sessionId)
                        .orderByAsc(ConsultationMessage::getCreatedAt)
        );
        return messages.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }

    // 删除某个会话的全部消息
    public void deleteBySessionId(Long sessionId) {
        consultationMessageMapper.delete(new LambdaQueryWrapper<ConsultationMessage>()
                .eq(ConsultationMessage::getSessionId, sessionId));
    }

    private ConsultationMessageResponseDTO convertToResponseDTO(ConsultationMessage message) {
        if (message == null) {
            return null;
        }
        ConsultationMessageResponseDTO responseDTO = new ConsultationMessageResponseDTO();
        responseDTO.setId(message.getId());
        responseDTO.setSessionId(message.getSessionId());
        responseDTO.setSenderType(message.getSenderType());
        responseDTO.setMessageType(message.getMessageType());
        responseDTO.setContent(message.getContent());
        responseDTO.setEmotionTag(message.getEmotionTag());
        responseDTO.setAiModel(message.getAiModel());
        responseDTO.setCreatedAt(message.getCreatedAt());
        responseDTO.setSenderTypeDesc(message.getSenderTypeDesc());
        responseDTO.setMessageTypeDesc(message.getMessageTypeDesc());
        responseDTO.calculateContentLength();
        return responseDTO;
    }
}
