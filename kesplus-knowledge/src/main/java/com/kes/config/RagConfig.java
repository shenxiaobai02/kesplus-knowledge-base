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

    @Data
    public static class RetrieverConfig {
        private Integer timeoutMs;
        private Integer retryCount;
        private Integer batchSize;
        private Integer parallelism;
    }

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
    public static class LlmConfig {
        private String modelType;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Integer timeoutMs;
        private Double temperature;
        private Integer maxTokens;
    }
}
