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
        model.setUuid(UuidUtil.generate());
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
        when(embeddingModelMapper.selectById(any(Long.class))).thenReturn(model);

        EmbeddingModel result = embeddingModelService.create(model);

        assertNotNull(result);
        assertEquals("BAAI/bge-m3", result.getModelName());
        assertEquals(1024, result.getEmbeddingDimension());
        verify(embeddingModelMapper, times(1)).insert(any(EmbeddingModel.class));
    }

    @Test
    void testFindById() {
        when(embeddingModelMapper.selectById(eq(1L))).thenReturn(model);

        EmbeddingModel result = embeddingModelService.findById(1L);

        assertNotNull(result);
        assertEquals("BAAI/bge-m3", result.getModelName());
    }

    @Test
    void testFindByUuid() {
        when(embeddingModelMapper.selectOne(any())).thenReturn(model);

        EmbeddingModel result = embeddingModelService.findByUuid(model.getUuid());

        assertNotNull(result);
        assertEquals(model.getUuid(), result.getUuid());
    }

    @Test
    void testFindByName() {
        when(embeddingModelMapper.selectOne(any())).thenReturn(model);

        EmbeddingModel result = embeddingModelService.findByName("BAAI/bge-m3");

        assertNotNull(result);
        assertEquals("BAAI/bge-m3", result.getModelName());
    }

    @Test
    void testFindAll() {
        when(embeddingModelMapper.selectList(any())).thenReturn(Arrays.asList(model));

        List<EmbeddingModel> result = embeddingModelService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindActiveModels() {
        when(embeddingModelMapper.selectList(any())).thenReturn(Arrays.asList(model));

        List<EmbeddingModel> result = embeddingModelService.findActiveModels();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateModel() {
        when(embeddingModelMapper.selectOne(any())).thenReturn(model);
        when(embeddingModelMapper.updateById(any(EmbeddingModel.class))).thenReturn(1);

        model.setModelName("Updated Model");
        EmbeddingModel result = embeddingModelService.update(model.getUuid(), model);

        assertNotNull(result);
        assertEquals("Updated Model", result.getModelName());
        verify(embeddingModelMapper, times(1)).updateById(any(EmbeddingModel.class));
    }

    @Test
    void testDeleteModel() {
        when(embeddingModelMapper.selectOne(any())).thenReturn(model);
        when(embeddingModelMapper.updateById(any(EmbeddingModel.class))).thenReturn(1);

        embeddingModelService.delete(model.getUuid());

        verify(embeddingModelMapper, times(1)).updateById(any(EmbeddingModel.class));
    }

    @Test
    void testDeactivateModel() {
        when(embeddingModelMapper.selectOne(any())).thenReturn(model);
        when(embeddingModelMapper.updateById(any(EmbeddingModel.class))).thenReturn(1);

        embeddingModelService.deactivate(model.getUuid());

        verify(embeddingModelMapper, times(1)).updateById(any(EmbeddingModel.class));
    }
}
