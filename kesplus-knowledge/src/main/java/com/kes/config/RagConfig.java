package com.kes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {
    private Integer maxSegmentSize;
    private Integer maxRetrieveResults;
    private Double minScore;
    private Boolean enableGraphRag;
    private Boolean enableSelfRag;
    private Boolean enableCache;

    private RetrieverConfig retriever;
    private EmbeddingConfig embedding;
    private CacheConfig cache;
    private LlmConfig llm;
    private GraphConfig graph;
    private RerankerConfig reranker;
    private QueryEnhancerConfig queryEnhancer;

    @Data
    public static class EmbeddingConfig {
        private String modelType;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Integer batchSize;
        private Integer embeddingDimension;
        private String pooling;
        private Boolean normalize;
    }

    @Data
    public static class CacheConfig {
        private Long ttlMinutes;
        private Integer maxSize;
        private String cacheType;
    }

    @Data
    public static class RetrieverConfig {
        private Integer timeoutMs;
        private Integer retryCount;
        private Integer batchSize;
        private Integer parallelism;
    }

    @Data
    public static class LlmConfig {
        private String modelType;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Integer timeoutMs;
        private Double temperature;
        private Integer maxTokens;
    }

    @Data
    public static class GraphConfig {
        private String storageType;  // neo4j/postgresql
        private Neo4jConfig neo4j;
    }

    @Data
    public static class Neo4jConfig {
        private String uri;
        private String username;
        private String password;
    }

    @Data
    public static class RerankerConfig {
        private String type;  // score/llm
        private Double vectorWeight;
        private Double graphWeight;
    }

    @Data
    public static class QueryEnhancerConfig {
        /**
         * 是否启用查询增强
         */
        private Boolean enabled = true;

        /**
         * 是否启用查询改写
         */
        private Boolean enableQueryRewrite = true;

        /**
         * 是否启用查询扩展
         */
        private Boolean enableQueryExpansion = true;

        /**
         * 是否启用意图识别
         */
        private Boolean enableIntentRecognition = true;

        /**
         * 是否启用结构化转换改写
         */
        private Boolean structuralTransformEnabled = true;

        /**
         * 最大增强查询数量
         */
        private Integer maxEnhancedQueries = 3;

        /**
         * Self-RAG评分阈值
         */
        private Integer selfRagThreshold = 70;

        /**
         * 是否启用多轮对话上下文管理
         */
        private Boolean conversationContextEnabled = true;

        /**
         * 对话上下文TTL（分钟）
         */
        private Integer conversationContextTtlMinutes = 30;
    }
}
