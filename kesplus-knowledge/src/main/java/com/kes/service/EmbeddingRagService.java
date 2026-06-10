package com.kes.service;

import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseEmbedding;
import com.kes.mapper.EmbeddingMapper;
import com.kes.util.UuidUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingRagService {

    @Autowired
    private EmbeddingMapper embeddingMapper;

    @Autowired
    private DynamicTableService dynamicTableService;

    @Autowired
    private EmbeddingModelService embeddingModelService;

    @Autowired
    private RagConfig ragConfig;

    @Transactional
    public void ingest(KnowledgeBase kb, Document document, EmbeddingModel embeddingModel) {
        String tableName = dynamicTableService.getTableName(kb.getEmbeddingDimension());

        String text = document.text();
        Embedding embedding = embeddingModel.embed(text).content();

        KnowledgeBaseEmbedding kbEmbedding = new KnowledgeBaseEmbedding();
        kbEmbedding.setUuid(UuidUtil.create());
        kbEmbedding.setKbUuid(kb.getUuid());
        kbEmbedding.setEmbedding(new com.pgvector.PGvector(embedding.vector()));
        kbEmbedding.setText(text);

        Metadata metadata = document.metadata();
        if (metadata != null && !metadata.isEmpty()) {
            kbEmbedding.setMetadataJson(com.kes.util.JsonUtil.toJson(metadata.asMap()));
        }

        embeddingMapper.insert(tableName, kbEmbedding);
        log.debug("Ingested embedding for kb: {}, dimension: {}", kb.getUuid(), kb.getEmbeddingDimension());
    }

    @Transactional
    public void batchIngest(KnowledgeBase kb, List<Document> documents, EmbeddingModel embeddingModel) {
        String tableName = dynamicTableService.getTableName(kb.getEmbeddingDimension());

        List<KnowledgeBaseEmbedding> embeddings = new ArrayList<>();
        for (Document document : documents) {
            String text = document.text();
            Embedding embedding = embeddingModel.embed(text).content();

            KnowledgeBaseEmbedding kbEmbedding = new KnowledgeBaseEmbedding();
            kbEmbedding.setUuid(UuidUtil.create());
            kbEmbedding.setKbUuid(kb.getUuid());
            kbEmbedding.setEmbedding(new com.pgvector.PGvector(embedding.vector()));
            kbEmbedding.setText(text);

            Metadata metadata = document.metadata();
            if (metadata != null && !metadata.isEmpty()) {
                kbEmbedding.setMetadataJson(com.kes.util.JsonUtil.toJson(metadata.asMap()));
            }

            embeddings.add(kbEmbedding);
        }

        if (!embeddings.isEmpty()) {
            embeddingMapper.batchInsert(tableName, embeddings);
            log.info("Batch ingested {} embeddings for kb: {}", embeddings.size(), kb.getUuid());
        }
    }

    public List<KnowledgeBaseEmbedding> retrieve(KnowledgeBase kb, String query, EmbeddingModel embeddingModel) {
        String tableName = dynamicTableService.getTableName(kb.getEmbeddingDimension());

        Embedding queryEmbedding = embeddingModel.embed(query).content();
        float[] queryVector = queryEmbedding.vector();

        int maxResults = kb.getRetrieveMaxResults() != null ? kb.getRetrieveMaxResults() : ragConfig.getMaxRetrieveResults();
        double minScore = kb.getRetrieveMinScore() != null ? kb.getRetrieveMinScore() : ragConfig.getMinScore();

        return embeddingMapper.retrieve(tableName, kb.getUuid(), queryVector, minScore, maxResults);
    }

    @Transactional
    public void deleteByKbUuid(KnowledgeBase kb) {
        String tableName = dynamicTableService.getTableName(kb.getEmbeddingDimension());
        embeddingMapper.deleteByKbUuid(tableName, kb.getUuid());
        log.info("Deleted all embeddings for kb: {}", kb.getUuid());
    }

    public int countByKbUuid(KnowledgeBase kb) {
        String tableName = dynamicTableService.getTableName(kb.getEmbeddingDimension());
        return embeddingMapper.countByKbUuid(tableName, kb.getUuid());
    }

    public void ensureTableExists(int dimension) {
        embeddingModelService.ensureTableExists(dimension);
    }
}