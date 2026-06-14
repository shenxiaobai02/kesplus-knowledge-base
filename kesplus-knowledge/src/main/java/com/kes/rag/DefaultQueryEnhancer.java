package com.kes.rag;

import com.kes.config.RagConfig;
import com.kes.dto.QueryContext;
import com.kes.enums.QueryIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 默认查询增强器实现
 * <p>
 * 基于规则的查询增强器，提供查询改写、扩展和意图识别功能。
 * 支持配置化开关控制各功能模块。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Component
public class DefaultQueryEnhancer implements QueryEnhancer {

    private static final Map<QueryIntent, List<String>> INTENT_KEYWORDS;

    static {
        INTENT_KEYWORDS = new HashMap<>();
        INTENT_KEYWORDS.put(QueryIntent.FACTS, Arrays.asList(
                "是谁", "是什么", "什么时候", "在哪里", "多少", "什么是",
                "who", "what", "when", "where", "how many"
        ));
        INTENT_KEYWORDS.put(QueryIntent.PROCEDURE, Arrays.asList(
                "如何", "怎么", "怎样", "步骤", "流程", "方法", "操作", "使用",
                "how to", "how do", "steps", "process", "procedure"
        ));
        INTENT_KEYWORDS.put(QueryIntent.COMPARISON, Arrays.asList(
                "区别", "比较", "不同", "差异", "相比", "对比",
                "difference", "compare", "versus", "vs", "between"
        ));
        INTENT_KEYWORDS.put(QueryIntent.ANALYSIS, Arrays.asList(
                "为什么", "原因", "影响", "结果", "分析", "探讨",
                "why", "cause", "effect", "impact", "analysis"
        ));
        INTENT_KEYWORDS.put(QueryIntent.CREATIVE, Arrays.asList(
                "建议", "想象", "假如", "假设", "创意", "推荐", "意见",
                "suggest", "imagine", "if", "recommend", "idea"
        ));
    }

    private static final Map<String, List<String>> SYNONYMS;

    static {
        SYNONYMS = new HashMap<>();
        SYNONYMS.put("配置", Arrays.asList("设置", "配置", "config", "setting"));
        SYNONYMS.put("安装", Arrays.asList("安装", "部署", "setup", "install", "deploy"));
        SYNONYMS.put("使用", Arrays.asList("使用", "应用", "运用", "use", "usage", "how to use"));
        SYNONYMS.put("问题", Arrays.asList("问题", "故障", "错误", "issue", "error", "bug", "故障"));
        SYNONYMS.put("服务", Arrays.asList("服务", "service", "server", "服务"));
    }

    private static final Set<String> PRONOUNS = new HashSet<>(Arrays.asList(
            "它", "他", "她", "这个", "那个", "这些", "那些", "此", "该"
    ));

    private static final Set<String> GREETINGS = new HashSet<>(Arrays.asList(
            "你好", "您好", "hi", "hello", "hey", "在吗", "在不在"
    ));

    @Autowired
    private RagConfig ragConfig;

    /**
     * 设置RagConfig（用于测试）
     */
    public void setRagConfig(RagConfig ragConfig) {
        this.ragConfig = ragConfig;
    }

    @Override
    public List<String> enhance(String originalQuery, QueryContext context) {
        List<String> enhancedQueries = new ArrayList<>();
        enhancedQueries.add(originalQuery);

        if (!isQueryRewriteEnabled() && !isQueryExpansionEnabled()) {
            return enhancedQueries;
        }

        try {
            if (isQueryRewriteEnabled()) {
                String rewrittenQuery = rewriteQuery(originalQuery);
                if (!rewrittenQuery.equals(originalQuery)) {
                    enhancedQueries.add(rewrittenQuery);
                }
            }

            if (isQueryExpansionEnabled()) {
                List<String> expandedQueries = expandQuery(originalQuery);
                for (String expanded : expandedQueries) {
                    if (!enhancedQueries.contains(expanded)) {
                        enhancedQueries.add(expanded);
                    }
                }
            }

            int maxQueries = getMaxEnhancedQueries();
            if (enhancedQueries.size() > maxQueries) {
                enhancedQueries = enhancedQueries.subList(0, maxQueries);
            }

        } catch (Exception e) {
            log.warn("Query enhancement failed for query: {}, error: {}", originalQuery, e.getMessage());
        }

        return enhancedQueries;
    }

