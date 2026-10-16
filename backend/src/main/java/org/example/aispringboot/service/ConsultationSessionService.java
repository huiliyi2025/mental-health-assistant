package org.example.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.DTO.response.ConsultationSessionResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.ConsultationSessionMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 咨询会话服务：创建、查询、删除会话。
 * 所有查询/删除都会校验会话归属，避免越权访问他人会话。
 */
@Service
@RequiredArgsConstructor
public class ConsultationSessionService {
    private static final DateTimeFormatter TITLE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final UserMapper userMapper;
    private final ConsultationSessionMapper consultationSessionMapper;
    private final ConsultationMessageService consultationMessageService;

    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 验证用户是否存在
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("用户不存在");
        }

        // 标题为空时生成默认标题
        String title = createDTO.getSessionTitle();
        if (StrUtil.isBlank(title)) {
            title = "温柔智能金凤 - " + LocalDateTime.now().format(TITLE_FORMATTER);
        }

        ConsultationSession session = ConsultationSession.builder()
                .userId(userId)
                .sessionTitle(title)
                .startedAt(LocalDateTime.now())
                .build();
        consultationSessionMapper.insert(session);
        return session;
    }

    /**
     * 分页查询会话列表。
     * 管理员可见全部用户的会话；普通用户只能看到自己的会话。
     *
     * @param isAdmin 当前登录用户是否为管理员
     */
    public Page<ConsultationSessionResponseDTO> listSessions(Long userId, boolean isAdmin,
                                                            Integer pageNum, Integer pageSize) {
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            wrapper.eq(ConsultationSession::getUserId, userId);
        }
        wrapper.orderByDesc(ConsultationSession::getStartedAt);

        Page<ConsultationSession> page = new Page<>(current, size);
        Page<ConsultationSession> result = consultationSessionMapper.selectPage(page, wrapper);

        Page<ConsultationSessionResponseDTO> dtoPage =
                new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    /**
     * 获取会话并校验访问权限：管理员可访问任意会话，普通用户只能访问自己的。
     * 不存在或无权限时抛业务异常。
     */
    public ConsultationSession getAccessibleSession(Long sessionId, Long userId, boolean isAdmin) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!isAdmin && !session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }
        return session;
    }

    /**
     * 删除会话及其消息（先校验访问权限）。
     */
    public void deleteAccessibleSession(Long sessionId, Long userId, boolean isAdmin) {
        getAccessibleSession(sessionId, userId, isAdmin);
        consultationMessageService.deleteBySessionId(sessionId);
        consultationSessionMapper.deleteById(sessionId);
    }

    /**
     * 保存会话的情绪分析结果（JSON）
     */
    public void saveEmotionAnalysis(Long sessionId, String analysisJson) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setLastEmotionAnalysis(analysisJson);
            session.setLastEmotionUpdatedAt(LocalDateTime.now());
            consultationSessionMapper.updateById(session);
        }
    }

    private ConsultationSessionResponseDTO toResponseDTO(ConsultationSession session) {
        Integer messageCount = consultationMessageService.getMessageCountBySessionId(session.getId());
        ConsultationMessageResponseDTO lastMessage =
                consultationMessageService.getLastMessageBySessionId(session.getId());

        Long durationMinutes = null;
        if (session.getStartedAt() != null && lastMessage != null && lastMessage.getCreatedAt() != null) {
            durationMinutes = Duration.between(session.getStartedAt(), lastMessage.getCreatedAt()).toMinutes();
        }

        String nickname = null;
        if (session.getUserId() != null) {
            User user = userMapper.selectById(session.getUserId());
            if (user != null) {
                nickname = user.getDisplayName();
            }
        }

        return ConsultationSessionResponseDTO.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .userNickname(nickname)
                .sessionTitle(session.getSessionTitle())
                .startedAt(session.getStartedAt())
                .durationMinutes(durationMinutes)
                .messageCount(messageCount)
                .lastMessageContent(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageTime(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .build();
    }
}
