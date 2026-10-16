package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.entity.ConsultationMessage;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.mapper.ConsultationMessageMapper;
import org.example.aispringboot.mapper.ConsultationSessionMapper;
import org.example.aispringboot.mapper.EmotionDiaryMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据分析服务：为管理后台仪表盘提供概览数据。
 */
@Service
@RequiredArgsConstructor
public class DataAnalyticsService {

    private static final int TREND_DAYS = 7;

    private final UserMapper userMapper;
    private final ConsultationSessionMapper sessionMapper;
    private final ConsultationMessageMapper messageMapper;
    private final EmotionDiaryMapper diaryMapper;

    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(TREND_DAYS - 1L);
        LocalDateTime startTime = start.atStartOfDay();

        Long totalUsers = userMapper.selectCount(null);
        Long totalSessions = sessionMapper.selectCount(null);
        Long totalDiaries = diaryMapper.selectCount(null);

        List<User> recentUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, startTime));
        List<ConsultationSession> recentSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ConsultationSession>().ge(ConsultationSession::getStartedAt, startTime));
        List<EmotionDiary> recentDiaries = diaryMapper.selectList(
                new LambdaQueryWrapper<EmotionDiary>().ge(EmotionDiary::getCreatedAt, startTime));
        List<EmotionDiary> diariesWithScore = diaryMapper.selectList(
                new LambdaQueryWrapper<EmotionDiary>().isNotNull(EmotionDiary::getMoodScore));

        double avgMood = diariesWithScore.stream()
                .mapToInt(EmotionDiary::getMoodScore)
                .average().orElse(0);

        long todayNewDiaries = recentDiaries.stream()
                .filter(d -> d.getDiaryDate() != null && today.equals(d.getDiaryDate()))
                .count();
        long todayNewSessions = recentSessions.stream()
                .filter(s -> s.getStartedAt() != null && today.equals(s.getStartedAt().toLocalDate()))
                .count();

        Set<Long> activeUserIds = new HashSet<>();
        recentSessions.forEach(s -> { if (s.getUserId() != null) activeUserIds.add(s.getUserId()); });
        recentDiaries.forEach(d -> { if (d.getUserId() != null) activeUserIds.add(d.getUserId()); });

        Map<String, Object> systemOverview = new LinkedHashMap<>();
        systemOverview.put("totalUsers", totalUsers);
        systemOverview.put("activeUsers", activeUserIds.size());
        systemOverview.put("totalDiaries", totalDiaries);
        systemOverview.put("todayNewDiaries", todayNewDiaries);
        systemOverview.put("totalSessions", totalSessions);
        systemOverview.put("todayNewSessions", todayNewSessions);
        systemOverview.put("avgMoodScore", Math.round(avgMood * 10) / 10.0);

        // 按天聚合
        List<Map<String, Object>> emotionTrend = new ArrayList<>();
        List<Map<String, Object>> consultationTrend = new ArrayList<>();
        List<Map<String, Object>> userActivity = new ArrayList<>();

        double totalDuration = 0;
        int durationCount = 0;
        for (ConsultationSession session : recentSessions) {
            ConsultationMessage last = messageMapper.selectOne(new LambdaQueryWrapper<ConsultationMessage>()
                    .eq(ConsultationMessage::getSessionId, session.getId())
                    .orderByDesc(ConsultationMessage::getCreatedAt)
                    .last("limit 1"));
            if (session.getStartedAt() != null && last != null && last.getCreatedAt() != null) {
                totalDuration += Duration.between(session.getStartedAt(), last.getCreatedAt()).toMinutes();
                durationCount++;
            }
        }
        long avgDuration = durationCount > 0 ? Math.round(totalDuration / durationCount) : 0;

        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate day = start.plusDays(i);
            String dayStr = day.toString();

            // 情绪趋势
            List<EmotionDiary> dayDiaries = recentDiaries.stream()
                    .filter(d -> d.getCreatedAt() != null && day.equals(d.getCreatedAt().toLocalDate()))
                    .toList();
            double dayAvgMood = dayDiaries.stream()
                    .filter(d -> d.getMoodScore() != null)
                    .mapToInt(EmotionDiary::getMoodScore)
                    .average().orElse(0);
            Map<String, Object> emotionItem = new HashMap<>();
            emotionItem.put("date", dayStr);
            emotionItem.put("avgMoodScore", Math.round(dayAvgMood * 10) / 10.0);
            emotionItem.put("recordCount", dayDiaries.size());
            emotionTrend.add(emotionItem);

            // 会话趋势
            List<ConsultationSession> daySessions = recentSessions.stream()
                    .filter(s -> s.getStartedAt() != null && day.equals(s.getStartedAt().toLocalDate()))
                    .toList();
            Set<Long> daySessionUsers = new HashSet<>();
            daySessions.forEach(s -> { if (s.getUserId() != null) daySessionUsers.add(s.getUserId()); });
            Map<String, Object> consultationItem = new HashMap<>();
            consultationItem.put("date", dayStr);
            consultationItem.put("sessionCount", daySessions.size());
            consultationItem.put("userCount", daySessionUsers.size());
            consultationTrend.add(consultationItem);

            // 用户活跃度
            long newUsers = recentUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && day.equals(u.getCreatedAt().toLocalDate()))
                    .count();
            Set<Long> diaryUsers = new HashSet<>();
            dayDiaries.forEach(d -> { if (d.getUserId() != null) diaryUsers.add(d.getUserId()); });
            Set<Long> allActive = new HashSet<>(daySessionUsers);
            allActive.addAll(diaryUsers);
            Map<String, Object> activityItem = new HashMap<>();
            activityItem.put("date", dayStr);
            activityItem.put("activeUsers", allActive.size());
            activityItem.put("newUsers", newUsers);
            activityItem.put("diaryUsers", diaryUsers.size());
            activityItem.put("consultationUsers", daySessionUsers.size());
            userActivity.add(activityItem);
        }

        Map<String, Object> consultationStats = new LinkedHashMap<>();
        consultationStats.put("totalSessions", totalSessions);
        consultationStats.put("avgDurationMinutes", avgDuration);
        consultationStats.put("dailyTrend", consultationTrend);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("systemOverview", systemOverview);
        result.put("consultationStats", consultationStats);
        result.put("emotionTrend", emotionTrend);
        result.put("userActivity", userActivity);
        return result;
    }
}
