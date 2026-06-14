package com.kes.entity;

import com.kes.enums.RewriteStrategy;
import lombok.Builder;
import lombok.Data;

/**
 * 查询改写结果
 * <p>
 * 包含查询改写的完整结果，包括改写后的查询、置信度、使用的策略和解释说明。
 * 用于记录和分析查询改写的效果，便于后续优化。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Data
@Builder
public class RewriteResult {

    /**
     * 改写后的查询
     */
    private String rewrittenQuery;

    /**
     * 置信度 (0.0-1.0)
     */
    private double confidence;

    /**
     * 使用的改写策略
     */
    private RewriteStrategy strategy;

    /**
     * 改写说明
     */
    private String explanation;

    /**
     * 原始查询
     */
    private String originalQuery;

    /**
     * 创建成功的改写结果
     */
    public static RewriteResult success(String originalQuery, String rewrittenQuery,
                                       RewriteStrategy strategy, double confidence) {
        return RewriteResult.builder()
                .originalQuery(originalQuery)
                .rewrittenQuery(rewrittenQuery)
                .strategy(strategy)
                .confidence(confidence)
                .explanation(strategy.getDescription())
                .build();
    }

    /**
     * 创建失败的改写结果
     */
    public static RewriteResult failed(String originalQuery, String reason) {
        return RewriteResult.builder()
                .originalQuery(originalQuery)
                .rewrittenQuery(originalQuery)
                .strategy(RewriteStrategy.DEFAULT)
                .confidence(0.0)
                .explanation(reason)
                .build();
    }

    /**
     * 判断改写是否有效
     */
    public boolean isEffective() {
        return confidence >= 0.7 && !originalQuery.equals(rewrittenQuery);
    }
}
