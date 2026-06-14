package com.kes.reranker;

import com.kes.KnowledgeBaseApplication;
import com.kes.config.RagConfig;
import com.kes.entity.HybridRetrievalResult;
import com.kes.entity.RerankedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScoreBasedReranker集成测试
 * 使用真实配置和依赖
 */
@SpringBootTest(classes = KnowledgeBaseApplication.class)
@ActiveProfiles("test")
class ScoreBasedRerankerTest {

    @Autowired
    private ScoreBasedReranker reranker;

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
        List<RerankedResult> results = reranker.rerank("test query", testResults);

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(testResults.size(), results.size());

        // 验证排序位置
        for (int i = 0; i < results.size(); i++) {
            assertEquals(i + 1, results.get(i).getRank());
        }

        // 验证分数调整
        for (RerankedResult result : results) {
            assertTrue(result.getRerankedScore() >= 0.0);
            assertTrue(result.getRerankedScore() <= 1.0);
        }
    }

    @Test
    void testRerank_EmptyResults() {
        // 执行测试（空结果）
        List<RerankedResult> results = reranker.rerank("test query", new ArrayList<>());

        // 验证结果（返回空列表）
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testRerank_NullResults() {
        // 执行测试（null结果）
        List<RerankedResult> results = reranker.rerank("test query", null);

        // 验证结果（返回空列表）
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testRerank_KeywordMatchBoost() {
        // 创建包含查询关键词的结果
        HybridRetrievalResult resultWithKeyword = new HybridRetrievalResult();
        resultWithKeyword.setContent("This is test content with query keywords");
        resultWithKeyword.setVectorScore(0.5);
        resultWithKeyword.setGraphScore(0.5);
        resultWithKeyword.calculateCombinedScore(0.6, 0.4);

        List<HybridRetrievalResult> results = new ArrayList<>();
        results.add(resultWithKeyword);

        // 执行测试
        List<RerankedResult> rerankedResults = reranker.rerank("test query", results);

        // 验证关键词匹配增强
        RerankedResult rerankedResult = rerankedResults.get(0);
        assertTrue(rerankedResult.getRerankedScore() > rerankedResult.getOriginalScore());
    }

    @Test
    void testRerank_HybridSourceBoost() {
        // 创建HYBRID来源的结果
        HybridRetrievalResult hybridResult = new HybridRetrievalResult();
        hybridResult.setContent("Hybrid content");
        hybridResult.setVectorScore(0.5);
        hybridResult.setGraphScore(0.5);
        hybridResult.setSourceType("HYBRID");
        hybridResult.calculateCombinedScore(0.6, 0.4);

        // 创建VECTOR来源的结果
        HybridRetrievalResult vectorResult = new HybridRetrievalResult();
        vectorResult.setContent("Vector content");
        vectorResult.setVectorScore(0.5);
        vectorResult.setGraphScore(0.0);
        vectorResult.setSourceType("VECTOR");
        vectorResult.calculateCombinedScore(0.6, 0.4);

        List<HybridRetrievalResult> results = new ArrayList<>();
        results.add(hybridResult);
        results.add(vectorResult);

        // 执行测试
        List<RerankedResult> rerankedResults = reranker.rerank("test", results);

        // 验证HYBRID来源获得额外权重
        assertTrue(rerankedResults.get(0).getSourceType().equals("HYBRID"));
    }

    @Test
    void testGetType() {
        // 执行测试
        String type = reranker.getType();

        // 验证结果
        assertEquals("score", type);
    }

    @Test
    void testConfiguration() {
        // 验证配置
        assertEquals("score", ragConfig.getReranker().getType());
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
