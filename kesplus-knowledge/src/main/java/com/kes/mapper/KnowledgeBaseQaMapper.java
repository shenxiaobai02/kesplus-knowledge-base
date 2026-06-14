package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.KnowledgeBaseQa;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库问答Mapper
 */
@Mapper
public interface KnowledgeBaseQaMapper extends BaseMapper<KnowledgeBaseQa> {

    @Select("SELECT id, uuid, kb_uuid, question, answer, prompt_tokens, answer_tokens, " +
            "is_deleted, created_time, updated_time " +
            "FROM kes_knowledge_base_qa WHERE uuid = #{uuid}")
    KnowledgeBaseQa selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT id, uuid, kb_uuid, question, answer, prompt_tokens, answer_tokens, " +
            "is_deleted, created_time, updated_time " +
            "FROM kes_knowledge_base_qa WHERE kb_uuid = #{kbUuid} ORDER BY created_time DESC")
    List<KnowledgeBaseQa> selectByKbUuid(@Param("kbUuid") String kbUuid);

    @Select("SELECT COUNT(*) FROM kes_knowledge_base_qa WHERE kb_uuid = #{kbUuid}")
    int countByKbUuid(@Param("kbUuid") String kbUuid);
}
