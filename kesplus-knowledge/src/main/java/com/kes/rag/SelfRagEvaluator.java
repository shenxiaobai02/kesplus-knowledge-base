package com.kes.rag;

import com.kes.config.RagConfig;
import com.kes.entity.IntentRecognitionResult;
import com.kes.entity.RetrievalDecision;
import com.kes.enums.IntentRecognitionMethod;
import com.kes.enums.QueryIntent;
import com.kes.util.JsonUtil;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Self-RAG自评估器
 * <p>
 * 使用LLM自动评估用户查询是否需要检索知识库，避免不必要的检索开销。
 * 评估结果包括：是否需要检索、判断理由、检索关键词、查询意图。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Component
public class SelfRagEvaluator {

    private static final Set<String> GREETINGS = new HashSet<>(Arrays.asList(
            "你好", "您好", "hi", "hello", "hey", "在吗", "在不在", "嗨", "嘿"
    ));

    private static final Set<String> CASUAL_WORDS = new HashSet<>(Arrays.asList(
            "天气", "今天", "明天", "新闻", "股票", "笑话", "讲个故事"
    ));

    private static final String EVALUATION_PROMPT_TEMPLATE = """
            请判断以下问题是否需要检索知识库来回答。

            问题：%s

            判断标准：
            - 需要检索：问题涉及具体事实、数据、流程、配置等需要从知识库获取的信息
            - 不需要检索：问题可以基于常识回答，或涉及主观判断、创意创作、闲聊等

            请按以下JSON格式回答，只返回JSON，不要有其他内容：
            {
              "needRetrieval": true或false,
              "reason": "判断理由",
              "keywords": ["关键词1", "关键词2"],
              "intent": "FACTS或PROCEDURE或COMPARISON或ANALYSIS或CREATIVE或UNKNOWN"
            }
            """;

    @Autowired(required = false)
    private ChatModel chatModel;

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private QueryEnhancer queryEnhancer;

    @Autowired(required = false)
    private LlmIntentClassifier llmIntentClassifier;

    /**
     * 设置ChatModel（用于测试）
     */
    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 设置RagConfig（用于测试）
     */
    public void setRagConfig(RagConfig ragConfig) {
        this.ragConfig = ragConfig;
    }

    /**
     * 设置QueryEnhancer（用于测试）
     */
    public void setQueryEnhancer(QueryEnhancer queryEnhancer) {
        this.queryEnhancer = queryEnhancer;
    }

    /**
     * 设置LlmIntentClassifier（用于测试）
     */
    public void setLlmIntentClassifier(LlmIntentClassifier llmIntentClassifier) {
        this.llmIntentClassifier = llmIntentClassifier;
    }

    /**
     * 评估查询是否需要检索
     *
     * @param query 用户查询
     * @return 检索决策
     */
    public RetrievalDecision evaluate(String query) {
        return evaluateWithThreshold(query);
    }

    /**
     * 带置信度阈值的评估
     * <p>
     * - 置信度高: 直接使用结果
     * - 置信度低: 触发LLM评估
     * </p>
     *
     * @param query 用户查询
     * @return 检索决策
     */
    public RetrievalDecision evaluateWithThreshold(String query) {
        if (query == null || query.trim().isEmpty()) {
            return RetrievalDecision.noRetrieval("空查询");
        }

        String normalizedQuery = query.toLowerCase().trim();

        if (isCasualQuery(normalizedQuery)) {
            return RetrievalDecision.noRetrieval("闲聊问题，无需检索");
        }

        if (isGreeting(normalizedQuery)) {
            return RetrievalDecision.noRetrieval("问候语，无需检索");
        }

        // 先使用规则进行初步评估
        IntentRecognitionResult ruleResult = ruleBasedRecognition(query);
        double threshold = getConfidenceThreshold();

        // 如果规则匹配置信度高，直接返回
        if (ruleResult.isHighConfidence() && ruleResult.getConfidence() >= threshold) {
            log.debug("Rule-based recognition confidence is high enough: {}", ruleResult.getConfidence());
            RetrievalDecision decision = buildDecisionFromIntent(query, ruleResult);
            decision.setReason(decision.getReason() + " (规则匹配高置信度)");
            return decision;
        }

        // 置信度不够，尝试LLM评估
        if (llmIntentClassifier != null && llmIntentClassifier.isAvailable()) {
            try {
                IntentRecognitionResult llmResult = llmIntentClassifier.classify(query);
                log.debug("LLM classification result: intent={}, confidence={}",
                        llmResult.getIntent(), llmResult.getConfidence());

                RetrievalDecision decision = buildDecisionFromIntent(query, llmResult);
                decision.setReason(decision.getReason() + " (LLM分类)");
                return decision;
            } catch (Exception e) {
                log.warn("LLM classification failed, fallback to rule-based: {}", e.getMessage());
            }
        }

        // LLM不可用或失败，使用规则评估
        log.debug("Using rule-based evaluation");
        RetrievalDecision decision = buildDecisionFromIntent(query, ruleResult);
        decision.setReason(decision.getReason() + " (规则评估)");
        return decision;
    }

