package org.example.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.DTO.command.EmotionDiaryCommandDTO;
import org.example.aispringboot.DTO.response.EmotionDiaryResponseDTO;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.EmotionDiaryMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 情绪日记服务
 */
@Service
@RequiredArgsConstructor
public class EmotionDiaryService {

    private final EmotionDiaryMapper diaryMapper;
    private final UserMapper userMapper;

    /**
     * 新增一条情绪日记
     */
    public void create(Long userId, EmotionDiaryCommandDTO command) {
        EmotionDiary diary = EmotionDiary.builder()
                .userId(userId)
                .diaryDate(command.getDiaryDate() != null ? command.getDiaryDate() : LocalDate.now())
                .moodScore(command.getMoodScore())
                .dominantEmotion(command.getDominantEmotion())
                .emotionTriggers(command.getEmotionTriggers())
                .diaryContent(command.getDiaryContent())
                .sleepQuality(command.getSleepQuality())
                .stressLevel(command.getStressLevel())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        diaryMapper.insert(diary);
    }

    /**
     * 管理端分页查询
     *
     * @param moodScreRange 评分范围，形如 "1-3" / "4-6" / "7-10"
     */
    public Page<EmotionDiaryResponseDTO> adminPage(Integer pageNum, Integer pageSize,
                                                   Long userId, String moodScreRange) {
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(EmotionDiary::getUserId, userId);
        }
        if (StrUtil.isNotBlank(moodScreRange)) {
            String[] parts = moodScreRange.split("-");
            if (parts.length == 2) {
                try {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    wrapper.between(EmotionDiary::getMoodScore, min, max);
                } catch (NumberFormatException ignored) {
                    // 非法范围直接忽略
                }
            }
        }
        wrapper.orderByDesc(EmotionDiary::getCreatedAt);

        Page<EmotionDiary> page = diaryMapper.selectPage(new Page<>(current, size), wrapper);
        Page<EmotionDiaryResponseDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toResponseDTO).collect(Collectors.toList()));
        return result;
    }

    /**
     * 删除记录
     */
    public void delete(Long id) {
        if (diaryMapper.selectById(id) == null) {
            throw new BusinessException("记录不存在");
        }
        diaryMapper.deleteById(id);
    }

    private EmotionDiaryResponseDTO toResponseDTO(EmotionDiary diary) {
        String username = null;
        String nickname = null;
        if (diary.getUserId() != null) {
            User user = userMapper.selectById(diary.getUserId());
            if (user != null) {
                username = user.getUsername();
                nickname = user.getDisplayName();
            }
        }
        return EmotionDiaryResponseDTO.builder()
                .id(diary.getId())
                .userId(diary.getUserId())
                .username(username)
                .nickname(nickname)
                .diaryDate(diary.getDiaryDate())
                .moodScore(diary.getMoodScore())
                .dominantEmotion(diary.getDominantEmotion())
                .emotionTriggers(diary.getEmotionTriggers())
                .diaryContent(diary.getDiaryContent())
                .sleepQuality(diary.getSleepQuality())
                .stressLevel(diary.getStressLevel())
                .aiEmotionAnalysis(diary.getAiEmotionAnalysis())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
