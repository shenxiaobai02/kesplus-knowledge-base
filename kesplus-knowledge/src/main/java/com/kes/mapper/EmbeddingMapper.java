package com.kes.mapper;

import com.kes.entity.KnowledgeBaseEmbedding;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EmbeddingMapper {

    @Insert("INSERT INTO ${tableName} (uuid, kb_uuid, kb_item_uuid, embedding, text, metadata_json) " +
            "VALUES (#{embedding.uuid}, #{embedding.kbUuid}, #{embedding.kbItemUuid}, #{embedding.embedding}, #{embedding.text}, #{embedding.metadataJson})")
    void insert(@Param("tableName") String tableName, @Param("embedding") KnowledgeBaseEmbedding embedding);

    @Insert("<script>" +
            "INSERT INTO ${tableName} (uuid, kb_uuid, kb_item_uuid, embedding, text, metadata_json) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.uuid}, #{item.kbUuid}, #{item.kbItemUuid}, #{item.embedding}, #{item.text}, #{item.metadataJson})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("tableName") String tableName, @Param("list") List<KnowledgeBaseEmbedding> list);

    @Select("SELECT * FROM ${tableName} WHERE kb_uuid = #{kbUuid}")
    List<KnowledgeBaseEmbedding> selectByKbUuid(@Param("tableName") String tableName, @Param("kbUuid") String kbUuid);

    @Delete("DELETE FROM ${tableName} WHERE kb_uuid = #{kbUuid}")
    int deleteByKbUuid(@Param("tableName") String tableName, @Param("kbUuid") String kbUuid);

    @Select("SELECT COUNT(*) FROM ${tableName} WHERE kb_uuid = #{kbUuid}")
    int countByKbUuid(@Param("tableName") String tableName, @Param("kbUuid") String kbUuid);

    @Delete("DELETE FROM ${tableName} WHERE kb_item_uuid = #{kbItemUuid}")
    void deleteByKbItemUuid(@Param("tableName") String tableName, @Param("kbItemUuid") String kbItemUuid);

    @Select("SELECT *, 1 - (embedding <=> #{queryEmbedding}::vector) AS score " +
            "FROM ${tableName} " +
            "WHERE kb_uuid = #{kbUuid} " +
            "AND 1 - (embedding <=> #{queryEmbedding}::vector) >= #{minScore} " +
            "ORDER BY score DESC " +
            "LIMIT #{maxResults}")
    List<KnowledgeBaseEmbedding> retrieve(@Param("tableName") String tableName, 
                                          @Param("kbUuid") String kbUuid,
                                          @Param("queryEmbedding") com.pgvector.PGvector queryEmbedding,
                                          @Param("minScore") double minScore,
                                          @Param("maxResults") int maxResults);
}