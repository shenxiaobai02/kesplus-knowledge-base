package com.kes.entity;

import lombok.Data;

/**
 * 重排序结果
 * 用于封装重排序后的最终检索结果
 */
@Data
public class RerankedResult {

    /**
     * 内容UUID
     */
    private String uuid;

    /**
     * 知识库UUID
     */
    private String kbUuid;

    /**
     * 内容文本
     */
    private String content;

    /**
     * 原始分数
     */
    private double originalScore;

    /**
     * 重排序分数
     */
    private double rerankedScore;

    /**
     * 排序位置
     */
    private int rank;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 重排序原因（LLM重排序时使用）
     */
    private String reason;

    /**
     * 元数据JSON
     */
    private String metadataJson;

    /**
     * 从混合检索结果创建重排序结果
     */
    public static RerankedResult fromHybridResult(HybridRetrievalResult hybridResult, int rank) {
        RerankedResult result = new RerankedResult();
        result.setUuid(hybridResult.getUuid());
        result.setKbUuid(hybridResult.getKbUuid());
        result.setContent(hybridResult.getContent());
        result.setOriginalScore(hybridResult.getCombinedScore());
        result.setRerankedScore(hybridResult.getCombinedScore());
        result.setRank(rank);
        result.setSourceType(hybridResult.getSourceType());
        result.setMetadataJson(hybridResult.getMetadataJson());
        return result;
    }
}