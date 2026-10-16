package org.example.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项响应 DTO。
 */
@Data
@Builder
public class ConsultationSessionResponseDTO {

    private Long id;

    private Long userId;

    private String userNickname;

    private String sessionTitle;

    private LocalDateTime startedAt;

    /** 会话时长（分钟）：从开始到最后一条消息 */
    private Long durationMinutes;

    /** 消息数量 */
    private Integer messageCount;

    /** 最后一条消息内容 */
    private String lastMessageContent;

    /** 最后一条消息时间 */
    private LocalDateTime lastMessageTime;
}
