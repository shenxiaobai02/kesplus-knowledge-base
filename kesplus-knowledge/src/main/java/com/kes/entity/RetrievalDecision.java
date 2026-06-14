package com.kes.entity;

import com.kes.enums.QueryIntent;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 检索决策类
 * <p>
 * Self-RAG自评估器返回的决策结果，包含是否需要检索、检索关键词、意图等信息。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Data
public class RetrievalDecision {

    /**
     * 是否需要检索
     */
    private boolean needRetrieval;

    /**
     * 判断理由
     */
    private String reason;

    /**
     * 检索关键词列表
     */
    private List<String> keywords;

    /**
     * 识别到的查询意图
     */
    private QueryIntent intent;

    /**
     * 增强后的查询列表
     */
    private List<String> enhancedQueries;

    public RetrievalDecision() {
        this.keywords = new ArrayList<>();
        this.enhancedQueries = new ArrayList<>();
        this.intent = QueryIntent.UNKNOWN;
        this.needRetrieval = true;
    }

    /**
     * 创建一个需要检索的决策
     */
    public static RetrievalDecision needRetrieval(String reason, QueryIntent intent) {
        RetrievalDecision decision = new RetrievalDecision();
        decision.setNeedRetrieval(true);
        decision.setReason(reason);
        decision.setIntent(intent);
        return decision;
    }

    /**
     * 创建一个不需要检索的决策
     */
    public static RetrievalDecision noRetrieval(String reason) {
        RetrievalDecision decision = new RetrievalDecision();
        decision.setNeedRetrieval(false);
        decision.setReason(reason);
        decision.setIntent(QueryIntent.UNKNOWN);
        return decision;
    }

    /**
     * 添加检索关键词
     */
    public void addKeyword(String keyword) {
        if (this.keywords == null) {
            this.keywords = new ArrayList<>();
        }
        this.keywords.add(keyword);
    }

    /**
     * 添加增强查询
     */
    public void addEnhancedQuery(String query) {
        if (this.enhancedQueries == null) {
            this.enhancedQueries = new ArrayList<>();
        }
        this.enhancedQueries.add(query);
    }
}
