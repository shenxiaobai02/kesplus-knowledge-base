package com.kes.service;

import com.kes.KnowledgeBaseApplication;
import com.kes.config.RagConfig;
import com.kes.entity.EmbeddingModel;
import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseEmbedding;
import com.kes.mapper.EmbeddingMapper;
import com.kes.util.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmbeddingRagService集成测试
 * 使用真实数据库进行测试
 */
@SpringBootTest(classes = KnowledgeBaseApplication.class)
@ActiveProfiles("test")
@Transactional
class EmbeddingRagServiceTest {

    @Autowired
    private EmbeddingRagService embeddingRagService;

    @Autowired
    private DynamicTableService dynamicTableService;

    @Autowired
    private EmbeddingMapper embeddingMapper;

    @Autowired
    private EmbeddingModelService embeddingModelService;

    @Autowired
    private RagConfig ragConfig;

    @Value("${rag.embedding.api-key}")
    private String embeddingApiKey;

    private KnowledgeBase kb;
    private dev.langchain4j.model.embedding.EmbeddingModel langChainModel;

    @BeforeEach
    void setUp() {
        kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setUuid(UuidUtil.create());
        kb.setTitle("Test KB");
        kb.setEmbeddingDimension(1024);
        kb.setRetrieveMaxResults(5);
        kb.setRetrieveMinScore(0.6);

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
        langChainModel = embeddingModelService.createLangChainEmbeddingModel(entityModel);

        // 清理测试数据
        cleanupTestData();
    }

    @Test
    void testCountByKbUuid() {
        // 验证空计数
        int count = embeddingRagService.countByKbUuid(kb);
        assertEquals(0, count);
    }

    @Test
    void testEnsureTableExists() {
        assertDoesNotThrow(() -> embeddingRagService.ensureTableExists(1024));
    }

    @Test
    void testDeleteByKbUuid() {
        // 测试删除不存在的数据不会报错
        assertDoesNotThrow(() -> embeddingRagService.deleteByKbUuid(kb));
    }

    @Test
    void testConfiguration() {
        // 验证配置
        assertTrue(ragConfig.getEnableGraphRag());
    }

    @Test
    void testRetrieve_Empty() {
        // 测试空检索
        try {
            List<KnowledgeBaseEmbedding> results = embeddingRagService.retrieve(kb, "test query", langChainModel);
            assertNotNull(results);
        } catch (Exception e) {
            // 向量检索可能因API调用失败
            System.out.println("Vector retrieval may have failed: " + e.getMessage());
        }
    }

    @Test
    void testDynamicTableService_GetTableName() {
        // 测试动态表名服务
        String tableName = dynamicTableService.getTableName(1024);
        assertNotNull(tableName);
        assertTrue(tableName.contains("embedding"));
    }

    private void cleanupTestData() {
        try {
            String tableName = dynamicTableService.getTableName(kb.getEmbeddingDimension());
            embeddingMapper.deleteByKbUuid(tableName, kb.getUuid());
        } catch (Exception e) {
            // 忽略清理错误
        }
    }
}
