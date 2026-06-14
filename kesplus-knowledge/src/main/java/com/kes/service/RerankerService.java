package com.kes.service;

import com.kes.config.RagConfig;
import com.kes.entity.HybridRetrievalResult;
import com.kes.entity.RerankedResult;
import com.kes.reranker.Reranker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 重排序服务
 * 集成Reranker接口，根据配置选择重排序策略
 */
@Slf4j
@Service
public class RerankerService {

    @Autowired(required = false)
    private Reranker reranker;

    @Autowired
    private RagConfig ragConfig;

    /**
     * 对混合检索结果进行重排序
     *
     * @param query  查询内容
     * @param results 混合检索结果列表
     * @return 重排序后的结果列表
     */
    public List<RerankedResult> rerank(String query, List<HybridRetrievalResult> results) {
        log.info("Reranking results for query: {}, count: {}", query, results != null ? results.size() : 0);

        if (results == null || results.isEmpty()) {
            log.warn("No results to rerank");
            return java.util.Collections.emptyList();
        }

        // 检查是否启用重排序
        Boolean enableRerank = ragConfig.getEnableGraphRag();
        if (enableRerank == null || !enableRerank) {
            log.info("Reranking disabled, returning original results");
            return createDefaultResults(results);
        }

        // 检查Reranker是否可用
        if (reranker == null) {
            log.warn("Reranker not available, using default sorting");
            return createDefaultResults(results);
        }

        // 使用配置的Reranker进行重排序
        try {
            List<RerankedResult> rerankedResults = reranker.rerank(query, results);
            log.info("Reranking completed with type: {}, results: {}", reranker.getType(), rerankedResults.size());
            return rerankedResults;
        } catch (Exception e) {
            log.error("Reranking failed: {}", e.getMessage(), e);
            // 降级返回原始结果
            return createDefaultResults(results);
        }
    }

    /**
     * 创建默认结果（不进行重排序）
     *
     * @param results 混合检索结果
     * @return 默认重排序结果
     */
    private List<RerankedResult> createDefaultResults(List<HybridRetrievalResult> results) {
        List<RerankedResult> defaultResults = new java.util.ArrayList<>();
        
        // 按原始分数排序
        results.sort((a, b) -> Double.compare(b.getCombinedScore(), a.getCombinedScore()));
        
        for (int i = 0; i < results.size(); i++) {
            HybridRetrievalResult hybridResult = results.get(i);
            RerankedResult rerankedResult = RerankedResult.fromHybridResult(hybridResult, i + 1);
            defaultResults.add(rerankedResult);
        }
        
        return defaultResults;
    }

    /**
     * 获取当前使用的重排序器类型
     *
     * @return 重排序器类型（score, llm, none）
     */
    public String getRerankerType() {
        if (reranker == null) {
            return "none";
        }
        return reranker.getType();
    }
}