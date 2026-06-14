package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 知识库Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    @Select("SELECT id, uuid, title, remark, is_public, is_strict, star_count, item_count, embedding_count, " +
            "owner_uuid, owner_id, owner_name, ingest_max_overlap, ingest_model_name, ingest_model_id, " +
            "ingest_token_estimator, retrieve_max_results, retrieve_min_score, query_llm_temperature, " +
            "query_system_message, embedding_model_uuid, embedding_dimension, tenant_uuid, business_line_uuid, " +
            "visibility, allowed_role_codes, config_json, is_deleted, created_time, updated_time " +
            "FROM kes_knowledge_base WHERE uuid = #{uuid}")
    KnowledgeBase selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT id, uuid, title, remark, is_public, is_strict, star_count, item_count, embedding_count, " +
            "owner_uuid, owner_id, owner_name, ingest_max_overlap, ingest_model_name, ingest_model_id, " +
            "ingest_token_estimator, retrieve_max_results, retrieve_min_score, query_llm_temperature, " +
            "query_system_message, embedding_model_uuid, embedding_dimension, tenant_uuid, business_line_uuid, " +
            "visibility, allowed_role_codes, config_json, is_deleted, created_time, updated_time " +
            "FROM kes_knowledge_base WHERE tenant_uuid = #{tenantUuid} AND is_deleted = FALSE")
    List<KnowledgeBase> selectByTenantUuid(@Param("tenantUuid") String tenantUuid);

    @Select("SELECT id, uuid, title, remark, is_public, is_strict, star_count, item_count, embedding_count, " +
            "owner_uuid, owner_id, owner_name, ingest_max_overlap, ingest_model_name, ingest_model_id, " +
            "ingest_token_estimator, retrieve_max_results, retrieve_min_score, query_llm_temperature, " +
            "query_system_message, embedding_model_uuid, embedding_dimension, tenant_uuid, business_line_uuid, " +
            "visibility, allowed_role_codes, config_json, is_deleted, created_time, updated_time " +
            "FROM kes_knowledge_base WHERE is_deleted = FALSE ORDER BY created_time DESC")
    List<KnowledgeBase> selectAllActive();

    @Update("UPDATE kes_knowledge_base SET embedding_count = #{embeddingCount}, updated_time = NOW() WHERE uuid = #{uuid}")
    void updateStatByUuid(@Param("uuid") String uuid, @Param("embeddingCount") Integer embeddingCount);
}
