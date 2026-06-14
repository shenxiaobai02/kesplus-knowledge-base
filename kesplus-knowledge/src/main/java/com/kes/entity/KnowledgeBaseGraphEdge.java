package com.kes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图谱关系实体
 * 用于存储知识库图谱中节点之间的关系
 */
@Data
@TableName("kes_knowledge_base_graph_edge")
public class KnowledgeBaseGraphEdge {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("uuid")
    private String uuid;

    @TableField("kb_uuid")
    private String kbUuid;

    /**
     * 源节点UUID
     */
    @TableField("source_node_uuid")
    private String sourceNodeUuid;

    /**
     * 目标节点UUID
     */
    @TableField("target_node_uuid")
    private String targetNodeUuid;

    /**
     * 关系类型：CONTAINS, REFERENCES, SIMILAR, KEYWORD
     */
    @TableField("relation_type")
    private String relationType;

    /**
     * 关系权重
     */
    @TableField("weight")
    private Double weight;

    /**
     * 元数据JSON
     */
    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_time")
    private LocalDateTime createdTime;
}