package com.kes.rag;

import com.kes.config.RagConfig;
import com.kes.entity.RetrievalDecision;
import com.kes.enums.QueryIntent;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * SelfRagEvaluator 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SelfRagEvaluatorTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private RagConfig ragConfig;

    @Mock
    private RagConfig.QueryEnhancerConfig queryEnhancerConfig;

    @Mock
    private QueryEnhancer queryEnhancer;

    private SelfRagEvaluator selfRagEvaluator;

    @BeforeEach
    void setUp() {
        lenient().when(ragConfig.getQueryEnhancer()).thenReturn(queryEnhancerConfig);
        lenient().when(queryEnhancerConfig.getEnabled()).thenReturn(true);

        selfRagEvaluator = new SelfRagEvaluator();
        selfRagEvaluator.setChatModel(chatModel);
        selfRagEvaluator.setRagConfig(ragConfig);
        selfRagEvaluator.setQueryEnhancer(queryEnhancer);
    }

    @Test
    @DisplayName("评估问候语 - 不需要检索")
    void evaluate_greeting_returnsNoRetrieval() {
        String query = "你好";
        RetrievalDecision decision = selfRagEvaluator.evaluate(query);

        assertNotNull(decision);
        assertFalse(decision.isNeedRetrieval());
        assertEquals("问候语，无需检索", decision.getReason());
    }

    @Test
    @DisplayName("评估空查询 - 不需要检索")
    void evaluate_emptyQuery_returnsNoRetrieval() {
        RetrievalDecision decision = selfRagEvaluator.evaluate("");

        assertNotNull(decision);
        assertFalse(decision.isNeedRetrieval());
        assertEquals("空查询", decision.getReason());
    }

    @Test
    @DisplayName("评估null查询 - 不需要检索")
    void evaluate_nullQuery_returnsNoRetrieval() {
        RetrievalDecision decision = selfRagEvaluator.evaluate(null);

        assertNotNull(decision);
        assertFalse(decision.isNeedRetrieval());
        assertEquals("空查询", decision.getReason());
    }

    @Test
    @DisplayName("评估闲聊问题 - 不需要检索")
    void evaluate_casualQuery_returnsNoRetrieval() {
        String query = "今天天气怎么样？";
        RetrievalDecision decision = selfRagEvaluator.evaluate(query);

        assertNotNull(decision);
        assertFalse(decision.isNeedRetrieval());
    }

    @Test
    @DisplayName("评估技术问题 - 需要检索")
    void evaluate_technicalQuery_returnsNeedRetrieval() {
        String query = "如何配置Docker环境？";
        lenient().when(queryEnhancer.recognizeIntent(query)).thenReturn(QueryIntent.PROCEDURE);

        RetrievalDecision decision = selfRagEvaluator.evaluate(query);

        assertNotNull(decision);
        assertTrue(decision.isNeedRetrieval());
    }

    @Test
    @DisplayName("评估事实性问题 - 需要检索")
    void evaluate_factsQuery_returnsNeedRetrieval() {
        String query = "Java是什么？";
        lenient().when(queryEnhancer.recognizeIntent(query)).thenReturn(QueryIntent.FACTS);

        RetrievalDecision decision = selfRagEvaluator.evaluate(query);

        assertNotNull(decision);
        assertTrue(decision.isNeedRetrieval());
        assertEquals(QueryIntent.FACTS, decision.getIntent());
    }

    @Test
    @DisplayName("评估比较性问题 - 需要检索")
    void evaluate_comparisonQuery_returnsNeedRetrieval() {
        String query = "MySQL和PostgreSQL有什么区别？";
        lenient().when(queryEnhancer.recognizeIntent(query)).thenReturn(QueryIntent.COMPARISON);

        RetrievalDecision decision = selfRagEvaluator.evaluate(query);

        assertNotNull(decision);
        assertTrue(decision.isNeedRetrieval());
        assertEquals(QueryIntent.COMPARISON, decision.getIntent());
    }

    @Test
    @DisplayName("评估分析性问题 - 需要检索")
    void evaluate_analysisQuery_returnsNeedRetrieval() {
        String query = "为什么要使用微服务？";
        lenient().when(queryEnhancer.recognizeIntent(query)).thenReturn(QueryIntent.ANALYSIS);

        RetrievalDecision decision = selfRagEvaluator.evaluate(query);

        assertNotNull(decision);
        assertTrue(decision.isNeedRetrieval());
        assertEquals(QueryIntent.ANALYSIS, decision.getIntent());
    }

    @Test
    @DisplayName("评估问题包含技术术语 - 需要检索")
    void evaluate_withTechnicalTerms_returnsNeedRetrieval() {
        String query = "Nginx的负载均衡怎么配置";
        lenient().when(queryEnhancer.recognizeIntent(query)).thenReturn(QueryIntent.UNKNOWN);

        RetrievalDecision decision = selfRagEvaluator.evaluate(query);

        assertNotNull(decision);
        assertTrue(decision.isNeedRetrieval());
    }

    @Test
    @DisplayName("RetrievalDecision工厂方法 - needRetrieval")
    void retrievalDecision_factoryNeedRetrieval() {
        RetrievalDecision decision = RetrievalDecision.needRetrieval("测试原因", QueryIntent.FACTS);

        assertTrue(decision.isNeedRetrieval());
        assertEquals("测试原因", decision.getReason());
        assertEquals(QueryIntent.FACTS, decision.getIntent());
    }

    @Test
    @DisplayName("RetrievalDecision工厂方法 - noRetrieval")
    void retrievalDecision_factoryNoRetrieval() {
        RetrievalDecision decision = RetrievalDecision.noRetrieval("闲聊不需要检索");

        assertFalse(decision.isNeedRetrieval());
        assertEquals("闲聊不需要检索", decision.getReason());
        assertEquals(QueryIntent.UNKNOWN, decision.getIntent());
    }

    @Test
    @DisplayName("RetrievalDecision - 添加关键词")
    void retrievalDecision_addKeywords() {
        RetrievalDecision decision = new RetrievalDecision();
        decision.addKeyword("Docker");
        decision.addKeyword("配置");

        assertEquals(2, decision.getKeywords().size());
        assertTrue(decision.getKeywords().contains("Docker"));
        assertTrue(decision.getKeywords().contains("配置"));
    }

    @Test
    @DisplayName("RetrievalDecision - 添加增强查询")
    void retrievalDecision_addEnhancedQueries() {
        RetrievalDecision decision = new RetrievalDecision();
        decision.addEnhancedQuery("查询1");
        decision.addEnhancedQuery("查询2");

        assertEquals(2, decision.getEnhancedQueries().size());
        assertTrue(decision.getEnhancedQueries().contains("查询1"));
        assertTrue(decision.getEnhancedQueries().contains("查询2"));
    }
}
