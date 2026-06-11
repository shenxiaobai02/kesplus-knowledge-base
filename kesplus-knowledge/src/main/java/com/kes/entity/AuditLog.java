package com.kes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kes_audit_log")
public class AuditLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("uuid")
    private String uuid;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("tenant_uuid")
    private String tenantUuid;

    @TableField("operation_type")
    private String operationType;

    @TableField("resource_type")
    private String resourceType;

    @TableField("resource_uuid")
    private String resourceUuid;

    @TableField("resource_name")
    private String resourceName;

    @TableField("operation_detail")
    private String operationDetail;

    @TableField("success")
    private Boolean success;

    @TableField("error_message")
    private String errorMessage;

    @TableField("client_ip")
    private String clientIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("created_time")
    private LocalDateTime createdTime;
}