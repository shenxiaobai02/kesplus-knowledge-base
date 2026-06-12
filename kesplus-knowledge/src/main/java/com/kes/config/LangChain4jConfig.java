package com.kes.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
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
        
        if (embeddingConfig == null || embeddingConfig.getModelType() == null) {
            throw new IllegalStateException("rag.embedding.model-type must be configured");
        }
        
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
            default -> throw new IllegalArgumentException("Unsupported embedding model type: " + embeddingConfig.getModelType());
        };
    }

    @Bean
    public ChatModel chatModel() {
        RagConfig.LlmConfig llmConfig = ragConfig.getLlm();
        
        if (llmConfig == null || llmConfig.getModelType() == null) {
            throw new IllegalStateException("rag.llm.model-type must be configured");
        }
        
        return switch (llmConfig.getModelType().toLowerCase()) {
            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(llmConfig.getBaseUrl())
                    .modelName(llmConfig.getModelName())
                    .timeout(java.time.Duration.ofMillis(llmConfig.getTimeoutMs()))
                    .temperature(llmConfig.getTemperature())
                    .build();
            case "openai", "siliconflow" -> OpenAiChatModel.builder()
                    .baseUrl(llmConfig.getBaseUrl())
                    .apiKey(llmConfig.getApiKey())
                    .modelName(llmConfig.getModelName())
                    .timeout(java.time.Duration.ofMillis(llmConfig.getTimeoutMs()))
                    .temperature(llmConfig.getTemperature())
                    .maxTokens(llmConfig.getMaxTokens())
                    .build();
            default -> throw new IllegalArgumentException("Unsupported LLM model type: " + llmConfig.getModelType());
        };
    }

    @Bean
    public StreamingChatModel streamingChatModel() {
        RagConfig.LlmConfig llmConfig = ragConfig.getLlm();
        
        if (llmConfig == null || llmConfig.getModelType() == null) {
            throw new IllegalStateException("rag.llm.model-type must be configured");
        }
        
        return switch (llmConfig.getModelType().toLowerCase()) {
            case "ollama" -> OllamaStreamingChatModel.builder()
                    .baseUrl(llmConfig.getBaseUrl())
                    .modelName(llmConfig.getModelName())
                    .timeout(java.time.Duration.ofMillis(llmConfig.getTimeoutMs()))
                    .temperature(llmConfig.getTemperature())
                    .build();
            case "openai", "siliconflow" -> OpenAiStreamingChatModel.builder()
                    .baseUrl(llmConfig.getBaseUrl())
                    .apiKey(llmConfig.getApiKey())
                    .modelName(llmConfig.getModelName())
                    .timeout(java.time.Duration.ofMillis(llmConfig.getTimeoutMs()))
                    .temperature(llmConfig.getTemperature())
                    .maxTokens(llmConfig.getMaxTokens())
                    .build();
            default -> throw new IllegalArgumentException("Unsupported LLM model type: " + llmConfig.getModelType());
        };
    }
}
