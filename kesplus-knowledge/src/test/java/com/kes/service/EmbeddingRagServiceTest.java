package com.kes.service;

import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseEmbedding;
import com.kes.entity.EmbeddingModel;
import com.kes.mapper.EmbeddingMapper;
import com.kes.util.UuidUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingRagServiceTest {

    @Mock
    private EmbeddingMapper embeddingMapper;

    @Mock
    private DynamicTableService dynamicTableService;

    @Mock
    private EmbeddingModelService embeddingModelService;

    @InjectMocks
    private EmbeddingRagService embeddingRagService;

    private KnowledgeBase kb;
    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setUuid(UuidUtil.create());
        kb.setTitle("Test KB");
        kb.setEmbeddingDimension(1024);
        kb.setRetrieveMaxResults(5);
        kb.setRetrieveMinScore(0.6);

        embeddingModel = new EmbeddingModel();
        embeddingModel.setId(1L);
        embeddingModel.setUuid(UuidUtil.create());
        embeddingModel.setModelName("BAAI/bge-m3");
        embeddingModel.setEmbeddingDimension(1024);
        embeddingModel.setModelType("huggingface");
    }

    @Test
    void testIngestDocument() {
        when(dynamicTableService.getTableName(eq(1024))).thenReturn("kes_embedding_1024");

        dev.langchain4j.model.embedding.EmbeddingModel mockLangChainModel = 
            mock(dev.langchain4j.model.embedding.EmbeddingModel.class);
        float[] mockVector = new float[1024];
        Arrays.fill(mockVector, 0.1f);
        Embedding mockEmbedding = Embedding.from(mockVector);
        Response<Embedding> mockResponse = Response.from(mockEmbedding);
        when(mockLangChainModel.embed(anyString())).thenReturn(mockResponse);

        Document document = Document.from("Test document content for embedding");

        assertDoesNotThrow(() -> embeddingRagService.ingest(kb, document, mockLangChainModel));

        verify(embeddingMapper, times(1)).insert(eq("kes_embedding_1024"), any());
    }

    @Test
    void testBatchIngestDocuments() {
        when(dynamicTableService.getTableName(eq(1024))).thenReturn("kes_embedding_1024");

        dev.langchain4j.model.embedding.EmbeddingModel mockLangChainModel = 
            mock(dev.langchain4j.model.embedding.EmbeddingModel.class);
        float[] mockVector = new float[1024];
        Arrays.fill(mockVector, 0.1f);
        Embedding mockEmbedding = Embedding.from(mockVector);
        Response<Embedding> mockResponse = Response.from(mockEmbedding);
        when(mockLangChainModel.embed(anyString())).thenReturn(mockResponse);

        List<Document> documents = Arrays.asList(
            Document.from("Document 1 content"),
            Document.from("Document 2 content"),
            Document.from("Document 3 content")
        );

        assertDoesNotThrow(() -> embeddingRagService.batchIngest(kb, documents, mockLangChainModel));

        verify(embeddingMapper, times(1)).batchInsert(eq("kes_embedding_1024"), any());
    }

    @Test
    void testRetrieveDocuments() {
        when(dynamicTableService.getTableName(eq(1024))).thenReturn("kes_embedding_1024");

        dev.langchain4j.model.embedding.EmbeddingModel mockLangChainModel = 
            mock(dev.langchain4j.model.embedding.EmbeddingModel.class);
        float[] mockVector = new float[1024];
        Arrays.fill(mockVector, 0.1f);
        Embedding mockEmbedding = Embedding.from(mockVector);
        Response<Embedding> mockResponse = Response.from(mockEmbedding);
        when(mockLangChainModel.embed(anyString())).thenReturn(mockResponse);

        List<KnowledgeBaseEmbedding> mockResults = Arrays.asList(new KnowledgeBaseEmbedding());
        when(embeddingMapper.retrieve(eq("kes_embedding_1024"), eq(kb.getUuid()), any(float[].class), eq(0.6), eq(5)))
            .thenReturn(mockResults);

        List<KnowledgeBaseEmbedding> results = embeddingRagService.retrieve(kb, "test query", mockLangChainModel);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        verify(embeddingMapper, times(1)).retrieve(any(), any(), any(), any(), any());
    }

    @Test
    void testDeleteByKbUuid() {
        when(dynamicTableService.getTableName(eq(1024))).thenReturn("kes_embedding_1024");
        when(embeddingMapper.deleteByKbUuid(eq("kes_embedding_1024"), eq(kb.getUuid()))).thenReturn(1);

        assertDoesNotThrow(() -> embeddingRagService.deleteByKbUuid(kb));

        verify(embeddingMapper, times(1)).deleteByKbUuid(eq("kes_embedding_1024"), eq(kb.getUuid()));
    }

    @Test
    void testCountByKbUuid() {
        when(dynamicTableService.getTableName(eq(1024))).thenReturn("kes_embedding_1024");
        when(embeddingMapper.countByKbUuid(eq("kes_embedding_1024"), eq(kb.getUuid()))).thenReturn(10);

        int count = embeddingRagService.countByKbUuid(kb);

        assertEquals(10, count);
    }

    @Test
    void testEnsureTableExists() {
        assertDoesNotThrow(() -> embeddingRagService.ensureTableExists(1024));
        verify(embeddingModelService, times(1)).ensureTableExists(eq(1024));
    }
}
