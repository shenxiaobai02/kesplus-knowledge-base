package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.KnowledgeBaseQa;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeBaseQaMapper extends BaseMapper<KnowledgeBaseQa> {

    @Select("SELECT * FROM kes_knowledge_base_qa WHERE uuid = #{uuid}")
    KnowledgeBaseQa selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM kes_knowledge_base_qa WHERE kb_uuid = #{kbUuid} ORDER BY created_time DESC")
    List<KnowledgeBaseQa> selectByKbUuid(@Param("kbUuid") String kbUuid);

    @Select("SELECT COUNT(*) FROM kes_knowledge_base_qa WHERE kb_uuid = #{kbUuid}")
    int countByKbUuid(@Param("kbUuid") String kbUuid);
}