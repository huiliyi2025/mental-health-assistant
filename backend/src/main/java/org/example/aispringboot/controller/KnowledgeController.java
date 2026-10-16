package org.example.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.DTO.command.KnowledgeArticleCommandDTO;
import org.example.aispringboot.DTO.response.KnowledgeArticleResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.example.aispringboot.service.KnowledgeArticleService;
import org.example.aispringboot.service.KnowledgeCategoryService;
import org.example.aispringboot.service.UserService;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 知识文章接口
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeCategoryService categoryService;
    private final KnowledgeArticleService articleService;
    private final UserService userService;

    /**
     * 分类列表（公开）
     */
    @GetMapping("/category/tree")
    public Result<List<KnowledgeCategory>> categoryTree() {
        return Result.ok(categoryService.listAll());
    }

    /**
     * 文章分页（公开）
     */
    @GetMapping("/article/page")
    public Result<Page<KnowledgeArticleResponseDTO>> articlePage(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer currentPage,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        Integer pn = pageNum != null ? pageNum : currentPage;
        Integer ps = pageSize != null ? pageSize : size;
        return Result.ok(articleService.pageArticles(pn, ps, title, categoryId, status, sortField, sortDirection));
    }

    /**
     * 文章详情（公开）
     */
    @GetMapping("/article/{id}")
    public Result<KnowledgeArticleResponseDTO> articleDetail(@PathVariable String id) {
        return Result.ok(articleService.getDetail(id));
    }

    /**
     * 新增文章（管理员）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/article")
    public Result<String> createArticle(@Valid @RequestBody KnowledgeArticleCommandDTO command) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        return Result.ok(articleService.create(command, userId, currentAuthorName(userId)));
    }

    /**
     * 更新文章（管理员）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/article/{id}")
    public Result<Void> updateArticle(@PathVariable String id,
                                      @Valid @RequestBody KnowledgeArticleCommandDTO command) {
        articleService.update(id, command);
        return Result.ok();
    }

    /**
     * 变更文章状态（管理员）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/article/{id}/status")
    public Result<Void> changeStatus(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        articleService.changeStatus(id, body.get("status"));
        return Result.ok();
    }

    /**
     * 删除文章（管理员）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@PathVariable String id) {
        articleService.delete(id);
        return Result.ok();
    }

    private String currentAuthorName(Long userId) {
        try {
            return userService.getUserById(userId).getDisplayName();
        } catch (Exception e) {
            return "管理员";
        }
    }
}
