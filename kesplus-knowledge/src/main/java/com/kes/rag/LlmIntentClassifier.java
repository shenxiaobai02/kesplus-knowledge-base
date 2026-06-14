package com.kes.rag;

import com.kes.entity.IntentRecognitionResult;
import com.kes.enums.IntentRecognitionMethod;
import com.kes.enums.QueryIntent;
import com.kes.util.JsonUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLM意图分类器
 * <p>
 * 使用大语言模型进行意图识别，提供更准确的意图分类能力。
 * 当规则匹配无法准确识别时，调用LLM进行分类。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Component
public class LlmIntentClassifier {

    private static final String CLASSIFICATION_PROMPT_TEMPLATE = """
            # Role
            你是一个专业的意图分类专家。

            # 意图定义
            - FACTS: 事实性问题（是什么、是谁、什么时候、在哪里、多少）
            - PROCEDURE: 流程性问题（如何、怎么、步骤、流程、方法）
            - COMPARISON: 比较性问题（区别、比较、差异）
            - ANALYSIS: 分析性问题（为什么、原因、影响）
            - LIST_RETRIEVAL: 列表检索问题（有哪些、列举）
            - CASUAL: 闲聊问题（问候、日常对话）
            - GREETING: 问候问题（你好、您好）
            - SYSTEM_ACTION: 系统操作问题（查询状态、执行操作）
            - CREATIVE: 创意性问题（建议、想象）
            - UNKNOWN: 未知

            # 任务
            分析以下用户查询，判断其意图类型。

            用户查询：%s

            # Output Format (JSON)
            {
              "intent": "意图类型",
              "confidence": 0.95,
              "reasoning": "推理过程"
            }
            """;

    @Autowired(required = false)
    private ChatModel chatModel;

    /**
     * 使用LLM进行意图分类
     *
     * @param query 用户查询
     * @return 意图识别结果
     */
    public IntentRecognitionResult classify(String query) {
        if (chatModel == null) {
            log.warn("ChatModel is not available, cannot perform LLM classification");
            return IntentRecognitionResult.ruleBased(QueryIntent.UNKNOWN);
        }

        try {
            String prompt = String.format(CLASSIFICATION_PROMPT_TEMPLATE, query);
            ChatResponse response = chatModel.chat(
                    dev.langchain4j.data.message.UserMessage.from(prompt)
            );

            String content = response.aiMessage().text();
            return parseLlmResponse(content);

        } catch (Exception e) {
            log.error("LLM classification failed for query: {}, error: {}", query, e.getMessage());
            return IntentRecognitionResult.ruleBased(QueryIntent.UNKNOWN);
        }
    }

    /**
     * 解析LLM响应
     */
    private IntentRecognitionResult parseLlmResponse(String response) {
        try {
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                log.warn("Failed to extract JSON from LLM response");
                return IntentRecognitionResult.ruleBased(QueryIntent.UNKNOWN);
            }

            Map<String, Object> jsonMap = JsonUtil.fromJson(jsonStr, Map.class);
            if (jsonMap == null) {
                log.warn("Failed to parse JSON from LLM response");
                return IntentRecognitionResult.ruleBased(QueryIntent.UNKNOWN);
            }

            String intentStr = String.valueOf(jsonMap.getOrDefault("intent", "UNKNOWN"));
            QueryIntent intent;
            try {
                intent = QueryIntent.valueOf(intentStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown intent from LLM: {}", intentStr);
                intent = QueryIntent.UNKNOWN;
            }

            double confidence = 0.5;
            Object confidenceObj = jsonMap.get("confidence");
            if (confidenceObj != null) {
                if (confidenceObj instanceof Number) {
                    confidence = ((Number) confidenceObj).doubleValue();
                } else {
                    try {
                        confidence = Double.parseDouble(String.valueOf(confidenceObj));
                    } catch (NumberFormatException e) {
                        confidence = 0.5;
                    }
                }
            }

            String reasoning = String.valueOf(jsonMap.getOrDefault("reasoning", ""));

            return IntentRecognitionResult.llmClassified(intent, confidence, reasoning);

        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", e.getMessage());
            return IntentRecognitionResult.ruleBased(QueryIntent.UNKNOWN);
        }
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
     * 检查LLM分类器是否可用
     */
    public boolean isAvailable() {
        return chatModel != null;
    }
}
