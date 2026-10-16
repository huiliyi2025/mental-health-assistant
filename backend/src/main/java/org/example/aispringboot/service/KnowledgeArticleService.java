package org.example.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.DTO.command.KnowledgeArticleCommandDTO;
import org.example.aispringboot.DTO.response.KnowledgeArticleResponseDTO;
import org.example.aispringboot.entity.KnowledgeArticle;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.KnowledgeArticleMapper;
import org.example.aispringboot.mapper.KnowledgeCategoryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识文章服务
 */
@Service
@RequiredArgsConstructor
public class KnowledgeArticleService {

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeCategoryMapper categoryMapper;

    /**
     * 分页查询文章。
     *
     * @param status        状态过滤，为空时默认只返回「已发布」(1)，避免未登录用户看到草稿
     * @param sortField     排序字段：publishedAt / readCount
     * @param sortDirection 排序方向：desc / asc
     */
    public Page<KnowledgeArticleResponseDTO> pageArticles(Integer pageNum, Integer pageSize,
                                                          String title, Long categoryId, Integer status,
                                                          String sortField, String sortDirection) {
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(title)) {
            wrapper.like(KnowledgeArticle::getTitle, title);
        }
        if (categoryId != null) {
            wrapper.eq(KnowledgeArticle::getCategoryId, categoryId);
        }
        wrapper.eq(KnowledgeArticle::getStatus, status != null ? status : 1);

        boolean asc = "asc".equalsIgnoreCase(sortDirection);
        if ("readCount".equals(sortField)) {
            wrapper.orderBy(true, asc, KnowledgeArticle::getReadCount);
        } else {
            wrapper.orderBy(true, asc, KnowledgeArticle::getPublishedAt)
                    .orderBy(true, asc, KnowledgeArticle::getUpdatedAt);
        }

        Page<KnowledgeArticle> page = articleMapper.selectPage(new Page<>(current, size), wrapper);
        Page<KnowledgeArticleResponseDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toResponseDTO).collect(Collectors.toList()));
        return result;
    }

    /**
     * 文章详情（阅读量 +1）
     */
    public KnowledgeArticleResponseDTO getDetail(String id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setReadCount((article.getReadCount() == null ? 0 : article.getReadCount()) + 1);
        articleMapper.updateById(article);
        return toResponseDTO(article);
    }

    /**
     * 新增文章
     */
    public String create(KnowledgeArticleCommandDTO command, Long userId, String authorName) {
        String id = StrUtil.isNotBlank(command.getId()) ? command.getId() : UUID.randomUUID().toString();
        KnowledgeArticle article = KnowledgeArticle.builder()
                .id(id)
                .title(command.getTitle())
                .categoryId(command.getCategoryId())
                .summary(command.getSummary())
                .tags(command.getTags())
                .coverImage(command.getCoverImage())
                .content(command.getContent())
                .authorId(userId)
                .authorName(authorName)
                .readCount(0)
                .status(command.getStatus() != null ? command.getStatus() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .publishedAt(command.getStatus() != null && command.getStatus() == 1 ? LocalDateTime.now() : null)
                .build();
        articleMapper.insert(article);
        return id;
    }

    /**
     * 更新文章
     */
    public void update(String id, KnowledgeArticleCommandDTO command) {
        KnowledgeArticle exist = articleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("文章不存在");
        }
        exist.setTitle(command.getTitle());
        exist.setCategoryId(command.getCategoryId());
        exist.setSummary(command.getSummary());
        exist.setTags(command.getTags());
        exist.setCoverImage(command.getCoverImage());
        exist.setContent(command.getContent());
        exist.setUpdatedAt(LocalDateTime.now());
        if (command.getStatus() != null) {
            applyStatus(exist, command.getStatus());
        }
        articleMapper.updateById(exist);
    }

    /**
     * 变更文章状态：0-草稿 1-已发布 2-已下线
     */
    public void changeStatus(String id, Integer status) {
        KnowledgeArticle exist = articleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("文章不存在");
        }
        applyStatus(exist, status);
        exist.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(exist);
    }

    private void applyStatus(KnowledgeArticle article, Integer status) {
        article.setStatus(status);
        if (status != null && status == 1 && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
    }

    /**
     * 删除文章
     */
    public void delete(String id) {
        if (articleMapper.selectById(id) == null) {
            throw new BusinessException("文章不存在");
        }
        articleMapper.deleteById(id);
    }

    private KnowledgeArticleResponseDTO toResponseDTO(KnowledgeArticle article) {
        String categoryName = null;
        if (article.getCategoryId() != null) {
            KnowledgeCategory category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                categoryName = category.getCategoryName();
            }
        }
        List<String> tagArray = StrUtil.isBlank(article.getTags())
                ? List.of()
                : Arrays.stream(article.getTags().split(","))
                        .map(String::trim)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toList());

        return KnowledgeArticleResponseDTO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .categoryId(article.getCategoryId())
                .categoryName(categoryName)
                .summary(article.getSummary())
                .tags(article.getTags())
                .tagArray(tagArray)
                .coverImage(article.getCoverImage())
                .content(article.getContent())
                .authorId(article.getAuthorId())
                .authorName(article.getAuthorName())
                .readCount(article.getReadCount())
                .status(article.getStatus())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .publishedAt(article.getPublishedAt())
                .build();
    }

    /**
     * 分类 id -> 名称 映射（备用）
     */
    public Map<Long, String> categoryNameMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getCategoryName, (a, b) -> a));
    }
}
