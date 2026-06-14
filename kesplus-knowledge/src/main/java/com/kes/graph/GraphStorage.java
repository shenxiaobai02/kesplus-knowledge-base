package com.kes.graph;

import com.kes.entity.KnowledgeBaseGraphNode;
import com.kes.entity.KnowledgeBaseGraphEdge;

import java.util.List;

/**
 * 图谱存储接口
 * 支持无缝切换Neo4j和PostgreSQL存储方案
 */
public interface GraphStorage {

    /**
     * 创建节点
     *
     * @param node 节点实体
     */
    void createNode(KnowledgeBaseGraphNode node);

    /**
     * 批量创建节点
     *
     * @param nodes 节点列表
     */
    void batchCreateNodes(List<KnowledgeBaseGraphNode> nodes);

    /**
     * 创建关系
     *
     * @param edge 关系实体
     */
    void createEdge(KnowledgeBaseGraphEdge edge);

    /**
     * 批量创建关系
     *
     * @param edges 关系列表
     */
    void batchCreateEdges(List<KnowledgeBaseGraphEdge> edges);

    /**
     * 根据内容检索节点
     *
     * @param kbUuid 知识库UUID
     * @param query  查询内容
     * @param limit  返回数量限制
     * @return 节点列表
     */
    List<KnowledgeBaseGraphNode> searchNodes(String kbUuid, String query, int limit);

    /**
     * 获取相关节点（基于关系）
     *
     * @param nodeUuid 起始节点UUID
     * @param depth    关系深度
     * @param limit    返回数量限制
     * @return 相关节点列表
     */
    List<KnowledgeBaseGraphNode> getRelatedNodes(String nodeUuid, int depth, int limit);

    /**
     * 删除知识库所有图谱数据
     *
     * @param kbUuid 知识库UUID
     */
    void deleteByKbUuid(String kbUuid);

    /**
     * 统计节点数量
     *
     * @param kbUuid 知识库UUID
     * @return 节点数量
     */
    int countNodesByKbUuid(String kbUuid);

    /**
     * 根据UUID获取节点
     *
     * @param nodeUuid 节点UUID
     * @return 节点实体
     */
    KnowledgeBaseGraphNode getNodeByUuid(String nodeUuid);

    /**
     * 根据外部引用ID获取节点
     *
     * @param kbUuid 知识库UUID
     * @param nodeId 外部引用ID
     * @return 节点实体
     */
    KnowledgeBaseGraphNode getNodeByNodeId(String kbUuid, String nodeId);

    /**
     * 获取节点的所有关系
     *
     * @param nodeUuid 节点UUID
     * @return 关系列表
     */
    List<KnowledgeBaseGraphEdge> getEdgesByNodeUuid(String nodeUuid);
}