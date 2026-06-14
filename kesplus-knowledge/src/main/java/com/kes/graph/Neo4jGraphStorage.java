package com.kes.graph;

import com.kes.entity.KnowledgeBaseGraphNode;
import com.kes.entity.KnowledgeBaseGraphEdge;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j图谱存储实现
 * 使用Neo4j图数据库存储图谱数据
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rag.graph.storage-type", havingValue = "neo4j")
public class Neo4jGraphStorage implements GraphStorage {

    @Autowired
    private Driver neo4jDriver;

    @Override
    public void createNode(KnowledgeBaseGraphNode node) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                CREATE (n:GraphNode {
                    uuid: $uuid,
                    kbUuid: $kbUuid,
                    nodeType: $nodeType,
                    nodeId: $nodeId,
                    content: $content,
                    metadataJson: $metadataJson,
                    createdTime: datetime()
                })
                """;
            session.run(cypher, 
                org.neo4j.driver.Values.parameters(
                    "uuid", node.getUuid(),
                    "kbUuid", node.getKbUuid(),
                    "nodeType", node.getNodeType(),
                    "nodeId", node.getNodeId(),
                    "content", node.getContent(),
                    "metadataJson", node.getMetadataJson()
                ));
            log.debug("Created Neo4j graph node: {}", node.getUuid());
        }
    }

    @Override
    public void batchCreateNodes(List<KnowledgeBaseGraphNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        try (Session session = neo4jDriver.session()) {
            for (KnowledgeBaseGraphNode node : nodes) {
                createNode(node);
            }
            log.debug("Batch created {} Neo4j graph nodes", nodes.size());
        }
    }

    @Override
    public void createEdge(KnowledgeBaseGraphEdge edge) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (source:GraphNode {uuid: $sourceUuid})
                MATCH (target:GraphNode {uuid: $targetUuid})
                CREATE (source)-[r:RELATES {
                    uuid: $uuid,
                    kbUuid: $kbUuid,
                    relationType: $relationType,
                    weight: $weight,
                    metadataJson: $metadataJson,
                    createdTime: datetime()
                }]->(target)
                """;
            session.run(cypher,
                org.neo4j.driver.Values.parameters(
                    "sourceUuid", edge.getSourceNodeUuid(),
                    "targetUuid", edge.getTargetNodeUuid(),
                    "uuid", edge.getUuid(),
                    "kbUuid", edge.getKbUuid(),
                    "relationType", edge.getRelationType(),
                    "weight", edge.getWeight(),
                    "metadataJson", edge.getMetadataJson()
                ));
            log.debug("Created Neo4j graph edge: {} -> {}", edge.getSourceNodeUuid(), edge.getTargetNodeUuid());
        }
    }

    @Override
    public void batchCreateEdges(List<KnowledgeBaseGraphEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return;
        }
        try (Session session = neo4jDriver.session()) {
            for (KnowledgeBaseGraphEdge edge : edges) {
                createEdge(edge);
            }
            log.debug("Batch created {} Neo4j graph edges", edges.size());
        }
    }

    @Override
    public List<KnowledgeBaseGraphNode> searchNodes(String kbUuid, String query, int limit) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (n:GraphNode)
                WHERE n.kbUuid = $kbUuid AND n.content CONTAINS $query
                RETURN n
                LIMIT $limit
                """;
            Result result = session.run(cypher,
                org.neo4j.driver.Values.parameters(
                    "kbUuid", kbUuid,
                    "query", query,
                    "limit", limit
                ));
            return convertRecordsToNodes(result.list());
        }
    }

    @Override
    public List<KnowledgeBaseGraphNode> getRelatedNodes(String nodeUuid, int depth, int limit) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (start:GraphNode {uuid: $nodeUuid})
                MATCH (start)-[*1..%d]-(related:GraphNode)
                RETURN DISTINCT related AS n
                LIMIT $limit
                """.formatted(depth);
            Result result = session.run(cypher,
                org.neo4j.driver.Values.parameters(
                    "nodeUuid", nodeUuid,
                    "limit", limit
                ));
            return convertRecordsToNodes(result.list());
        }
    }

    @Override
    public void deleteByKbUuid(String kbUuid) {
        try (Session session = neo4jDriver.session()) {
            // 先删除关系，再删除节点
            String deleteEdgesCypher = """
                MATCH ()-[r:RELATES {kbUuid: $kbUuid}]-()
                DELETE r
                """;
            session.run(deleteEdgesCypher, org.neo4j.driver.Values.parameters("kbUuid", kbUuid));
            
            String deleteNodesCypher = """
                MATCH (n:GraphNode {kbUuid: $kbUuid})
                DELETE n
                """;
            session.run(deleteNodesCypher, org.neo4j.driver.Values.parameters("kbUuid", kbUuid));
            log.info("Deleted all Neo4j graph data for kb: {}", kbUuid);
        }
    }

    @Override
    public int countNodesByKbUuid(String kbUuid) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (n:GraphNode {kbUuid: $kbUuid})
                RETURN count(n) as count
                """;
            Result result = session.run(cypher, org.neo4j.driver.Values.parameters("kbUuid", kbUuid));
            if (result.hasNext()) {
                return result.next().get("count").asInt();
            }
            return 0;
        }
    }

    @Override
    public KnowledgeBaseGraphNode getNodeByUuid(String nodeUuid) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (n:GraphNode {uuid: $nodeUuid})
                RETURN n
                """;
            Result result = session.run(cypher, org.neo4j.driver.Values.parameters("nodeUuid", nodeUuid));
            if (result.hasNext()) {
                return convertRecordToNode(result.next());
            }
            return null;
        }
    }

    @Override
    public KnowledgeBaseGraphNode getNodeByNodeId(String kbUuid, String nodeId) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (n:GraphNode {kbUuid: $kbUuid, nodeId: $nodeId})
                RETURN n
                """;
            Result result = session.run(cypher,
                org.neo4j.driver.Values.parameters("kbUuid", kbUuid, "nodeId", nodeId));
            if (result.hasNext()) {
                return convertRecordToNode(result.next());
            }
            return null;
        }
    }

    @Override
    public List<KnowledgeBaseGraphEdge> getEdgesByNodeUuid(String nodeUuid) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (n:GraphNode {uuid: $nodeUuid})-[r:RELATES]-()
                RETURN r
                """;
            Result result = session.run(cypher, org.neo4j.driver.Values.parameters("nodeUuid", nodeUuid));
            return convertRecordsToEdges(result.list());
        }
    }

    /**
     * 将Neo4j记录转换为节点实体
     */
    private KnowledgeBaseGraphNode convertRecordToNode(Record record) {
        org.neo4j.driver.Value nodeValue = record.get("n");
        if (nodeValue == null || nodeValue.isNull()) {
            return null;
        }
        KnowledgeBaseGraphNode node = new KnowledgeBaseGraphNode();
        node.setUuid(nodeValue.get("uuid").asString());
        node.setKbUuid(nodeValue.get("kbUuid").asString());
        node.setNodeType(nodeValue.get("nodeType").asString());
        node.setNodeId(nodeValue.get("nodeId").asString(null));
        node.setContent(nodeValue.get("content").asString());
        node.setMetadataJson(nodeValue.get("metadataJson").asString(null));
        return node;
    }

    /**
     * 将Neo4j记录列表转换为节点实体列表
     */
    private List<KnowledgeBaseGraphNode> convertRecordsToNodes(List<Record> records) {
        List<KnowledgeBaseGraphNode> nodes = new ArrayList<>();
        for (Record record : records) {
            KnowledgeBaseGraphNode node = convertRecordToNode(record);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * 将Neo4j记录转换为关系实体
     */
    private KnowledgeBaseGraphEdge convertRecordToEdge(Record record) {
        org.neo4j.driver.Value edgeValue = record.get("r");
        KnowledgeBaseGraphEdge edge = new KnowledgeBaseGraphEdge();
        edge.setUuid(edgeValue.get("uuid").asString());
        edge.setKbUuid(edgeValue.get("kbUuid").asString());
        edge.setRelationType(edgeValue.get("relationType").asString());
        edge.setWeight(edgeValue.get("weight").asDouble(1.0));
        edge.setMetadataJson(edgeValue.get("metadataJson").asString(null));
        return edge;
    }

    /**
     * 将Neo4j记录列表转换为关系实体列表
     */
    private List<KnowledgeBaseGraphEdge> convertRecordsToEdges(List<Record> records) {
        List<KnowledgeBaseGraphEdge> edges = new ArrayList<>();
        for (Record record : records) {
            edges.add(convertRecordToEdge(record));
        }
        return edges;
    }
}