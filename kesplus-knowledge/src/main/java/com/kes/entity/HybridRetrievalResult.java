package com.kes.entity;

import lombok.Data;

/**
 * 混合检索结果
 * 用于封装向量检索和图谱检索合并后的结果
 */
@Data
public class HybridRetrievalResult {

    /**
     * 内容UUID（向量UUID或节点UUID）
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
     * 向量检索分数
     */
    private double vectorScore;

    /**
     * 图谱检索分数
     */
    private double graphScore;

    /**
     * 综合分数（加权后的分数）
     */
    private double combinedScore;

    /**
     * 来源类型：VECTOR, GRAPH, HYBRID
     */
    private String sourceType;

    /**
     * 元数据JSON
     */
    private String metadataJson;

    /**
     * 向量权重（默认0.6）
     */
    private double vectorWeight;

    /**
     * 图谱权重（默认0.4）
     */
    private double graphWeight;

    /**
     * 从向量检索结果创建
     */
    public static HybridRetrievalResult fromVector(KnowledgeBaseEmbedding embedding, double score) {
        HybridRetrievalResult result = new HybridRetrievalResult();
        result.setUuid(embedding.getUuid());
        result.setKbUuid(embedding.getKbUuid());
        result.setContent(embedding.getText());
        result.setVectorScore(score);
        result.setGraphScore(0);
        result.setSourceType("VECTOR");
        result.setMetadataJson(embedding.getMetadataJson());
        return result;
    }

    /**
     * 从图谱检索结果创建
     */
    public static HybridRetrievalResult fromGraph(GraphRetrievalResult graphResult) {
        HybridRetrievalResult result = new HybridRetrievalResult();
        result.setUuid(graphResult.getNodeUuid());
        result.setKbUuid(graphResult.getKbUuid());
        result.setContent(graphResult.getContent());
        result.setVectorScore(0);
        result.setGraphScore(graphResult.getScore());
        result.setSourceType("GRAPH");
        result.setMetadataJson(graphResult.getMetadataJson());
        return result;
    }

    /**
     * 计算综合分数
     */
    public void calculateCombinedScore(double vectorWeight, double graphWeight) {
        this.vectorWeight = vectorWeight;
        this.graphWeight = graphWeight;
        this.combinedScore = vectorScore * vectorWeight + graphScore * graphWeight;
        if (vectorScore > 0 && graphScore > 0) {
            this.sourceType = "HYBRID";
        }
    }
}