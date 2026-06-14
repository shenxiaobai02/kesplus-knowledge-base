package com.kes.service;

import com.kes.KnowledgeBaseApplication;
import com.kes.config.RagConfig;
import com.kes.entity.HybridRetrievalResult;
import com.kes.entity.RerankedResult;
import com.kes.reranker.Reranker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RerankerService集成测试
 * 使用真实数据库和配置
 */
@SpringBootTest(classes = KnowledgeBaseApplication.class)
@ActiveProfiles("test")
class RerankerServiceTest {

    @Autowired
    private RerankerService rerankerService;

    @Autowired
    private Reranker reranker;

    @Autowired
    private RagConfig ragConfig;

    private List<HybridRetrievalResult> testResults;

    @BeforeEach
    void setUp() {
        testResults = createHybridResults();
    }

    @Test
    void testRerank_Success() {
        // 执行测试
        List<RerankedResult> results = rerankerService.rerank("test query", testResults);

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(testResults.size(), results.size());

        // 验证排序位置
        for (int i = 0; i < results.size(); i++) {
            assertEquals(i + 1, results.get(i).getRank());
        }
    }

    @Test
    void testRerank_EmptyResults() {
        // 执行测试（空结果）
        List<RerankedResult> results = rerankerService.rerank("test query", new ArrayList<>());

        // 验证结果（返回空列表）
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testRerank_NullResults() {
        // 执行测试（null结果）
        List<RerankedResult> results = rerankerService.rerank("test query", null);

        // 验证结果（返回空列表）
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetRerankerType() {
        // 执行测试
        String type = rerankerService.getRerankerType();

        // 验证结果
        assertEquals("score", type);
    }

    @Test
    void testConfiguration() {
        // 验证配置
        assertTrue(ragConfig.getEnableGraphRag());
        assertEquals("score", ragConfig.getReranker().getType());
    }

    @Test
    void testReranker_Direct() {
        // 直接测试Reranker
        List<RerankedResult> results = reranker.rerank("java programming", testResults);

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());

        // 验证分数在合理范围内
        for (RerankedResult result : results) {
            assertTrue(result.getRerankedScore() >= 0.0);
            assertTrue(result.getRerankedScore() <= 1.0);
        }
    }

    // 辅助方法：创建混合检索结果
    private List<HybridRetrievalResult> createHybridResults() {
        List<HybridRetrievalResult> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            HybridRetrievalResult result = new HybridRetrievalResult();
            result.setUuid("hybrid-uuid-" + i);
            result.setKbUuid("test-kb-uuid");
            result.setContent("Hybrid content " + i);
            result.setVectorScore(0.8 - i * 0.1);
            result.setGraphScore(0.7 - i * 0.1);
            result.setSourceType(i < 2 ? "HYBRID" : "VECTOR");
            result.calculateCombinedScore(0.6, 0.4);
            results.add(result);
        }
        return results;
    }
}
