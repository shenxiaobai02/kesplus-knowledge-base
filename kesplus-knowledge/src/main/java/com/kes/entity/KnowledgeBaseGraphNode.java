package com.kes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图谱节点实体
 * 用于存储知识库图谱中的节点信息
 */
@Data
@TableName("kes_knowledge_base_graph_node")
public class KnowledgeBaseGraphNode {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("uuid")
    private String uuid;

    @TableField("kb_uuid")
    private String kbUuid;

    /**
     * 节点类型：DOCUMENT, SEGMENT, ENTITY, KEYWORD
     */
    @TableField("node_type")
    private String nodeType;

    /**
     * 外部引用ID（如文档UUID、段落UUID等）
     */
    @TableField("node_id")
    private String nodeId;

    /**
     * 节点内容
     */
    @TableField("content")
    private String content;

    /**
     * 元数据JSON
     */
    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_time")
    private LocalDateTime createdTime;
}