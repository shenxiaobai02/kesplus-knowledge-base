package com.kes.mapper;

import com.kes.entity.KnowledgeBaseEmbedding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmbeddingMapper {

    void insert(@Param("tableName") String tableName, @Param("embedding") KnowledgeBaseEmbedding embedding);

    void batchInsert(@Param("tableName") String tableName, @Param("list") List<KnowledgeBaseEmbedding> list);

    List<KnowledgeBaseEmbedding> selectByKbUuid(@Param("tableName") String tableName, @Param("kbUuid") String kbUuid);

    int deleteByKbUuid(@Param("tableName") String tableName, @Param("kbUuid") String kbUuid);

    int countByKbUuid(@Param("tableName") String tableName, @Param("kbUuid") String kbUuid);

    void deleteByKbItemUuid(@Param("tableName") String tableName, @Param("kbItemUuid") String kbItemUuid);

    List<KnowledgeBaseEmbedding> retrieve(@Param("tableName") String tableName, 
                                          @Param("kbUuid") String kbUuid,
                                          @Param("queryEmbedding") float[] queryEmbedding,
                                          @Param("minScore") double minScore,
                                          @Param("maxResults") int maxResults);
}