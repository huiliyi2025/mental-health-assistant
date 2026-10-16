package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 安慰邮件发送记录（用于限制发送频率）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("emotion_mail_log")
public class EmotionMailLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("session_id")
    private Long sessionId;

    private String email;

    @TableField("primary_emotion")
    private String primaryEmotion;

    @TableField("emotion_score")
    private Integer emotionScore;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("sent_at")
    private LocalDateTime sentAt;
}
