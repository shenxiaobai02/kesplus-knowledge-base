package com.kes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {
    private int maxSegmentSize = 1000;
    private int maxRetrieveResults = 5;
    private double minScore = 0.6;
    private boolean enableGraphRag = true;
    private boolean enableSelfRag = false;
    private boolean enableCache = true;

    private RetrieverConfig retriever = new RetrieverConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private CacheConfig cache = new CacheConfig();
    private LlmConfig llm = new LlmConfig();

    @Data
    public static class RetrieverConfig {
        private int timeoutMs = 30000;
        private int retryCount = 3;
        private int batchSize = 100;
        private int parallelism = 4;
    }

    @Data
    public static class EmbeddingConfig {
        private String modelType = "huggingface";
        private String baseUrl = "https://api.siliconflow.cn/v1";
        private String apiKey = "";
        private String modelName = "BAAI/bge-m3";
        private int batchSize = 32;
        private int embeddingDimension = 1024;
        private String pooling = "mean";
        private boolean normalize = true;
    }

    @Data
    public static class CacheConfig {
        private long ttlMinutes = 30;
        private int maxSize = 10000;
        private String cacheType = "caffeine";
    }

    @Data
    public static class LlmConfig {
        private String modelType = "ollama";
        private String baseUrl = "http://localhost:11434";
        private String modelName = "qwen2";
        private int timeoutMs = 60000;
        private double temperature = 0.7;
        private int maxTokens = 4096;
    }
}
