package com.kes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kes_knowledge_base_qa")
public class KnowledgeBaseQa {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("uuid")
    private String uuid;

    @TableField("kb_uuid")
    private String kbUuid;

    @TableField("question")
    private String question;

    @TableField("answer")
    private String answer;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("answer_tokens")
    private Integer answerTokens;

    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}