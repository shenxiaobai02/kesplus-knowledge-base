package com.kes.reranker;

import com.kes.entity.HybridRetrievalResult;
import com.kes.entity.RerankedResult;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM重排序实现
 * 使用LLM评估语义相关性进行重排序
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rag.reranker.type", havingValue = "llm")
public class LlmReranker implements Reranker {

    @Autowired(required = false)
    private ChatModel chatModel;

    private static final String RERANK_PROMPT_TEMPLATE = """
        你是一个相关性评估专家。请根据用户查询，评估以下文档片段的相关性。
        
        用户查询：
        {query}
        
        文档片段：
        {content}
        
        请评估该文档片段与用户查询的相关性，并给出一个0到1之间的分数。
        分数标准：
        - 0.0-0.3: 不相关或相关性极低
        - 0.4-0.6: 有一定相关性，但不够直接
        - 0.7-0.9: 相关性较高，能回答部分问题
        - 1.0: 完全相关，能准确回答问题
        
        请直接输出分数值（仅数字，不要其他内容）。
        """;

    @Override
    public List<RerankedResult> rerank(String query, List<HybridRetrievalResult> results) {
        log.info("LLM-based reranking for query: {}, results: {}", query, results.size());

        if (chatModel == null) {
            log.warn("ChatModel not available, falling back to score-based reranking");
            return fallbackRerank(query, results);
        }

        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        List<RerankedResult> rerankedResults = new ArrayList<>();

        // 使用LLM评估每个结果的相关性
        for (int i = 0; i < results.size(); i++) {
            HybridRetrievalResult hybridResult = results.get(i);
            RerankedResult rerankedResult = RerankedResult.fromHybridResult(hybridResult, i + 1);

            try {
                // 使用LLM评估相关性
                double llmScore = evaluateRelevanceWithLLM(query, hybridResult.getContent());
                rerankedResult.setRerankedScore(llmScore);
                rerankedResult.setReason("LLM评估分数: " + llmScore);
            } catch (Exception e) {
                log.warn("LLM evaluation failed for result {}: {}", i, e.getMessage());
                // 降级使用原始分数
                rerankedResult.setRerankedScore(hybridResult.getCombinedScore());
                rerankedResult.setReason("LLM评估失败，使用原始分数");
            }

            rerankedResults.add(rerankedResult);
        }

        // 按LLM评估分数重新排序
        rerankedResults.sort((a, b) -> Double.compare(b.getRerankedScore(), a.getRerankedScore()));

        // 更新排序位置
        for (int i = 0; i < rerankedResults.size(); i++) {
            rerankedResults.get(i).setRank(i + 1);
        }

        log.info("LLM-based reranking completed, final results: {}", rerankedResults.size());
        return rerankedResults;
    }

    /**
     * 使用LLM评估相关性
     *
     * @param query  查询内容
     * @param content 文档内容
     * @return 相关性分数（0-1）
     */
    private double evaluateRelevanceWithLLM(String query, String content) {
        if (query == null || content == null) {
            return 0;
        }

        // 构建评估提示
        String prompt = RERANK_PROMPT_TEMPLATE
            .replace("{query}", query)
            .replace("{content}", content);

        // 调用LLM
        String responseText = chatModel.chat(prompt);

        // 解析分数
        try {
            double score = Double.parseDouble(responseText.trim());
            // 确保分数在0-1范围内
            return Math.min(1.0, Math.max(0.0, score));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse LLM score: {}", responseText);
            return 0.5; // 默认中等分数
        }
    }

    /**
     * 降级重排序（当LLM不可用时）
     *
     * @param query  查询内容
     * @param results 混合检索结果
     * @return 重排序结果
     */
    private List<RerankedResult> fallbackRerank(String query, List<HybridRetrievalResult> results) {
        List<RerankedResult> rerankedResults = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            HybridRetrievalResult hybridResult = results.get(i);
            RerankedResult rerankedResult = RerankedResult.fromHybridResult(hybridResult, i + 1);
            rerankedResult.setReason("LLM不可用，使用原始分数");
            rerankedResults.add(rerankedResult);
        }

        // 按原始分数排序
        rerankedResults.sort((a, b) -> Double.compare(b.getRerankedScore(), a.getRerankedScore()));

        // 更新排序位置
        for (int i = 0; i < rerankedResults.size(); i++) {
            rerankedResults.get(i).setRank(i + 1);
        }

        return rerankedResults;
    }

    @Override
    public String getType() {
        return "llm";
    }
}