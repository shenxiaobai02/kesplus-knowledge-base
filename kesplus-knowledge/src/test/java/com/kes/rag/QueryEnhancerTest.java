package com.kes.rag;

import com.kes.config.RagConfig;
import com.kes.dto.QueryContext;
import com.kes.enums.QueryIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * QueryEnhancer 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QueryEnhancerTest {

    @Mock
    private RagConfig ragConfig;

    @Mock
    private RagConfig.QueryEnhancerConfig queryEnhancerConfig;

    private DefaultQueryEnhancer queryEnhancer;

    @BeforeEach
    void setUp() {
        lenient().when(ragConfig.getQueryEnhancer()).thenReturn(queryEnhancerConfig);
        lenient().when(queryEnhancerConfig.getEnabled()).thenReturn(true);
        lenient().when(queryEnhancerConfig.getEnableQueryRewrite()).thenReturn(true);
        lenient().when(queryEnhancerConfig.getEnableQueryExpansion()).thenReturn(true);
        lenient().when(queryEnhancerConfig.getMaxEnhancedQueries()).thenReturn(3);

        queryEnhancer = new DefaultQueryEnhancer();
        queryEnhancer.setRagConfig(ragConfig);
    }

    @Test
    @DisplayName("识别事实性问题意图")
    void recognizeIntent_factsQuery_returnsFactsIntent() {
        String query = "Java是什么？";
        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        assertEquals(QueryIntent.FACTS, intent);
    }

    @Test
    @DisplayName("识别流程性问题意图")
    void recognizeIntent_procedureQuery_returnsProcedureIntent() {
        String query = "如何安装Docker？";
        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        assertEquals(QueryIntent.PROCEDURE, intent);
    }

    @Test
    @DisplayName("识别比较性问题意图")
    void recognizeIntent_comparisonQuery_returnsComparisonIntent() {
        String query = "MySQL和PostgreSQL有什么区别？";
        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        assertEquals(QueryIntent.COMPARISON, intent);
    }

    @Test
    @DisplayName("识别分析性问题意图")
    void recognizeIntent_analysisQuery_returnsAnalysisIntent() {
        String query = "为什么需要微服务架构？";
        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        assertEquals(QueryIntent.ANALYSIS, intent);
    }

    @Test
    @DisplayName("识别创意性问题意图")
    void recognizeIntent_creativeQuery_returnsCreativeIntent() {
        String query = "请给我一些Docker优化的建议";
        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        assertEquals(QueryIntent.CREATIVE, intent);
    }

    @Test
    @DisplayName("识别未知意图")
    void recognizeIntent_unknownQuery_returnsUnknownIntent() {
        String query = "今天天气真好";
        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        assertEquals(QueryIntent.UNKNOWN, intent);
    }

    @Test
    @DisplayName("识别空查询意图")
    void recognizeIntent_emptyQuery_returnsUnknownIntent() {
        QueryIntent intent = queryEnhancer.recognizeIntent("");
        assertEquals(QueryIntent.UNKNOWN, intent);
    }

    @Test
    @DisplayName("识别null查询意图")
    void recognizeIntent_nullQuery_returnsUnknownIntent() {
        QueryIntent intent = queryEnhancer.recognizeIntent(null);
        assertEquals(QueryIntent.UNKNOWN, intent);
    }

    @Test
    @DisplayName("增强查询 - 返回原始查询和改写后的查询")
    void enhance_withValidQuery_returnsEnhancedQueries() {
        String query = "如何使用Docker";
        QueryContext context = QueryContext.of("kb-uuid-123", query);

        List<String> enhancedQueries = queryEnhancer.enhance(query, context);

        assertNotNull(enhancedQueries);
        assertTrue(enhancedQueries.size() >= 1);
        assertTrue(enhancedQueries.contains(query));
    }

    @Test
    @DisplayName("增强查询 - 包含扩展的同义词查询")
    void enhance_withTechnicalTerm_includesSynonymExpansion() {
        String query = "如何配置Nginx";
        QueryContext context = QueryContext.of("kb-uuid-123", query);

        List<String> enhancedQueries = queryEnhancer.enhance(query, context);

        assertNotNull(enhancedQueries);
        assertTrue(enhancedQueries.size() >= 1);
    }

    @Test
    @DisplayName("增强查询 - 空查询返回原始空查询")
    void enhance_withEmptyQuery_returnsEmptyQuery() {
        String query = "";
        QueryContext context = QueryContext.of("kb-uuid-123", query);

        List<String> enhancedQueries = queryEnhancer.enhance(query, context);

        assertNotNull(enhancedQueries);
        assertEquals(1, enhancedQueries.size());
        assertEquals("", enhancedQueries.get(0));
    }

    @Test
    @DisplayName("增强查询 - 禁用查询改写时只返回原始查询")
    void enhance_withQueryRewriteDisabled_returnsOnlyOriginalQuery() {
        when(queryEnhancerConfig.getEnableQueryRewrite()).thenReturn(false);
        when(queryEnhancerConfig.getEnableQueryExpansion()).thenReturn(false);

        String query = "如何使用Docker";
        QueryContext context = QueryContext.of("kb-uuid-123", query);

        List<String> enhancedQueries = queryEnhancer.enhance(query, context);

        assertNotNull(enhancedQueries);
        assertEquals(1, enhancedQueries.size());
        assertEquals(query, enhancedQueries.get(0));
    }

    @Test
    @DisplayName("增强查询 - 限制最大增强查询数量")
    void enhance_exceedsMaxQueries_limitsToMax() {
        when(queryEnhancerConfig.getMaxEnhancedQueries()).thenReturn(2);

        String query = "如何使用Docker部署应用";
        QueryContext context = QueryContext.of("kb-uuid-123", query);

        List<String> enhancedQueries = queryEnhancer.enhance(query, context);

        assertNotNull(enhancedQueries);
        assertTrue(enhancedQueries.size() <= 2);
    }

    @Test
    @DisplayName("增强查询 - 缩写展开")
    void enhance_withAbbreviation_expandsAbbreviation() {
        String query = "RAG的配置方法";
        QueryContext context = QueryContext.of("kb-uuid-123", query);

        List<String> enhancedQueries = queryEnhancer.enhance(query, context);

        assertNotNull(enhancedQueries);
        assertTrue(enhancedQueries.size() >= 1);
    }

    @Test
    @DisplayName("意图识别 - 英文关键词识别")
    void recognizeIntent_withEnglishKeywords_recognizesCorrectly() {
        String query = "how to install docker";
        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        assertEquals(QueryIntent.PROCEDURE, intent);
    }

    @Test
    @DisplayName("意图识别 - 中英文混合识别")
    void recognizeIntent_mixedLanguage_recognizesCorrectly() {
        String query = "Git的workflow是什么？";
        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        assertEquals(QueryIntent.FACTS, intent);
    }
}
