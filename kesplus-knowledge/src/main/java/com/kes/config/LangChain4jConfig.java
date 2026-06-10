package com.kes.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaLanguageModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    private final RagConfig ragConfig;

    public LangChain4jConfig(RagConfig ragConfig) {
        this.ragConfig = ragConfig;
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        RagConfig.EmbeddingConfig embeddingConfig = ragConfig.getEmbedding();
        
        return switch (embeddingConfig.getModelType().toLowerCase()) {
            case "ollama" -> OllamaEmbeddingModel.builder()
                    .baseUrl(embeddingConfig.getBaseUrl())
                    .modelName(embeddingConfig.getModelName())
                    .build();
            case "huggingface", "siliconflow", "openai" -> OpenAiEmbeddingModel.builder()
                    .baseUrl(embeddingConfig.getBaseUrl())
                    .apiKey(embeddingConfig.getApiKey())
                    .modelName(embeddingConfig.getModelName())
                    .build();
            default -> OllamaEmbeddingModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("all-minilm")
                    .build();
        };
    }

    @Bean
    public OllamaLanguageModel ollamaLanguageModel() {
        RagConfig.LlmConfig llmConfig = ragConfig.getLlm();
        
        return OllamaLanguageModel.builder()
                .baseUrl(llmConfig.getBaseUrl())
                .modelName(llmConfig.getModelName())
                .timeout(java.time.Duration.ofMillis(llmConfig.getTimeoutMs()))
                .temperature(llmConfig.getTemperature())
                .build();
    }
}
