package com.kes.service;

import com.kes.entity.EmbeddingModel;
import com.kes.mapper.EmbeddingModelMapper;
import com.kes.util.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingModelServiceTest {

    @Mock
    private EmbeddingModelMapper embeddingModelMapper;

    @InjectMocks
    private EmbeddingModelService embeddingModelService;

    private EmbeddingModel model;

    @BeforeEach
    void setUp() {
        model = new EmbeddingModel();
        model.setId(1L);
        model.setUuid(UuidUtil.create());
        model.setModelName("BAAI/bge-m3");
        model.setEmbeddingDimension(1024);
        model.setModelType("huggingface");
        model.setBaseUrl("https://api.siliconflow.cn/v1");
        model.setIsActive(true);
        model.setCreatedTime(LocalDateTime.now());
        model.setUpdatedTime(LocalDateTime.now());
    }

    @Test
    void testCreateModel() {
        when(embeddingModelMapper.insert(any(EmbeddingModel.class))).thenReturn(1);

        EmbeddingModel result = embeddingModelService.create(model);

        assertNotNull(result);
        assertEquals("BAAI/bge-m3", result.getModelName());
        assertEquals(1024, result.getEmbeddingDimension());
        verify(embeddingModelMapper, times(1)).insert(any(EmbeddingModel.class));
    }

    @Test
    void testGetByUuid() {
        when(embeddingModelMapper.selectByUuid(eq(model.getUuid()))).thenReturn(model);

        EmbeddingModel result = embeddingModelService.getByUuid(model.getUuid());

        assertNotNull(result);
        assertEquals(model.getUuid(), result.getUuid());
    }

    @Test
    void testGetByModelName() {
        when(embeddingModelMapper.selectByModelName(eq("BAAI/bge-m3"))).thenReturn(model);

        EmbeddingModel result = embeddingModelService.getByModelName("BAAI/bge-m3");

        assertNotNull(result);
        assertEquals("BAAI/bge-m3", result.getModelName());
    }

    @Test
    void testGetByDimension() {
        when(embeddingModelMapper.selectByDimension(eq(1024))).thenReturn(Arrays.asList(model));

        List<EmbeddingModel> result = embeddingModelService.getByDimension(1024);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetActiveModels() {
        when(embeddingModelMapper.selectActiveModels()).thenReturn(Arrays.asList(model));

        List<EmbeddingModel> result = embeddingModelService.getActiveModels();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateModel() {
        when(embeddingModelMapper.selectByUuid(eq(model.getUuid()))).thenReturn(model);
        when(embeddingModelMapper.updateById(any(EmbeddingModel.class))).thenReturn(1);

        model.setModelName("Updated Model");
        EmbeddingModel result = embeddingModelService.update(model);

        assertNotNull(result);
        assertEquals("Updated Model", result.getModelName());
        verify(embeddingModelMapper, times(1)).updateById(any(EmbeddingModel.class));
    }

    @Test
    void testDeleteModel() {
        when(embeddingModelMapper.selectByUuid(eq(model.getUuid()))).thenReturn(model);
        when(embeddingModelMapper.deleteById(eq(1L))).thenReturn(1);

        embeddingModelService.delete(model.getUuid());

        verify(embeddingModelMapper, times(1)).deleteById(eq(1L));
    }
}
