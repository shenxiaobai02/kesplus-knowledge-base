package com.kes.rag;

import com.kes.dto.QueryContext;
import com.kes.enums.QueryIntent;

import java.util.List;

/**
 * 查询增强器接口
 * <p>
 * 定义查询增强的核心能力，包括查询改写、查询扩展和意图识别。
 * 实现类可以根据不同的策略提供不同的增强效果。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
public interface QueryEnhancer {

    /**
     * 增强查询
     * <p>
     * 将原始查询进行改写和扩展，生成多个增强后的查询语句。
     * 增强策略包括：
     * - 查询改写：将模糊问题转化为精确查询
     * - 同义词扩展：添加关键词的同义词
     * - 相关概念：添加相关领域的概念
     * </p>
     *
     * @param originalQuery 原始查询语句
     * @param context       查询上下文
     * @return 增强后的查询列表
     */
    List<String> enhance(String originalQuery, QueryContext context);

    /**
     * 识别查询意图
     * <p>
     * 根据查询内容和上下文，识别用户查询的意图类型。
     * 意图类型包括：事实性、流程性、比较性、分析性、创意性、未知。
     * </p>
     *
     * @param query 查询语句
     * @return 识别的意图类型
     */
    QueryIntent recognizeIntent(String query);

    /**
     * 检查查询增强是否可用
     *
     * @return true if enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }
}
