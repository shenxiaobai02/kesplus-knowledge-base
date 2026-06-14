package com.kes.reranker;

import com.kes.entity.HybridRetrievalResult;
import com.kes.entity.RerankedResult;

import java.util.List;

/**
 * 重排序接口
 * 定义重排序策略的统一接口
 */
public interface Reranker {

    /**
     * 对混合检索结果进行重排序
     *
     * @param query  查询内容
     * @param results 混合检索结果列表
     * @return 重排序后的结果列表
     */
    List<RerankedResult> rerank(String query, List<HybridRetrievalResult> results);

    /**
     * 获取重排序器类型
     *
     * @return 重排序器类型（score, llm）
     */
    String getType();
}