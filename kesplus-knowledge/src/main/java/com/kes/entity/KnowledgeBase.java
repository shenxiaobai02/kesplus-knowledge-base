package com.kes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kes_knowledge_base")
public class KnowledgeBase {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("uuid")
    private String uuid;

    @TableField("title")
    private String title;

    @TableField("remark")
    private String remark;

    @TableField("is_public")
    private Boolean isPublic;

    @TableField("is_strict")
    private Boolean isStrict;

    @TableField("star_count")
    private Integer starCount;

    @TableField("item_count")
    private Integer itemCount;

    @TableField("embedding_count")
    private Integer embeddingCount;

    @TableField("owner_uuid")
    private String ownerUuid;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("owner_name")
    private String ownerName;

    @TableField("ingest_max_overlap")
    private Integer ingestMaxOverlap;

    @TableField("ingest_model_name")
    private String ingestModelName;

    @TableField("ingest_model_id")
    private Long ingestModelId;

    @TableField("ingest_token_estimator")
    private String ingestTokenEstimator;

    @TableField("retrieve_max_results")
    private Integer retrieveMaxResults;

    @TableField("retrieve_min_score")
    private Double retrieveMinScore;

    @TableField("query_llm_temperature")
    private Double queryLlmTemperature;

    @TableField("query_system_message")
    private String querySystemMessage;

    @TableField("embedding_model_uuid")
    private String embeddingModelUuid;

    @TableField("embedding_dimension")
    private Integer embeddingDimension;

    @TableField("tenant_uuid")
    private String tenantUuid;

    @TableField("business_line_uuid")
    private String businessLineUuid;

    @TableField("visibility")
    private String visibility;

    @TableField("allowed_role_codes")
    private String allowedRoleCodes;

    @TableField("config_json")
    private String configJson;

    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}