package org.example.aispringboot.controller;

import lombok.RequiredArgsConstructor;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.service.DataAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据分析接口
 */
@RestController
@RequestMapping("/api/data-analytics")
@RequiredArgsConstructor
public class DataAnalyticsController {

    private final DataAnalyticsService analyticsService;

    /**
     * 仪表盘概览数据
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(analyticsService.overview());
    }
}
