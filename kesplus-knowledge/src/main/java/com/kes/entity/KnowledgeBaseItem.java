package com.kes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kes_knowledge_base_item")
public class KnowledgeBaseItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("uuid")
    private String uuid;

    @TableField("kb_id")
    private Long kbId;

    @TableField("kb_uuid")
    private String kbUuid;

    @TableField("source_file_id")
    private Long sourceFileId;

    @TableField("title")
    private String title;

    @TableField("brief")
    private String brief;

    @TableField("remark")
    private String remark;

    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}