    @Override
    public QueryIntent recognizeIntent(String query) {
        if (query == null || query.trim().isEmpty()) {
            return QueryIntent.UNKNOWN;
        }

        String normalizedQuery = query.toLowerCase().trim();

        if (isGreeting(normalizedQuery)) {
            return QueryIntent.UNKNOWN;
        }

        int maxMatchCount = 0;
        QueryIntent bestMatchIntent = QueryIntent.UNKNOWN;

        for (Map.Entry<QueryIntent, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            int matchCount = countMatches(normalizedQuery, entry.getValue());
            if (matchCount > maxMatchCount) {
                maxMatchCount = matchCount;
                bestMatchIntent = entry.getKey();
            }
        }

        return maxMatchCount > 0 ? bestMatchIntent : QueryIntent.UNKNOWN;
    }

    @Override
    public boolean isEnabled() {
        return ragConfig != null
                && ragConfig.getQueryEnhancer() != null
                && ragConfig.getQueryEnhancer().getEnabled() != null
                && ragConfig.getQueryEnhancer().getEnabled();
    }

    /**
     * 查询改写：将模糊问题转化为精确查询
     */
    private String rewriteQuery(String query) {
        String rewritten = query.trim();

        rewritten = resolvePronouns(rewritten);

        rewritten = expandAbbreviations(rewritten);

        return rewritten;
    }

    /**
     * 指代消解：将代词替换为更明确的表述
     */
    private String resolvePronouns(String query) {
        String result = query;

        if (query.contains("它") || query.contains("他") || query.contains("她")) {
            if (query.contains("它的") || query.contains("他的") || query.contains("她的")) {
                result = result.replaceAll("(它的|他的|她的)", "该");
            }
        }

        if (query.contains("这个") || query.contains("那个")) {
            result = result.replace("这个", "当前").replace("那个", "指定");
        }

        return result;
    }

    /**
     * 展开缩写
     */
    private String expandAbbreviations(String query) {
        String result = query;

        result = result.replaceAll("\\bKB\\b", "知识库");
        result = result.replaceAll("\\bRAG\\b", "检索增强生成");
        result = result.replaceAll("\\bAI\\b", "人工智能");
        result = result.replaceAll("\\bLLM\\b", "大语言模型");

        return result;
    }

    /**
     * 查询扩展：添加同义词和相关概念
     */
    private List<String> expandQuery(String query) {
        List<String> expandedQueries = new ArrayList<>();

        Set<String> processedTerms = new HashSet<>();

        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            String term = entry.getKey();
            List<String> synonyms = entry.getValue();

            if (query.contains(term)) {
                for (String synonym : synonyms) {
                    if (!query.contains(synonym) && !processedTerms.contains(synonym)) {
                        String expanded = query.replace(term, synonym);
                        if (!expanded.equals(query)) {
                            expandedQueries.add(expanded);
                            processedTerms.add(synonym);
                        }
                    }
                }
            }
        }

        String lowerQuery = query.toLowerCase();
        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            for (String synonym : entry.getValue()) {
                if (lowerQuery.contains(synonym.toLowerCase()) && !query.contains(entry.getKey())) {
                    String expanded = query.replaceAll("(?i)" + Pattern.quote(synonym), entry.getKey());
                    if (!expanded.equals(query) && !processedTerms.contains(entry.getKey())) {
                        expandedQueries.add(expanded);
                        processedTerms.add(entry.getKey());
                    }
                }
            }
        }

        return expandedQueries;
    }

    /**
     * 检查是否为问候语
     */
    private boolean isGreeting(String query) {
        for (String greeting : GREETINGS) {
            if (query.contains(greeting)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 统计匹配次数
     */
    private int countMatches(String query, List<String> keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (query.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private boolean isQueryRewriteEnabled() {
        return ragConfig != null
                && ragConfig.getQueryEnhancer() != null
                && ragConfig.getQueryEnhancer().getEnableQueryRewrite() != null
                && ragConfig.getQueryEnhancer().getEnableQueryRewrite();
    }

    private boolean isQueryExpansionEnabled() {
        return ragConfig != null
                && ragConfig.getQueryEnhancer() != null
                && ragConfig.getQueryEnhancer().getEnableQueryExpansion() != null
                && ragConfig.getQueryEnhancer().getEnableQueryExpansion();
    }

    private int getMaxEnhancedQueries() {
        if (ragConfig != null
                && ragConfig.getQueryEnhancer() != null
                && ragConfig.getQueryEnhancer().getMaxEnhancedQueries() != null) {
            return ragConfig.getQueryEnhancer().getMaxEnhancedQueries();
        }
        return 3;
    }
}