    /**
     * 基于规则的意图识别（带置信度）
     */
    private IntentRecognitionResult ruleBasedRecognition(String query) {
        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        double confidence = calculateRuleBasedConfidence(intent);
        return IntentRecognitionResult.ruleBased(intent).toBuilder()
                .confidence(confidence)
                .reasoning("基于关键词规则匹配")
                .build();
    }

    /**
     * 根据意图计算机规则匹配的置信度
     */
    private double calculateRuleBasedConfidence(QueryIntent intent) {
        if (intent == QueryIntent.UNKNOWN) {
            return 0.3;
        }
        // 知识检索类意图置信度较高
        if (intent.isKnowledgeRetrieval()) {
            return 0.8;
        }
        // 非知识检索类意图置信度高
        if (intent == QueryIntent.GREETING || intent == QueryIntent.CASUAL) {
            return 0.95;
        }
        return 0.6;
    }

    /**
     * 根据意图识别结果构建检索决策
     */
    private RetrievalDecision buildDecisionFromIntent(String query, IntentRecognitionResult intentResult) {
        RetrievalDecision decision = new RetrievalDecision();
        decision.setIntent(intentResult.getIntent());

        List<String> keywords = extractKeywords(query);
        decision.setKeywords(keywords);

        boolean needRetrieval;
        QueryIntent intent = intentResult.getIntent();

        if (intent == QueryIntent.UNKNOWN) {
            // UNKNOWN意图时，检查是否包含技术术语
            needRetrieval = containsTechnicalTerms(query.toLowerCase());
        } else {
            needRetrieval = intent.shouldRetrieve();
        }

        decision.setNeedRetrieval(needRetrieval);

        if (needRetrieval) {
            decision.setReason("意图[" + intentResult.getIntent().name() + "]需要检索知识库");
        } else {
            decision.setReason("意图[" + intentResult.getIntent().name() + "]无需检索");
        }

        return decision;
    }

    /**
     * 获取置信度阈值
     */
    private double getConfidenceThreshold() {
        if (ragConfig != null
                && ragConfig.getQueryEnhancer() != null
                && ragConfig.getQueryEnhancer().getSelfRagThreshold() != null) {
            return ragConfig.getQueryEnhancer().getSelfRagThreshold() / 100.0;
        }
        return 0.7;
    }

    /**
     * 基于LLM的评估
     */
    private RetrievalDecision llmBasedEvaluation(String query) {
        String prompt = String.format(EVALUATION_PROMPT_TEMPLATE, query);

        try {
            dev.langchain4j.model.chat.response.ChatResponse response = chatModel.chat(
                    dev.langchain4j.data.message.UserMessage.from(prompt)
            );

            String content = response.aiMessage().text();
            return parseLlmResponse(content, query);

        } catch (Exception e) {
            log.error("LLM evaluation error: {}", e.getMessage());
            throw new RuntimeException("LLM evaluation failed", e);
        }
    }

