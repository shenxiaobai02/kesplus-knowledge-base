package com.kes.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图谱检索结果
 * 用于封装图谱检索返回的结果
 */
@Data
public class GraphRetrievalResult {

    /**
     * 节点UUID
     */
    private String nodeUuid;

    /**
     * 知识库UUID
     */
    private String kbUuid;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点内容
     */
    private String content;

    /**
     * 关系路径深度
     */
    private int depth;

    /**
     * 关系权重分数
     */
    private double score;

    /**
     * 来源节点UUID列表（关系路径）
     */
    private java.util.List<String> pathNodes;

    /**
     * 元数据
     */
    private String metadataJson;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 从节点实体创建检索结果
     */
    public static GraphRetrievalResult fromNode(KnowledgeBaseGraphNode node, double score, int depth) {
        GraphRetrievalResult result = new GraphRetrievalResult();
        result.setNodeUuid(node.getUuid());
        result.setKbUuid(node.getKbUuid());
        result.setNodeType(node.getNodeType());
        result.setContent(node.getContent());
        result.setScore(score);
        result.setDepth(depth);
        result.setMetadataJson(node.getMetadataJson());
        result.setCreatedTime(node.getCreatedTime());
        return result;
    }
}