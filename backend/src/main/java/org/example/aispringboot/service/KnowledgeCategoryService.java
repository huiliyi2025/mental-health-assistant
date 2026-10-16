package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.example.aispringboot.mapper.KnowledgeCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识分类服务
 */
@Service
@RequiredArgsConstructor
public class KnowledgeCategoryService {

    private final KnowledgeCategoryMapper categoryMapper;

    /**
     * 全部分类（按排序）
     */
    public List<KnowledgeCategory> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<KnowledgeCategory>()
                .orderByAsc(KnowledgeCategory::getSortOrder)
                .orderByAsc(KnowledgeCategory::getId));
    }
}
