package org.example.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.DTO.command.EmotionDiaryCommandDTO;
import org.example.aispringboot.DTO.response.EmotionDiaryResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.service.EmotionDiaryService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 情绪日记接口
 */
@RestController
@RequestMapping("/api/emotion-diary")
@RequiredArgsConstructor
public class EmotionDiaryController {

    private final EmotionDiaryService diaryService;

    /**
     * 提交情绪日记（当前登录用户）
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody EmotionDiaryCommandDTO command) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        diaryService.create(userId, command);
        return Result.ok();
    }

    /**
     * 管理端分页查询
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/page")
    public Result<Page<EmotionDiaryResponseDTO>> adminPage(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer currentPage,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String moodScreRange) {
        Integer pn = firstNonNull(pageNum, currentPage, current);
        Integer ps = firstNonNull(pageSize, size);
        return Result.ok(diaryService.adminPage(pn, ps, userId, moodScreRange));
    }

    /**
     * 管理端删除记录
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        diaryService.delete(id);
        return Result.ok();
    }

    private Integer firstNonNull(Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
