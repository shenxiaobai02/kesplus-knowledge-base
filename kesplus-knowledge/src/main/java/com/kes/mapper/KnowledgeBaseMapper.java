package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    @Select("SELECT * FROM kes_knowledge_base WHERE uuid = #{uuid}")
    KnowledgeBase selectByUuid(@Param("uuid") String uuid);

    @Update("UPDATE kes_knowledge_base SET embedding_count = #{embeddingCount}, updated_time = NOW() WHERE uuid = #{uuid}")
    void updateStatByUuid(@Param("uuid") String uuid, @Param("embeddingCount") Integer embeddingCount);
}