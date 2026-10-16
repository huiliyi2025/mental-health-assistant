package org.example.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识文章响应 DTO（列表与详情共用）
 */
@Data
@Builder
public class KnowledgeArticleResponseDTO {

    private String id;

    private String title;

    private Long categoryId;

    private String categoryName;

    private String summary;

    /** 标签，逗号分隔原始值 */
    private String tags;

    /** 标签数组（由 tags 拆分） */
    private List<String> tagArray;

    private String coverImage;

    private String content;

    private Long authorId;

    private String authorName;

    private Integer readCount;

    /** 0-草稿 1-已发布 2-已下线 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;
}
