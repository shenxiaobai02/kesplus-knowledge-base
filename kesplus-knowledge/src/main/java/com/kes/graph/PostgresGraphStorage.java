package com.kes.graph;

import com.kes.entity.KnowledgeBaseGraphNode;
import com.kes.entity.KnowledgeBaseGraphEdge;
import com.kes.mapper.GraphNodeMapper;
import com.kes.mapper.GraphEdgeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL图谱存储实现
 * 使用关系型数据库存储图谱数据
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rag.graph.storage-type", havingValue = "postgresql", matchIfMissing = true)
public class PostgresGraphStorage implements GraphStorage {

    @Autowired
    private GraphNodeMapper nodeMapper;

    @Autowired
    private GraphEdgeMapper edgeMapper;

    @Override
    public void createNode(KnowledgeBaseGraphNode node) {
        nodeMapper.insert(node);
        log.debug("Created graph node: {}", node.getUuid());
    }

    @Override
    public void batchCreateNodes(List<KnowledgeBaseGraphNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodeMapper.batchInsert(nodes);
        log.debug("Batch created {} graph nodes", nodes.size());
    }

    @Override
    public void createEdge(KnowledgeBaseGraphEdge edge) {
        edgeMapper.insert(edge);
        log.debug("Created graph edge: {} -> {}", edge.getSourceNodeUuid(), edge.getTargetNodeUuid());
    }

    @Override
    public void batchCreateEdges(List<KnowledgeBaseGraphEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return;
        }
        edgeMapper.batchInsert(edges);
        log.debug("Batch created {} graph edges", edges.size());
    }

    @Override
    public List<KnowledgeBaseGraphNode> searchNodes(String kbUuid, String query, int limit) {
        return nodeMapper.searchByContent(kbUuid, query, limit);
    }

    @Override
    public List<KnowledgeBaseGraphNode> getRelatedNodes(String nodeUuid, int depth, int limit) {
        List<KnowledgeBaseGraphNode> result = new ArrayList<>();
        List<String> visited = new ArrayList<>();
        visited.add(nodeUuid);
        
        // BFS遍历获取相关节点
        List<KnowledgeBaseGraphNode> currentLevel = nodeMapper.selectRelatedNodes(nodeUuid, limit);
        result.addAll(currentLevel);
        
        for (int i = 1; i < depth && result.size() < limit; i++) {
            List<KnowledgeBaseGraphNode> nextLevel = new ArrayList<>();
            for (KnowledgeBaseGraphNode node : currentLevel) {
                if (!visited.contains(node.getUuid())) {
                    visited.add(node.getUuid());
                    List<KnowledgeBaseGraphNode> related = nodeMapper.selectRelatedNodes(node.getUuid(), limit - result.size());
                    nextLevel.addAll(related);
                }
            }
            result.addAll(nextLevel);
            currentLevel = nextLevel;
            
            if (result.size() >= limit) {
                break;
            }
        }
        
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    @Override
    public void deleteByKbUuid(String kbUuid) {
        edgeMapper.deleteByKbUuid(kbUuid);
        nodeMapper.deleteByKbUuid(kbUuid);
        log.info("Deleted all graph data for kb: {}", kbUuid);
    }

    @Override
    public int countNodesByKbUuid(String kbUuid) {
        return nodeMapper.countByKbUuid(kbUuid);
    }

    @Override
    public KnowledgeBaseGraphNode getNodeByUuid(String nodeUuid) {
        return nodeMapper.selectByUuid(nodeUuid);
    }

    @Override
    public KnowledgeBaseGraphNode getNodeByNodeId(String kbUuid, String nodeId) {
        return nodeMapper.selectByNodeId(kbUuid, nodeId);
    }

    @Override
    public List<KnowledgeBaseGraphEdge> getEdgesByNodeUuid(String nodeUuid) {
        return edgeMapper.selectByNodeUuid(nodeUuid);
    }
}