package com.kes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.pgvector.PGvector;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseEmbedding {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("uuid")
    private String uuid;

    @TableField("kb_uuid")
    private String kbUuid;

    @TableField("kb_item_uuid")
    private String kbItemUuid;

    @TableField("embedding")
    private PGvector embedding;

    @TableField("text")
    private String text;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_time")
    private LocalDateTime createdTime;
}