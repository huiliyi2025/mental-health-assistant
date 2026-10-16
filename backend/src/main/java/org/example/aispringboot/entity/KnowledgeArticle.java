package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识文章实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_article")
public class KnowledgeArticle {

    /** 文章ID（UUID，客户端可传） */
    @TableId(type = IdType.INPUT)
    private String id;

    private String title;

    @TableField("category_id")
    private Long categoryId;

    private String summary;

    /** 标签，逗号分隔 */
    private String tags;

    @TableField("cover_image")
    private String coverImage;

    private String content;

    @TableField("author_id")
    private Long authorId;

    @TableField("author_name")
    private String authorName;

    @TableField("read_count")
    private Integer readCount;

    /** 状态：0-草稿 1-已发布 2-已下线 */
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("published_at")
    private LocalDateTime publishedAt;
}
