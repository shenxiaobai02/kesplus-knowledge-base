package com.kes.reranker;

import com.kes.entity.HybridRetrievalResult;
import com.kes.entity.RerankedResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 分数加权重排序实现
 * 基于向量分数和图谱分数的加权组合进行重排序
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rag.reranker.type", havingValue = "score", matchIfMissing = true)
public class ScoreBasedReranker implements Reranker {

    @Override
    public List<RerankedResult> rerank(String query, List<HybridRetrievalResult> results) {
        log.info("Score-based reranking for query: {}, results: {}", query, results != null ? results.size() : 0);

        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        List<RerankedResult> rerankedResults = new ArrayList<>();

        // 按综合分数排序（已在HybridRetriever中完成）
        // 这里主要进行分数调整和相关性增强
        for (int i = 0; i < results.size(); i++) {
            HybridRetrievalResult hybridResult = results.get(i);
            RerankedResult rerankedResult = RerankedResult.fromHybridResult(hybridResult, i + 1);

            // 分数调整策略
            double adjustedScore = calculateAdjustedScore(query, hybridResult);
            rerankedResult.setRerankedScore(adjustedScore);

            rerankedResults.add(rerankedResult);
        }

        // 按调整后的分数重新排序
        rerankedResults.sort((a, b) -> Double.compare(b.getRerankedScore(), a.getRerankedScore()));

        // 更新排序位置
        for (int i = 0; i < rerankedResults.size(); i++) {
            rerankedResults.get(i).setRank(i + 1);
        }

        log.info("Score-based reranking completed, final results: {}", rerankedResults.size());
        return rerankedResults;
    }

    /**
     * 计算调整后的分数
     * 基于查询相关性进行分数增强
     *
     * @param query        查询内容
     * @param hybridResult 混合检索结果
     * @return 调整后的分数
     */
    private double calculateAdjustedScore(String query, HybridRetrievalResult hybridResult) {
        double baseScore = hybridResult.getCombinedScore();

        // 查询关键词匹配增强
        double keywordMatchBoost = calculateKeywordMatchBoost(query, hybridResult.getContent());

        // 来源类型权重调整
        double sourceTypeBoost = calculateSourceTypeBoost(hybridResult.getSourceType());

        // 最终分数 = 基础分数 + 关键词匹配增强 + 来源类型权重
        double adjustedScore = baseScore + keywordMatchBoost + sourceTypeBoost;

        // 确保分数在合理范围内（0-1）
        return Math.min(1.0, Math.max(0.0, adjustedScore));
    }

    /**
     * 计算关键词匹配增强分数
     *
     * @param query  查询内容
     * @param content 结果内容
     * @return 增强分数
     */
    private double calculateKeywordMatchBoost(String query, String content) {
        if (query == null || content == null) {
            return 0;
        }

        // 提取查询关键词
        String[] queryWords = query.toLowerCase().split("[\\s,，。！？；;：:\"\"''\\[\\]()（）]+");
        String lowerContent = content.toLowerCase();

        int matchCount = 0;
        for (String word : queryWords) {
            if (word.length() >= 2 && lowerContent.contains(word)) {
                matchCount++;
            }
        }

        // 关键词匹配增强：匹配比例 * 0.1
        return queryWords.length > 0 ? (double) matchCount / queryWords.length * 0.1 : 0;
    }

    /**
     * 计算来源类型权重调整
     *
     * @param sourceType 来源类型
     * @return 权重调整值
     */
    private double calculateSourceTypeBoost(String sourceType) {
        if (sourceType == null) {
            return 0;
        }

        // HYBRID来源（向量+图谱）给予额外权重
        if ("HYBRID".equals(sourceType)) {
            return 0.05;
        }

        return 0;
    }

    @Override
    public String getType() {
        return "score";
    }
}