package com.kes.service;

import com.kes.entity.EmbeddingModel;
import com.kes.util.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmbeddingModelServiceTest {

    @Autowired
    private EmbeddingModelService embeddingModelService;

    private EmbeddingModel model;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = System.currentTimeMillis() + "-" + Thread.currentThread().getId();
        model = new EmbeddingModel();
        model.setModelName("BAAI/bge-m3-test-" + uniqueSuffix);
        model.setEmbeddingDimension(1024);
        model.setModelType("huggingface");
        model.setBaseUrl("https://api.siliconflow.cn/v1");
        model.setIsActive(true);
    }

    @Test
    void testGetByUuid() {
        EmbeddingModel created = embeddingModelService.create(model);

        EmbeddingModel result = embeddingModelService.getByUuid(created.getUuid());

        assertNotNull(result);
        assertEquals(created.getUuid(), result.getUuid());
        assertEquals(model.getModelName(), result.getModelName());
    }

    @Test
    void testGetByModelName() {
        embeddingModelService.create(model);

        EmbeddingModel result = embeddingModelService.getByModelName(model.getModelName());

        assertNotNull(result);
        assertEquals(model.getModelName(), result.getModelName());
    }

    @Test
    void testGetByDimension() {
        embeddingModelService.create(model);

        String uniqueSuffix = System.currentTimeMillis() + "-" + Thread.currentThread().getId();
        EmbeddingModel model2 = new EmbeddingModel();
        model2.setModelName("text-embedding-3-small-test-" + uniqueSuffix);
        model2.setEmbeddingDimension(1536);
        model2.setModelType("openai");
        model2.setIsActive(true);
        embeddingModelService.create(model2);

        List<EmbeddingModel> result = embeddingModelService.getByDimension(1024);

        assertNotNull(result);
        assertTrue(result.size() >= 1);
        assertTrue(result.stream().anyMatch(m -> model.getModelName().equals(m.getModelName())));
    }

    @Test
    void testGetActiveModels() {
        embeddingModelService.create(model);

        String uniqueSuffix = System.currentTimeMillis() + "-" + Thread.currentThread().getId();
        EmbeddingModel model2 = new EmbeddingModel();
        model2.setModelName("inactive-model-test-" + uniqueSuffix);
        model2.setEmbeddingDimension(768);
        model2.setModelType("ollama");
        model2.setIsActive(false);
        embeddingModelService.create(model2);

        List<EmbeddingModel> result = embeddingModelService.getActiveModels();

        assertNotNull(result);
        assertTrue(result.size() >= 1);
        assertTrue(result.stream().allMatch(EmbeddingModel::getIsActive));
        assertTrue(result.stream().anyMatch(m -> model.getModelName().equals(m.getModelName())));
    }

    @Test
    void testUpdateModel() {
        EmbeddingModel created = embeddingModelService.create(model);

        created.setModelName("Updated Model");
        created.setBaseUrl("https://api.new-url.com");
        EmbeddingModel result = embeddingModelService.update(created);

        assertNotNull(result);
        assertEquals("Updated Model", result.getModelName());
        assertEquals("https://api.new-url.com", result.getBaseUrl());
    }

    @Test
    void testUpdateModelNotFound() {
        model.setUuid(UuidUtil.create());

        assertThrows(RuntimeException.class, () -> embeddingModelService.update(model));
    }

    @Test
    void testDeleteModel() {
        EmbeddingModel created = embeddingModelService.create(model);
        Long id = created.getId();

        embeddingModelService.delete(created.getUuid());

        EmbeddingModel result = embeddingModelService.getById(id);
        assertNull(result);
    }

    @Test
    void testDeleteModelNotFound() {
        assertDoesNotThrow(() -> embeddingModelService.delete(UuidUtil.create()));
    }
}