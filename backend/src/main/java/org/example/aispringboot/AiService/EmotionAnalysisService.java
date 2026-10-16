package org.example.aispringboot.AiService;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.service.ConsultationSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 情绪分析服务
 * 读取一段咨询对话，交给大模型做结构化情绪分析，并缓存到会话记录上。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionAnalysisService {

    @Qualifier("open-ai")
    private final ChatClient chatClient;

    private final ConsultationMessageService consultationMessageService;
    private final ConsultationSessionService consultationSessionService;

    /**
     * 分析会话情绪（带缓存：若最后一条消息不晚于上次分析时间，直接复用缓存）
     */
    public Map<String, Object> analyzeSession(ConsultationSession session) {
        // 命中缓存则直接返回
        if (session.getLastEmotionAnalysis() != null && session.getLastEmotionUpdatedAt() != null) {
            ConsultationMessageResponseDTO last = consultationMessageService.getLastMessageBySessionId(session.getId());
            boolean upToDate = last == null || last.getCreatedAt() == null
                    || !last.getCreatedAt().isAfter(session.getLastEmotionUpdatedAt());
            if (upToDate) {
                return parseEmotion(session.getLastEmotionAnalysis());
            }
        }

        // 调用大模型分析
        Map<String, Object> emotion;
        try {
            List<ConsultationMessageResponseDTO> messages =
                    consultationMessageService.listMessagesBySessionId(session.getId());
            String prompt = PromptManage.EMOTION_ANALYSIS_PROMPT
                    .replace("{conversation}", buildConversation(messages));
            String content = chatClient.prompt().user(prompt).call().content();
            emotion = parseEmotion(content);
        } catch (Exception e) {
            log.error("情绪分析失败, sessionId={}", session.getId(), e);
            emotion = neutralEmotion();
        }

        // 缓存结果
        try {
            consultationSessionService.saveEmotionAnalysis(session.getId(), JSONUtil.toJsonStr(emotion));
        } catch (Exception e) {
            log.error("保存情绪分析结果失败, sessionId={}", session.getId(), e);
        }
        return emotion;
    }

    private String buildConversation(List<ConsultationMessageResponseDTO> messages) {
        StringBuilder sb = new StringBuilder();
        for (ConsultationMessageResponseDTO m : messages) {
            String who = (m.getSenderType() != null && m.getSenderType() == 1) ? "用户" : "AI心理助手";
            sb.append(who).append("：").append(m.getContent() == null ? "" : m.getContent()).append('\n');
        }
        return sb.toString();
    }

    /**
     * 从模型输出中解析出结构化情绪（从中截取第一个 JSON 对象）
     */
    private Map<String, Object> parseEmotion(String content) {
        if (content == null) {
            return neutralEmotion();
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return neutralEmotion();
        }
        try {
            JSONObject obj = JSONUtil.parseObj(content.substring(start, end + 1));

            List<String> tips = new ArrayList<>();
            JSONArray arr = obj.getJSONArray("improvementSuggestions");
            if (arr != null) {
                for (Object o : arr) {
                    tips.add(String.valueOf(o));
                }
            }

            Map<String, Object> emotion = new LinkedHashMap<>();
            emotion.put("primaryEmotion", obj.getStr("primaryEmotion", "中性"));
            emotion.put("emotionScore", clamp(obj.getInt("emotionScore", 50), 0, 100));
            emotion.put("isNegative", obj.getBool("isNegative", false));
            emotion.put("riskLevel", clamp(obj.getInt("riskLevel", 0), 0, 3));
            emotion.put("suggestion", obj.getStr("suggestion", ""));
            emotion.put("improvementSuggestions", tips);
            emotion.put("riskDescription", obj.getStr("riskDescription", ""));
            return emotion;
        } catch (Exception e) {
            log.warn("解析情绪分析结果失败: {}", content);
            return neutralEmotion();
        }
    }

    private Map<String, Object> neutralEmotion() {
        Map<String, Object> emotion = new LinkedHashMap<>();
        emotion.put("primaryEmotion", "中性");
        emotion.put("emotionScore", 50);
        emotion.put("isNegative", false);
        emotion.put("riskLevel", 0);
        emotion.put("suggestion", "情绪状态平稳");
        emotion.put("improvementSuggestions", new ArrayList<>());
        emotion.put("riskDescription", "");
        return emotion;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
