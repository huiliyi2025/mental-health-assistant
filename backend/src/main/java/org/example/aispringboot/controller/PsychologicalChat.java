package org.example.aispringboot.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.AiService.EmotionAnalysisService;
import org.example.aispringboot.AiService.PsychologicalSupportService;
import org.example.aispringboot.AiService.StructOutPut;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.command.ConsultationStreamDTO;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.DTO.response.ConsultationSessionResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.service.ComfortMailService;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.service.ConsultationSessionService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 心理疏导对话接口
 * 提供会话创建、列表、历史消息、删除、情绪以及流式对话（SSE）等接口。
 * 管理员可查看/管理所有用户的会话；普通用户只能操作自己的会话。
 */
@RestController
@RequestMapping("/api/psychological-chat")
@RequiredArgsConstructor
public class PsychologicalChat {
    private final PsychologicalSupportService psychologicalSupportService;
    private final ConsultationSessionService consultationSessionService;
    private final ConsultationMessageService consultationMessageService;
    private final EmotionAnalysisService emotionAnalysisService;
    private final ComfortMailService comfortMailService;

    /**
     * 创建咨询会话
     */
    @PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        StructOutPut.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
        return Result.ok(session);
    }

    /**
     * 会话列表（分页）：管理员返回全部，普通用户仅返回自己的
     */
    @GetMapping("/sessions")
    public Result<Page<ConsultationSessionResponseDTO>> listSessions(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer currentPage,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer size) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        Integer pn = pageNum != null ? pageNum : currentPage;
        Integer ps = pageSize != null ? pageSize : size;
        return Result.ok(consultationSessionService.listSessions(userId, isAdmin(), pn, ps));
    }

    /**
     * 某个会话的历史消息（普通用户校验归属，管理员可看任意）
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ConsultationMessageResponseDTO>> listSessionMessages(@PathVariable Long sessionId) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        consultationSessionService.getAccessibleSession(sessionId, userId, isAdmin());
        return Result.ok(consultationMessageService.listMessagesBySessionId(sessionId));
    }

    /**
     * 删除某个会话（普通用户校验归属，管理员可删任意）
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        consultationSessionService.deleteAccessibleSession(sessionId, userId, isAdmin());
        return Result.ok();
    }

    /**
     * 会话的情绪分析（普通用户校验归属，管理员可看任意）
     * 调用大模型对对话做结构化情绪分析，并缓存结果；
     * 若判定为情绪低落，则异步向用户注册邮箱发送一封温暖诗句（每人每天限 1 封）。
     */
    @GetMapping("/session/{sessionId}/emotion")
    public Result<Map<String, Object>> getSessionEmotion(@PathVariable String sessionId) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        Long dbSessionId = psychologicalSupportService.extractSessionId(sessionId);
        if (dbSessionId == null) {
            try {
                dbSessionId = Long.parseLong(sessionId);
            } catch (NumberFormatException e) {
                throw new BusinessException("会话ID格式错误");
            }
        }
        ConsultationSession session = consultationSessionService.getAccessibleSession(dbSessionId, userId, isAdmin());
        Map<String, Object> emotion = emotionAnalysisService.analyzeSession(session);
        // 情绪低落则异步发送安慰邮件（不阻塞接口响应）
        comfortMailService.maybeSendComfortMail(session, emotion);
        return Result.ok(emotion);
    }

    /**
     * 流式对话（SSE）
     * 事件类型：message（AI 回复片段）、done（结束）。每条 message 的 data 为 JSON 字符串。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        Long userId = JwtTokenUtil.getCurrentUserId();

        return psychologicalSupportService
                .streamPsychologicalChat(streamDTO.getSessionId(), streamDTO.getUserMessage(), userId, isAdmin())
                .map(fragment -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(JSONUtil.toJsonStr(Result.ok(Map.of("content", fragment, "type", "normal"))))
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{}")
                        .build()))
                .delayElements(Duration.ofMillis(50));
    }

    /**
     * 当前登录用户是否为管理员
     */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
