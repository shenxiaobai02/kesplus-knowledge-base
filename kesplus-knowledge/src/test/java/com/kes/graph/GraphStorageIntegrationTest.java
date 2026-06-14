package com.kes.graph;

import com.kes.KnowledgeBaseApplication;
import com.kes.config.RagConfig;
import com.kes.entity.KnowledgeBaseGraphNode;
import com.kes.entity.KnowledgeBaseGraphEdge;
import com.kes.util.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GraphStorage集成测试
 * 使用实际的PostgreSQL数据库
 */
@SpringBootTest(classes = KnowledgeBaseApplication.class)
@ActiveProfiles("test")
@Transactional
class GraphStorageIntegrationTest {

    @Autowired
    private GraphStorage graphStorage;

    @Autowired
    private RagConfig ragConfig;

    private String testKbUuid;
    private KnowledgeBaseGraphNode testNode1;
    private KnowledgeBaseGraphNode testNode2;

    @BeforeEach
    void setUp() {
        testKbUuid = UuidUtil.create();

        // 创建测试节点
        testNode1 = new KnowledgeBaseGraphNode();
        testNode1.setUuid(UuidUtil.create());
        testNode1.setKbUuid(testKbUuid);
        testNode1.setNodeType("SEGMENT");
        testNode1.setNodeId("segment-1");
        testNode1.setContent("First test segment content about Java programming");
        testNode1.setMetadataJson("{\"source\": \"test\"}");

        testNode2 = new KnowledgeBaseGraphNode();
        testNode2.setUuid(UuidUtil.create());
        testNode2.setKbUuid(testKbUuid);
        testNode2.setNodeType("SEGMENT");
        testNode2.setNodeId("segment-2");
        testNode2.setContent("Second test segment about Python data science");
        testNode2.setMetadataJson("{\"source\": \"test\"}");

        // 清理测试数据
        cleanupTestData();
    }

    @Test
    void testGraphStorage_Configuration() {
        // 验证配置（根据实际配置动态验证）
        String storageType = ragConfig.getGraph().getStorageType();
        assertNotNull(storageType);
        assertTrue(storageType.equals("postgresql") || storageType.equals("neo4j"));
        assertNotNull(ragConfig.getGraph().getNeo4j());
    }

    @Test
    void testGraphStorage_CreateAndRetrieveNode() {
        // 创建节点
        graphStorage.createNode(testNode1);

        // 验证节点已创建
        KnowledgeBaseGraphNode retrieved = graphStorage.getNodeByUuid(testNode1.getUuid());
        assertNotNull(retrieved);
        assertEquals(testNode1.getContent(), retrieved.getContent());
        assertEquals(testNode1.getNodeType(), retrieved.getNodeType());
    }

    @Test
    void testGraphStorage_BatchCreateNodes() {
        // 批量创建节点
        graphStorage.batchCreateNodes(List.of(testNode1, testNode2));

        // 验证节点数量
        int count = graphStorage.countNodesByKbUuid(testKbUuid);
        assertEquals(2, count);
    }

    @Test
    void testGraphStorage_CreateAndRetrieveEdge() {
        // 创建节点
        graphStorage.createNode(testNode1);
        graphStorage.createNode(testNode2);

        // 创建关系
        KnowledgeBaseGraphEdge edge = new KnowledgeBaseGraphEdge();
        edge.setUuid(UuidUtil.create());
        edge.setKbUuid(testKbUuid);
        edge.setSourceNodeUuid(testNode1.getUuid());
        edge.setTargetNodeUuid(testNode2.getUuid());
        edge.setRelationType("SIMILAR");
        edge.setWeight(0.8);
        edge.setMetadataJson("{\"score\": 0.8}");

        graphStorage.createEdge(edge);

        // 验证关系
        List<KnowledgeBaseGraphEdge> edges = graphStorage.getEdgesByNodeUuid(testNode1.getUuid());
        assertNotNull(edges);
        assertEquals(1, edges.size());
        assertEquals(edge.getRelationType(), edges.get(0).getRelationType());
    }

    @Test
    void testGraphStorage_SearchNodes() {
        // 创建节点
        graphStorage.batchCreateNodes(List.of(testNode1, testNode2));

        // 搜索节点
        List<KnowledgeBaseGraphNode> results = graphStorage.searchNodes(testKbUuid, "Java", 10);

        // 验证搜索结果
        assertNotNull(results);
        assertFalse(results.isEmpty());

        // 验证搜索结果包含关键词
        boolean found = results.stream().anyMatch(n -> n.getContent().contains("Java"));
        assertTrue(found);
    }

    @Test
    void testGraphStorage_GetRelatedNodes() {
        // 创建节点
        graphStorage.batchCreateNodes(List.of(testNode1, testNode2));

        // 创建关系
        KnowledgeBaseGraphEdge edge = new KnowledgeBaseGraphEdge();
        edge.setUuid(UuidUtil.create());
        edge.setKbUuid(testKbUuid);
        edge.setSourceNodeUuid(testNode1.getUuid());
        edge.setTargetNodeUuid(testNode2.getUuid());
        edge.setRelationType("CONTAINS");
        edge.setWeight(1.0);

        graphStorage.batchCreateEdges(List.of(edge));

        // 获取相关节点
        List<KnowledgeBaseGraphNode> related = graphStorage.getRelatedNodes(testNode1.getUuid(), 1, 10);

        // 验证相关节点
        assertNotNull(related);
    }

    @Test
    void testGraphStorage_DeleteByKbUuid() {
        // 创建节点和关系
        graphStorage.batchCreateNodes(List.of(testNode1, testNode2));

        KnowledgeBaseGraphEdge edge = new KnowledgeBaseGraphEdge();
        edge.setUuid(UuidUtil.create());
        edge.setKbUuid(testKbUuid);
        edge.setSourceNodeUuid(testNode1.getUuid());
        edge.setTargetNodeUuid(testNode2.getUuid());
        edge.setRelationType("CONTAINS");
        edge.setWeight(1.0);

        graphStorage.batchCreateEdges(List.of(edge));

        // 删除
        graphStorage.deleteByKbUuid(testKbUuid);

        // 验证删除
        int count = graphStorage.countNodesByKbUuid(testKbUuid);
        assertEquals(0, count);
    }

    @Test
    void testGraphStorage_GetNodeByNodeId() {
        // 创建节点
        graphStorage.createNode(testNode1);

        // 按nodeId查询
        KnowledgeBaseGraphNode retrieved = graphStorage.getNodeByNodeId(testKbUuid, "segment-1");

        // 验证结果
        assertNotNull(retrieved);
        assertEquals(testNode1.getUuid(), retrieved.getUuid());
    }

    @Test
    void testGraphStorage_CountNodes() {
        // 创建节点
        graphStorage.batchCreateNodes(List.of(testNode1, testNode2));

        // 统计
        int count = graphStorage.countNodesByKbUuid(testKbUuid);

        // 验证
        assertTrue(count >= 2);
    }

    private void cleanupTestData() {
        try {
            graphStorage.deleteByKbUuid(testKbUuid);
        } catch (Exception e) {
            // 忽略清理错误
        }
    }
}
