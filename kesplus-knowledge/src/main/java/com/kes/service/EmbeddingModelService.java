package com.kes.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kes.entity.EmbeddingModel;
import com.kes.mapper.EmbeddingModelMapper;
import com.kes.util.UuidUtil;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class EmbeddingModelService extends ServiceImpl<EmbeddingModelMapper, EmbeddingModel> {

    @Autowired
    private DynamicTableService dynamicTableService;

    public EmbeddingModel getByUuid(String uuid) {
        return baseMapper.selectByUuid(uuid);
    }

    public EmbeddingModel getByModelName(String modelName) {
        return baseMapper.selectByModelName(modelName);
    }

    public List<EmbeddingModel> getByDimension(Integer dimension) {
        return baseMapper.selectByDimension(dimension);
    }

    public List<EmbeddingModel> getActiveModels() {
        return baseMapper.selectActiveModels();
    }

    @Transactional
    public EmbeddingModel create(EmbeddingModel model) {
        model.setUuid(UuidUtil.create());
        model.setIsActive(true);
        model.setCreatedTime(LocalDateTime.now());
        model.setUpdatedTime(LocalDateTime.now());

        baseMapper.insert(model);

        try {
            dynamicTableService.createTableIfNotExists(model.getEmbeddingDimension());
        } catch (Exception e) {
            log.error("Failed to create embedding table for dimension {}", model.getEmbeddingDimension(), e);
            throw new RuntimeException("Failed to create embedding table", e);
        }

        log.info("Created embedding model: {}", model.getModelName());
        return model;
    }

    @Transactional
    public EmbeddingModel update(EmbeddingModel model) {
        EmbeddingModel existing = getByUuid(model.getUuid());
        if (existing == null) {
            throw new RuntimeException("Embedding model not found");
        }

        int oldDimension = existing.getEmbeddingDimension();
        int newDimension = model.getEmbeddingDimension();

        model.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(model);

        if (oldDimension != newDimension) {
            try {
                dynamicTableService.createTableIfNotExists(newDimension);
            } catch (Exception e) {
                log.error("Failed to create embedding table for dimension {}", newDimension, e);
            }
        }

        log.info("Updated embedding model: {}", model.getModelName());
        return model;
    }

    @Transactional
    public void delete(String uuid) {
        EmbeddingModel model = getByUuid(uuid);
        if (model == null) {
            return;
        }

        baseMapper.deleteById(model.getId());
        log.info("Deleted embedding model: {}", model.getModelName());
    }

    public void ensureTableExists(int dimension) {
        try {
            dynamicTableService.createTableIfNotExists(dimension);
        } catch (Exception e) {
            log.error("Failed to ensure table exists for dimension {}", dimension, e);
            throw new RuntimeException("Failed to ensure table exists", e);
        }
    }

    public dev.langchain4j.model.embedding.EmbeddingModel createLangChainEmbeddingModel(EmbeddingModel model) {
        if (model == null) {
            return null;
        }
        
        String modelType = model.getModelType() != null ? model.getModelType().toLowerCase() : "ollama";
        
        return switch (modelType) {
            case "ollama" -> OllamaEmbeddingModel.builder()
                    .baseUrl(model.getBaseUrl() != null ? model.getBaseUrl() : "http://localhost:11434")
                    .modelName(model.getModelName())
                    .build();
            case "huggingface", "siliconflow", "openai" -> OpenAiEmbeddingModel.builder()
                    .baseUrl(model.getBaseUrl() != null ? model.getBaseUrl() : "https://api.siliconflow.cn/v1")
                    .apiKey(model.getApiKey())
                    .modelName(model.getModelName())
                    .build();
            default -> OllamaEmbeddingModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName(model.getModelName() != null ? model.getModelName() : "all-minilm")
                    .build();
        };
    }
}