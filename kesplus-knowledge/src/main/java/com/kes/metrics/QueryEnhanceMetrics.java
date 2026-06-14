package com.kes.metrics;

import com.kes.entity.IntentRecognitionResult;
import com.kes.entity.RewriteResult;
import com.kes.enums.IntentRecognitionMethod;
import com.kes.enums.QueryIntent;
import com.kes.enums.RewriteStrategy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 查询增强指标收集器
 * <p>
 * 负责收集和记录查询增强相关的指标，包括：
 * - 意图识别指标（总数、成功率、延迟）
 * - 查询改写指标（总数、置信度分布）
 * - 各维度标签支持（意图类型、识别方法、改写策略）
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rag.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class QueryEnhanceMetrics {

    private final MeterRegistry meterRegistry;

    // 意图识别指标
    private final Counter intentRecognitionTotal;
    private final Counter intentRecognitionSuccess;
    private final Counter intentRecognitionFallback;
    private final Timer intentRecognitionLatency;

    // 查询改写指标
    private final Counter queryRewriteTotal;
    private final Counter queryRewriteEffective;
    private final Timer queryRewriteLatency;
    private final DistributionSummary rewriteConfidence;

    // Self-RAG评估指标
    private final Counter retrievalDecisionTotal;
    private final Counter retrievalDecisionNeedRetrieval;
    private final Counter retrievalDecisionNoRetrieval;

    @Autowired
    public QueryEnhanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化意图识别指标
        this.intentRecognitionTotal = Counter.builder("rag.intent.recognition.total")
                .description("Total intent recognition requests")
                .register(meterRegistry);

        this.intentRecognitionSuccess = Counter.builder("rag.intent.recognition.success")
                .description("Successful intent recognition (high confidence)")
                .register(meterRegistry);

        this.intentRecognitionFallback = Counter.builder("rag.intent.recognition.fallback")
                .description("Intent recognition fallback to LLM")
                .register(meterRegistry);

        this.intentRecognitionLatency = Timer.builder("rag.intent.recognition.latency")
                .description("Intent recognition latency")
                .register(meterRegistry);

        // 初始化查询改写指标
        this.queryRewriteTotal = Counter.builder("rag.query.rewrite.total")
                .description("Total query rewrite requests")
                .register(meterRegistry);

        this.queryRewriteEffective = Counter.builder("rag.query.rewrite.effective")
                .description("Effective query rewrites")
                .register(meterRegistry);

        this.queryRewriteLatency = Timer.builder("rag.query.rewrite.latency")
                .description("Query rewrite latency")
                .register(meterRegistry);

        this.rewriteConfidence = DistributionSummary.builder("rag.query.rewrite.confidence")
                .description("Query rewrite confidence distribution")
                .register(meterRegistry);

        // 初始化Self-RAG指标
        this.retrievalDecisionTotal = Counter.builder("rag.retrieval.decision.total")
                .description("Total retrieval decision requests")
                .register(meterRegistry);

        this.retrievalDecisionNeedRetrieval = Counter.builder("rag.retrieval.decision.need_retrieval")
                .description("Decisions that need retrieval")
                .register(meterRegistry);

        this.retrievalDecisionNoRetrieval = Counter.builder("rag.retrieval.decision.no_retrieval")
                .description("Decisions that don't need retrieval")
                .register(meterRegistry);
    }

    /**
     * 记录意图识别
     */
    public void recordIntentRecognition(QueryIntent intent, double confidence,
                                        IntentRecognitionMethod method, long latencyMs) {
        intentRecognitionTotal.increment();

        // 根据置信度判断是否成功
        if (confidence >= 0.9) {
            intentRecognitionSuccess.increment();
        }

        // 记录fallback
        if (method == IntentRecognitionMethod.LLM_CLASSIFY) {
            intentRecognitionFallback.increment();
        }

        // 记录带标签的延迟
        Tags tags = Tags.of(
                "intent", intent != null ? intent.name() : "UNKNOWN",
                "method", method != null ? method.name() : "UNKNOWN",
                "confidence_level", getConfidenceLevel(confidence)
        );

        meterRegistry.timer("rag.intent.recognition.latency", tags)
                .record(latencyMs, TimeUnit.MILLISECONDS);

        log.debug("Intent recognition metrics recorded: intent={}, confidence={}, method={}, latency={}ms",
                intent, confidence, method, latencyMs);
    }

    /**
     * 记录意图识别结果（从IntentRecognitionResult）
     */
    public void recordIntentRecognition(IntentRecognitionResult result, long latencyMs) {
        if (result == null) {
            recordIntentRecognition(null, 0.0, null, latencyMs);
            return;
        }

        recordIntentRecognition(
                result.getIntent(),
                result.getConfidence(),
                result.getMethod(),
                latencyMs
        );
    }

    /**
     * 记录查询改写
     */
    public void recordQueryRewrite(String originalQuery, RewriteResult result, long latencyMs) {
        queryRewriteTotal.increment();

        if (result != null && result.isEffective()) {
            queryRewriteEffective.increment();
        }

        // 记录改写置信度
        if (result != null) {
            Tags strategyTags = Tags.of("strategy", result.getStrategy() != null ? result.getStrategy().name() : "UNKNOWN");
            meterRegistry.summary("rag.query.rewrite.confidence", strategyTags)
                    .record(result.getConfidence());
        }

        // 记录改写延迟
        queryRewriteLatency.record(latencyMs, TimeUnit.MILLISECONDS);

        log.debug("Query rewrite metrics recorded: originalQuery={}, rewrittenQuery={}, strategy={}, latency={}ms",
                originalQuery, result != null ? result.getRewrittenQuery() : "null",
                result != null ? result.getStrategy() : "null", latencyMs);
    }

    /**
     * 记录检索决策
     */
    public void recordRetrievalDecision(boolean needRetrieval, QueryIntent intent, String reason) {
        retrievalDecisionTotal.increment();

        if (needRetrieval) {
            retrievalDecisionNeedRetrieval.increment();
        } else {
            retrievalDecisionNoRetrieval.increment();
        }

        // 记录意图和原因标签
        Tags tags = Tags.of(
                "intent", intent != null ? intent.name() : "UNKNOWN",
                "need_retrieval", String.valueOf(needRetrieval)
        );

        meterRegistry.counter("rag.retrieval.decision.by_intent", tags).increment();

        log.debug("Retrieval decision recorded: needRetrieval={}, intent={}, reason={}",
                needRetrieval, intent, reason);
    }

    /**
     * 获取置信度等级
     */
    private String getConfidenceLevel(double confidence) {
        if (confidence >= 0.9) {
            return "HIGH";
        } else if (confidence >= 0.7) {
            return "MEDIUM";
        } else if (confidence >= 0.5) {
            return "LOW";
        } else {
            return "VERY_LOW";
        }
    }

    /**
     * 记录指标收集错误
     */
    public void recordError(String operation, Exception e) {
        log.error("Failed to record metrics for operation {}: {}", operation, e.getMessage());
        meterRegistry.counter("rag.metrics.error", Tags.of("operation", operation)).increment();
    }
}
