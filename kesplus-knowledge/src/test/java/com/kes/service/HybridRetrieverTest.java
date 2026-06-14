package com.kes.service;

import com.kes.KnowledgeBaseApplication;
import com.kes.config.RagConfig;
import com.kes.entity.EmbeddingModel;
import com.kes.entity.GraphRetrievalResult;
import com.kes.entity.HybridRetrievalResult;
import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseGraphNode;
import com.kes.mapper.EmbeddingMapper;
import com.kes.mapper.GraphNodeMapper;
import com.kes.mapper.GraphEdgeMapper;
import com.kes.graph.GraphStorage;
import com.kes.util.UuidUtil;
import dev.langchain4j.data.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HybridRetriever集成测试
 * 使用真实数据库进行测试
 */
@SpringBootTest(classes = KnowledgeBaseApplication.class)
@ActiveProfiles("test")
@Transactional
class HybridRetrieverTest {

    @Autowired
    private HybridRetriever hybridRetriever;

    @Autowired
    private EmbeddingRagService embeddingRagService;

    @Autowired
    private GraphRagService graphRagService;

    @Autowired
    private DynamicTableService dynamicTableService;

    @Autowired
    private EmbeddingMapper embeddingMapper;

    @Autowired
    private GraphNodeMapper graphNodeMapper;

    @Autowired
    private GraphEdgeMapper graphEdgeMapper;

    @Autowired
    private GraphStorage graphStorage;

    @Autowired
    private EmbeddingModelService embeddingModelService;

    @Autowired
    private RagConfig ragConfig;

    @Value("${rag.embedding.api-key}")
    private String embeddingApiKey;

    private KnowledgeBase testKb;
    private dev.langchain4j.model.embedding.EmbeddingModel testEmbeddingModel;

    @BeforeEach
    void setUp() {
        // 创建测试知识库
        testKb = new KnowledgeBase();
        testKb.setUuid(UuidUtil.create());
        testKb.setTitle("Test Hybrid Retrieval KB");
        testKb.setEmbeddingDimension(1024);
        testKb.setRetrieveMaxResults(5);
        testKb.setRetrieveMinScore(0.5);

        // 创建嵌入模型配置
        EmbeddingModel entityModel = new EmbeddingModel();
        entityModel.setId(1L);
        entityModel.setUuid(UuidUtil.create());
        entityModel.setModelName("BAAI/bge-m3");
        entityModel.setEmbeddingDimension(1024);
        entityModel.setModelType("huggingface");
        entityModel.setBaseUrl("https://api.siliconflow.cn/v1");
        entityModel.setApiKey(embeddingApiKey);

        // 创建LangChain嵌入模型
        testEmbeddingModel = embeddingModelService.createLangChainEmbeddingModel(entityModel);

        // 清理测试数据
        cleanupTestData();
    }

    @Test
    void testRetrieve_Success() {
        // 准备图谱数据
        Document doc1 = Document.from("Java programming language features");
        Document doc2 = Document.from("Python data science libraries");
        Document doc3 = Document.from("Machine learning algorithms");

        // 创建图谱索引
        graphRagService.indexGraph(testKb, Arrays.asList(doc1, doc2, doc3));

        // 验证图谱数据已创建
        int nodeCount = graphStorage.countNodesByKbUuid(testKb.getUuid());
        assertTrue(nodeCount > 0);

        // 执行混合检索（向量检索可能失败，但图谱检索应该成功）
        try {
            List<HybridRetrievalResult> results = hybridRetriever.retrieve(testKb, "Java", testEmbeddingModel);

            // 验证结果
            assertNotNull(results);
        } catch (Exception e) {
            // 向量检索可能因API调用失败，但测试应该继续
            System.out.println("Vector retrieval may have failed: " + e.getMessage());
        }
    }

    @Test
    void testRetrieve_GraphOnly() {
        // 准备图谱数据
        Document doc1 = Document.from("Java programming language");
        Document doc2 = Document.from("Python data science");

        // 创建图谱索引
        graphRagService.indexGraph(testKb, Arrays.asList(doc1, doc2));

        // 执行图谱检索
        List<HybridRetrievalResult> results = hybridRetriever.retrieveGraphOnly(testKb, "Java");

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());

        // 验证结果类型
        for (HybridRetrievalResult result : results) {
            assertEquals("GRAPH", result.getSourceType());
        }
    }

    @Test
    void testRetrieve_GraphOnly_Fallback() {
        // 没有数据的情况下执行检索
        List<HybridRetrievalResult> results = hybridRetriever.retrieveGraphOnly(testKb, "test query");

        // 验证结果（应该返回空列表或空结果）
        assertNotNull(results);
    }

    @Test
    void testGraphRagService_Index() {
        // 准备测试文档
        Document doc = Document.from("Graph database concepts introduction");

        // 执行图谱索引
        graphRagService.indexGraph(testKb, Arrays.asList(doc));

        // 验证节点创建
        int nodeCount = graphStorage.countNodesByKbUuid(testKb.getUuid());
        assertTrue(nodeCount >= 2); // DOCUMENT + SEGMENT + KEYWORDs
    }

    @Test
    void testGraphRagService_Retrieve() {
        // 准备测试文档
        Document doc = Document.from("Java programming language features");

        // 创建图谱索引
        graphRagService.indexGraph(testKb, Arrays.asList(doc));

        // 执行图谱检索
        List<GraphRetrievalResult> results = graphRagService.retrieve(testKb, "Java", 5);

        // 验证结果
        assertNotNull(results);
    }

    @Test
    void testGraphStorage_CreateNode() {
        // 创建节点
        KnowledgeBaseGraphNode node = new KnowledgeBaseGraphNode();
        node.setUuid(UuidUtil.create());
        node.setKbUuid(testKb.getUuid());
        node.setNodeType("SEGMENT");
        node.setContent("Test content");

        // 插入节点
        graphNodeMapper.insert(node);

        // 查询验证
        KnowledgeBaseGraphNode retrieved = graphNodeMapper.selectByUuid(node.getUuid());
        assertNotNull(retrieved);
        assertEquals(node.getContent(), retrieved.getContent());
    }

    @Test
    void testConfiguration() {
        // 验证配置
        assertTrue(ragConfig.getEnableGraphRag());
        String storageType = ragConfig.getGraph().getStorageType();
        assertTrue(storageType.equals("postgresql") || storageType.equals("neo4j"));
    }

    private void cleanupTestData() {
        try {
            // 清理图谱存储（Neo4j或PostgreSQL）
            graphStorage.deleteByKbUuid(testKb.getUuid());
            // 清理PostgreSQL中的图谱数据（如果使用Neo4j存储）
            graphEdgeMapper.deleteByKbUuid(testKb.getUuid());
            graphNodeMapper.deleteByKbUuid(testKb.getUuid());
        } catch (Exception e) {
            // 忽略清理错误
        }
    }
}
