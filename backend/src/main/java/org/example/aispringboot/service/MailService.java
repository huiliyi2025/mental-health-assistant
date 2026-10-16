package org.example.aispringboot.service;

import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;

/**
 * 邮件发送服务（底层封装 JavaMailSender）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${app.mail.from-name:心理助手}")
    private String fromName;

    /**
     * 是否已完成发件邮箱配置（未配置时跳过发送，只记录日志）
     */
    public boolean isConfigured() {
        return StringUtils.hasText(from);
    }

    /**
     * 发送 HTML 邮件
     *
     * @return 是否发送成功
     */
    public boolean sendHtml(String to, String subject, String html) {
        if (!isConfigured()) {
            log.warn("未配置发件邮箱（spring.mail.username 为空），跳过邮件发送: to={}, subject={}", to, subject);
            return false;
        }
        if (!StringUtils.hasText(to)) {
            log.warn("收件人为空，跳过邮件发送");
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("安慰邮件发送成功: to={}", to);
            return true;
        } catch (Exception e) {
            log.error("安慰邮件发送失败: to={}", to, e);
            return false;
        }
    }
}