    /**
     * 解析LLM响应
     */
    private RetrievalDecision parseLlmResponse(String response, String originalQuery) {
        RetrievalDecision decision = new RetrievalDecision();

        try {
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                return ruleBasedEvaluation(originalQuery);
            }

            Map<String, Object> jsonMap = JsonUtil.fromJson(jsonStr, Map.class);
            if (jsonMap == null) {
                return ruleBasedEvaluation(originalQuery);
            }

            Object needRetrievalObj = jsonMap.get("needRetrieval");
            if (needRetrievalObj != null) {
                decision.setNeedRetrieval(Boolean.TRUE.equals(needRetrievalObj)
                        || "true".equalsIgnoreCase(String.valueOf(needRetrievalObj)));
            }

            Object reasonObj = jsonMap.get("reason");
            if (reasonObj != null) {
                decision.setReason(String.valueOf(reasonObj));
            }

            Object keywordsObj = jsonMap.get("keywords");
            if (keywordsObj instanceof List) {
                List<?> keywordsList = (List<?>) keywordsObj;
                for (Object keyword : keywordsList) {
                    decision.addKeyword(String.valueOf(keyword));
                }
            }

            Object intentObj = jsonMap.get("intent");
            if (intentObj != null) {
                try {
                    decision.setIntent(QueryIntent.valueOf(String.valueOf(intentObj).toUpperCase()));
                } catch (IllegalArgumentException e) {
                    decision.setIntent(QueryIntent.UNKNOWN);
                }
            }

            if (decision.getKeywords().isEmpty()) {
                decision.setKeywords(extractKeywords(originalQuery));
            }

        } catch (Exception e) {
            log.warn("Failed to parse LLM response: {}", e.getMessage());
            return ruleBasedEvaluation(originalQuery);
        }

        return decision;
    }

    /**
     * 提取JSON字符串
     */
    private String extractJson(String response) {
        if (response == null) {
            return null;
        }

        int startIdx = response.indexOf("{");
        int endIdx = response.lastIndexOf("}");

        if (startIdx >= 0 && endIdx > startIdx) {
            return response.substring(startIdx, endIdx + 1);
        }

        return null;
    }

    /**
     * 基于规则的评估
     */
    private RetrievalDecision ruleBasedEvaluation(String query) {
        RetrievalDecision decision = new RetrievalDecision();

        QueryIntent intent = queryEnhancer.recognizeIntent(query);
        decision.setIntent(intent);

        List<String> keywords = extractKeywords(query);
        decision.setKeywords(keywords);

        boolean needRetrieval = decideNeedRetrieval(query, intent);
        decision.setNeedRetrieval(needRetrieval);

        if (needRetrieval) {
            decision.setReason("基于规则评估：问题涉及知识库内容");
        } else {
            decision.setReason("基于规则评估：问题无需知识库检索");
        }

        return decision;
    }

    /**
     * 根据查询和意图决定是否需要检索
     */
    private boolean decideNeedRetrieval(String query, QueryIntent intent) {
        String lowerQuery = query.toLowerCase();

        if (intent == QueryIntent.FACTS || intent == QueryIntent.PROCEDURE) {
            return true;
        }

        if (intent == QueryIntent.COMPARISON || intent == QueryIntent.ANALYSIS) {
            return true;
        }

        if (intent == QueryIntent.CREATIVE) {
            return !isSubjectiveCreativeQuery(query);
        }

        return containsTechnicalTerms(lowerQuery);
    }

    /**
     * 检查是否包含技术术语
     */
    private boolean containsTechnicalTerms(String query) {
        Set<String> technicalTerms = new HashSet<>(Arrays.asList(
                "配置", "安装", "部署", "使用", "设置", "方法", "步骤", "教程",
                "配置", "安装", "部署", "使用", "设置", "方法", "步骤", "教程",
                "config", "install", "deploy", "setup", "use", "method"
        ));

        for (String term : technicalTerms) {
            if (query.contains(term)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为主观创意性问题
     */
    private boolean isSubjectiveCreativeQuery(String query) {
        Set<String> subjectiveTerms = new HashSet<>(Arrays.asList(
                "你觉得", "你认为", "你的看法", "想象一下", "假如你会"
        ));

        for (String term : subjectiveTerms) {
            if (query.contains(term)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();

        String[] words = query.split("[\\s，。、,.!?，。！？、]+");

        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 10) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    /**
     * 判断是否为闲聊问题
     */
    private boolean isCasualQuery(String query) {
        for (String casualWord : CASUAL_WORDS) {
            if (query.contains(casualWord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为问候语
     */
    private boolean isGreeting(String query) {
        for (String greeting : GREETINGS) {
            if (query.contains(greeting)) {
                return true;
            }
        }
        return false;
    }
}
