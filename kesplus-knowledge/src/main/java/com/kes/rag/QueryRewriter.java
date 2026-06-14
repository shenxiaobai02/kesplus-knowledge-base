package com.kes.rag;

import com.kes.dto.QueryContext;
import com.kes.entity.RewriteResult;
import com.kes.enums.QueryIntent;

import java.util.Set;

/**
 * 查询改写器接口
 * <p>
 * 定义查询改写的核心能力，根据不同意图采用不同的改写策略。
 * 实现类可以根据不同的策略提供不同的改写效果。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
public interface QueryRewriter {

    /**
     * 改写查询
     * <p>
     * 根据识别到的意图，对原始查询进行改写，生成更适合检索的查询语句。
     * 改写策略包括：
     * - 同义词扩展
     * - 指代消解
     * - 结构化转换
     * - 实体补全
     * - 上下文补全
     * </p>
     *
     * @param query  原始查询
     * @param intent 识别到的意图
     * @param context 查询上下文
     * @return 改写结果
     */
    RewriteResult rewrite(String query, QueryIntent intent, QueryContext context);

    /**
     * 获取支持的意图类型
     *
     * @return 支持的意图类型集合
     */
    Set<QueryIntent> supportedIntents();

    /**
     * 检查改写器是否可用
     *
     * @return true if enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }
}
