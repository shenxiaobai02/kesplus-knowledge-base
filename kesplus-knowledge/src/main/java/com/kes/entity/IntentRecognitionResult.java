package com.kes.entity;

import com.kes.enums.IntentRecognitionMethod;
import com.kes.enums.QueryIntent;
import lombok.Builder;
import lombok.Data;

/**
 * 意图识别结果
 * <p>
 * 包含意图识别的完整结果，包括识别的意图类型、置信度、识别方法和推理过程。
 * 用于记录和分析意图识别的全过程，便于后续优化和评估。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
public class IntentRecognitionResult {

    /**
     * 识别到的意图类型
     */
    private QueryIntent intent;

    /**
     * 置信度 (0.0-1.0)
     */
    private double confidence;

    /**
     * 识别方法
     */
    private IntentRecognitionMethod method;

    /**
     * 推理过程描述
     */
    private String reasoning;

    /**
     * 创建默认的规则匹配结果
     */
    public static IntentRecognitionResult ruleBased(QueryIntent intent) {
        return IntentRecognitionResult.builder()
                .intent(intent)
                .confidence(1.0)
                .method(IntentRecognitionMethod.RULE_BASED)
                .reasoning("基于关键词规则匹配")
                .build();
    }

    /**
     * 创建LLM分类结果
     */
    public static IntentRecognitionResult llmClassified(QueryIntent intent, double confidence, String reasoning) {
        return IntentRecognitionResult.builder()
                .intent(intent)
                .confidence(confidence)
                .method(IntentRecognitionMethod.LLM_CLASSIFY)
                .reasoning(reasoning)
                .build();
    }

    /**
     * 创建RAG增强分类结果
     */
    public static IntentRecognitionResult ragAugmented(QueryIntent intent, double confidence, String reasoning) {
        return IntentRecognitionResult.builder()
                .intent(intent)
                .confidence(confidence)
                .method(IntentRecognitionMethod.RAG_AUGMENTED)
                .reasoning(reasoning)
                .build();
    }

    /**
     * 判断置信度是否足够高
     */
    public boolean isHighConfidence() {
        return confidence >= 0.9;
    }

    /**
     * 判断置信度是否需要fallback
     */
    public boolean needsFallback() {
        return confidence < 0.7;
    }
}
