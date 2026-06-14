package com.kes.service;

import com.kes.config.RagConfig;
import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseEmbedding;
import com.kes.entity.GraphRetrievalResult;
import com.kes.entity.HybridRetrievalResult;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 混合检索器
 * 实现向量检索与图谱检索的并行执行和结果合并
 */
@Slf4j
@Service
public class HybridRetriever {

    @Autowired
    private EmbeddingRagService embeddingRagService;

    @Autowired
    private GraphRagService graphRagService;

    @Autowired
    private RagConfig ragConfig;

    /**
     * 并行执行向量检索和图谱检索
     *
     * @param kb            知识库
     * @param query         查询内容
     * @param embeddingModel 嵌入模型
     * @return 混合检索结果列表
     */
    public List<HybridRetrievalResult> retrieve(KnowledgeBase kb, String query, EmbeddingModel embeddingModel) {
        log.info("Starting hybrid retrieval for kb: {}, query: {}", kb.getUuid(), query);

        // 获取配置参数
        int maxResults = ragConfig.getMaxRetrieveResults() != null ? ragConfig.getMaxRetrieveResults() : 10;
        int timeoutMs = ragConfig.getRetriever() != null && ragConfig.getRetriever().getTimeoutMs() != null 
            ? ragConfig.getRetriever().getTimeoutMs() : 5000;
        double vectorWeight = 0.6;  // 默认向量权重
        double graphWeight = 0.4;   // 默认图谱权重

        // 并行执行向量检索和图谱检索
        CompletableFuture<List<KnowledgeBaseEmbedding>> vectorFuture = 
            CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("Starting vector retrieval");
                    List<KnowledgeBaseEmbedding> results = embeddingRagService.retrieve(kb, query, embeddingModel);
                    log.debug("Vector retrieval completed, found {} results", results.size());
                    return results;
                } catch (Exception e) {
                    log.error("Vector retrieval failed: {}", e.getMessage(), e);
                    return Collections.emptyList();
                }
            });

        CompletableFuture<List<GraphRetrievalResult>> graphFuture = 
            CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("Starting graph retrieval");
                    List<GraphRetrievalResult> results = graphRagService.retrieve(kb, query, maxResults);
                    log.debug("Graph retrieval completed, found {} results", results.size());
                    return results;
                } catch (Exception e) {
                    log.error("Graph retrieval failed: {}", e.getMessage(), e);
                    return Collections.emptyList();
                }
            });

        // 等待两个检索任务完成（带超时控制）
        try {
            List<KnowledgeBaseEmbedding> vectorResults = vectorFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            List<GraphRetrievalResult> graphResults = graphFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            
            log.info("Hybrid retrieval completed: vector={}, graph={}", vectorResults.size(), graphResults.size());
            
            // 合并结果
            return mergeResults(vectorResults, graphResults, vectorWeight, graphWeight, maxResults);
        } catch (Exception e) {
            log.error("Hybrid retrieval timeout or error: {}", e.getMessage(), e);
            
            // 降级策略：返回已完成的检索结果
            List<HybridRetrievalResult> fallbackResults = new ArrayList<>();
            
            // 尝试获取向量检索结果
            if (vectorFuture.isDone()) {
                try {
                    List<KnowledgeBaseEmbedding> vectorResults = vectorFuture.get();
                    for (KnowledgeBaseEmbedding embedding : vectorResults) {
                        HybridRetrievalResult result = HybridRetrievalResult.fromVector(embedding, 1.0);
                        result.calculateCombinedScore(vectorWeight, graphWeight);
                        fallbackResults.add(result);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to get vector results in fallback: {}", ex.getMessage());
                }
            }
            
            // 尝试获取图谱检索结果
            if (graphFuture.isDone()) {
                try {
                    List<GraphRetrievalResult> graphResults = graphFuture.get();
                    for (GraphRetrievalResult graphResult : graphResults) {
                        HybridRetrievalResult result = HybridRetrievalResult.fromGraph(graphResult);
                        result.calculateCombinedScore(vectorWeight, graphWeight);
                        fallbackResults.add(result);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to get graph results in fallback: {}", ex.getMessage());
                }
            }
            
            // 按分数排序并限制数量
            fallbackResults.sort((a, b) -> Double.compare(b.getCombinedScore(), a.getCombinedScore()));
            return fallbackResults.size() > maxResults ? fallbackResults.subList(0, maxResults) : fallbackResults;
        }
    }

    /**
     * 合并向量检索和图谱检索结果
     *
     * @param vectorResults 向量检索结果
     * @param graphResults  图谱检索结果
     * @param vectorWeight  向量权重
     * @param graphWeight   图谱权重
     * @param maxResults    最大返回数量
     * @return 合并后的结果列表
     */
    private List<HybridRetrievalResult> mergeResults(
            List<KnowledgeBaseEmbedding> vectorResults,
            List<GraphRetrievalResult> graphResults,
            double vectorWeight,
            double graphWeight,
            int maxResults) {
        
        log.debug("Merging results: vector={}, graph={}, weights={}/{}, max={}", 
            vectorResults.size(), graphResults.size(), vectorWeight, graphWeight, maxResults);

        // 使用Map进行去重，key为内容文本
        Map<String, HybridRetrievalResult> mergedMap = new HashMap<>();

        // 处理向量检索结果
        for (KnowledgeBaseEmbedding embedding : vectorResults) {
            String content = embedding.getText();
            if (content != null && !content.isEmpty()) {
                HybridRetrievalResult result = HybridRetrievalResult.fromVector(embedding, 1.0);
                result.calculateCombinedScore(vectorWeight, graphWeight);
                mergedMap.put(content, result);
            }
        }

        // 处理图谱检索结果，合并相同内容的分数
        for (GraphRetrievalResult graphResult : graphResults) {
            String content = graphResult.getContent();
            if (content != null && !content.isEmpty()) {
                HybridRetrievalResult existing = mergedMap.get(content);
                if (existing != null) {
                    // 内容已存在，合并分数
                    existing.setGraphScore(graphResult.getScore());
                    existing.calculateCombinedScore(vectorWeight, graphWeight);
                } else {
                    // 内容不存在，创建新结果
                    HybridRetrievalResult result = HybridRetrievalResult.fromGraph(graphResult);
                    result.calculateCombinedScore(vectorWeight, graphWeight);
                    mergedMap.put(content, result);
                }
            }
        }

        // 转换为列表并按综合分数排序
        List<HybridRetrievalResult> mergedResults = new ArrayList<>(mergedMap.values());
        mergedResults.sort((a, b) -> Double.compare(b.getCombinedScore(), a.getCombinedScore()));

        // 限制返回数量
        if (mergedResults.size() > maxResults) {
            mergedResults = mergedResults.subList(0, maxResults);
        }

        log.info("Merged results: total={}, unique={}, final={}", 
            vectorResults.size() + graphResults.size(), mergedMap.size(), mergedResults.size());

        return mergedResults;
    }

    /**
     * 仅向量检索（降级模式）
     *
     * @param kb            知识库
     * @param query         查询内容
     * @param embeddingModel 嵌入模型
     * @return 混合检索结果列表
     */
    public List<HybridRetrievalResult> retrieveVectorOnly(KnowledgeBase kb, String query, EmbeddingModel embeddingModel) {
        log.info("Vector-only retrieval for kb: {}, query: {}", kb.getUuid(), query);
        
        int maxResults = ragConfig.getMaxRetrieveResults() != null ? ragConfig.getMaxRetrieveResults() : 10;
        double vectorWeight = 1.0;
        double graphWeight = 0.0;

        List<KnowledgeBaseEmbedding> vectorResults = embeddingRagService.retrieve(kb, query, embeddingModel);
        
        List<HybridRetrievalResult> results = new ArrayList<>();
        for (KnowledgeBaseEmbedding embedding : vectorResults) {
            HybridRetrievalResult result = HybridRetrievalResult.fromVector(embedding, 1.0);
            result.calculateCombinedScore(vectorWeight, graphWeight);
            results.add(result);
        }

        // 按分数排序并限制数量
        results.sort((a, b) -> Double.compare(b.getCombinedScore(), a.getCombinedScore()));
        return results.size() > maxResults ? results.subList(0, maxResults) : results;
    }

    /**
     * 仅图谱检索（降级模式）
     *
     * @param kb    知识库
     * @param query 查询内容
     * @return 混合检索结果列表
     */
    public List<HybridRetrievalResult> retrieveGraphOnly(KnowledgeBase kb, String query) {
        log.info("Graph-only retrieval for kb: {}, query: {}", kb.getUuid(), query);
        
        int maxResults = ragConfig.getMaxRetrieveResults() != null ? ragConfig.getMaxRetrieveResults() : 10;
        double vectorWeight = 0.0;
        double graphWeight = 1.0;

        List<GraphRetrievalResult> graphResults = graphRagService.retrieve(kb, query, maxResults);
        
        List<HybridRetrievalResult> results = new ArrayList<>();
        for (GraphRetrievalResult graphResult : graphResults) {
            HybridRetrievalResult result = HybridRetrievalResult.fromGraph(graphResult);
            result.calculateCombinedScore(vectorWeight, graphWeight);
            results.add(result);
        }

        // 按分数排序并限制数量
        results.sort((a, b) -> Double.compare(b.getCombinedScore(), a.getCombinedScore()));
        return results.size() > maxResults ? results.subList(0, maxResults) : results;
    }
}