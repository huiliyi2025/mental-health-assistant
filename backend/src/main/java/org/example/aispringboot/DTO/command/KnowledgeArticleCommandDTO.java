package org.example.aispringboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识文章新增/编辑命令
 */
@Data
public class KnowledgeArticleCommandDTO {

    /** 文章ID（可选，UUID；为空则后端生成） */
    private String id;

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题最多200个字符")
    private String title;

    private Long categoryId;

    @Size(max = 1000, message = "文章摘要最多1000个字符")
    private String summary;

    /** 标签，逗号分隔 */
    private String tags;

    private String coverImage;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    /** 状态：0-草稿 1-已发布 2-已下线（可选） */
    private Integer status;
}
