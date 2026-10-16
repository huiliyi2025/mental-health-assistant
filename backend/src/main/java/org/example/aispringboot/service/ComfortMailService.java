package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.aispringboot.AiService.PoemLibrary;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.EmotionMailLog;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.mapper.EmotionMailLogMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 安慰邮件服务
 * 当情绪分析判定为「低落」时，向用户注册邮箱发送一封随机的温暖诗句。
 * 为避免打扰，每个用户每天最多发送 app.mail.daily-limit 封。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComfortMailService {

    /** 触发阈值：负面情绪强度达到该值也视为低落 */
    private static final int EMOTION_SCORE_THRESHOLD = 60;

    private final EmotionMailLogMapper mailLogMapper;
    private final UserMapper userMapper;
    private final MailService mailService;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Value("${app.mail.daily-limit:1}")
    private int dailyLimit;

    /**
     * 若情绪低落则异步发送安慰邮件（不阻塞调用方）
     */
    @Async
    public void maybeSendComfortMail(ConsultationSession session, Map<String, Object> emotion) {
        try {
            if (!enabled) {
                return;
            }
            if (!isLowMood(emotion)) {
                return;
            }

            Long userId = session.getUserId();
            // 频率限制：每天最多 N 封
            LocalDateTime dayStart = LocalDate.now().atStartOfDay();
            long sentToday = mailLogMapper.selectCount(new LambdaQueryWrapper<EmotionMailLog>()
                    .eq(EmotionMailLog::getUserId, userId)
                    .ge(EmotionMailLog::getSentAt, dayStart));
            if (sentToday >= dailyLimit) {
                log.debug("用户 {} 今日安慰邮件已达上限，跳过", userId);
                return;
            }

            User user = userMapper.selectById(userId);
            if (user == null || !StringUtils.hasText(user.getEmail())) {
                return;
            }

            PoemLibrary.Poem poem = PoemLibrary.random();
            String subject = "致此刻的你 · 一句话想对你说";
            boolean ok = mailService.sendHtml(user.getEmail(), subject, buildHtml(user, poem));
            if (ok) {
                mailLogMapper.insert(EmotionMailLog.builder()
                        .userId(userId)
                        .sessionId(session.getId())
                        .email(user.getEmail())
                        .primaryEmotion(str(emotion.get("primaryEmotion")))
                        .emotionScore(toInt(emotion.get("emotionScore")))
                        .riskLevel(toInt(emotion.get("riskLevel")))
                        .sentAt(LocalDateTime.now())
                        .build());
            }
        } catch (Exception e) {
            log.error("发送安慰邮件时出错, userId={}", session.getUserId(), e);
        }
    }

    /**
     * 情绪是否低落：负面情绪且（风险等级 ≥ 1 或情绪强度 ≥ 阈值）
     */
    private boolean isLowMood(Map<String, Object> emotion) {
        if (emotion == null) {
            return false;
        }
        boolean isNegative = Boolean.TRUE.equals(emotion.get("isNegative"))
                || "true".equalsIgnoreCase(String.valueOf(emotion.get("isNegative")));
        int score = toInt(emotion.get("emotionScore"));
        int risk = toInt(emotion.get("riskLevel"));
        return isNegative && (risk >= 1 || score >= EMOTION_SCORE_THRESHOLD);
    }

    private String buildHtml(User user, PoemLibrary.Poem poem) {
        String name = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
        return "<div style=\"max-width:560px;margin:0 auto;padding:32px 24px;font-family:'PingFang SC','Microsoft YaHei',sans-serif;background:#fdfaf6;border-radius:16px;color:#3f3a34;\">"
                + "<p style=\"font-size:16px;line-height:1.9;\">" + safe(name) + "，你好：</p>"
                + "<p style=\"font-size:15px;line-height:1.9;color:#6b6259;\">我们留意到，你最近似乎有些不太好过。想给你留一段话，不用急着回应，慢慢来就好。</p>"
                + "<div style=\"margin:24px 0;padding:24px;background:#fff;border-left:4px solid #f0a06a;border-radius:12px;\">"
                + "<p style=\"font-size:19px;line-height:2;margin:0 0 10px;letter-spacing:1px;color:#4a4238;\">" + safe(poem.content()) + "</p>"
                + "<p style=\"text-align:right;margin:0;color:#a89b8c;font-size:13px;\">—— " + safe(poem.author()) + "</p>"
                + "</div>"
                + "<p style=\"font-size:15px;line-height:1.9;color:#6b6259;\">" + safe(poem.warmWords()) + "</p>"
                + "<p style=\"font-size:15px;line-height:1.9;color:#6b6259;\">如果情绪一直很低落，也请记得向身边的人、或专业心理支持力量求助，你值得被认真对待。</p>"
                + "<p style=\"margin-top:28px;font-size:13px;color:#b3a89c;\">—— 金凤心理助手</p>"
                + "</div>";
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private int toInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return 0;
        }
    }
}
