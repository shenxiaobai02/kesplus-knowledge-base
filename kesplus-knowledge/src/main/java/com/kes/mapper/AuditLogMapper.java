package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    @Select("<script>" +
            "SELECT uuid, user_id, tenant_uuid, operation_type, resource_type, resource_id, " +
            "operation_detail, ip_address, user_agent, created_time " +
            "FROM kes_audit_log WHERE 1=1" +
            "<if test='userId != null'> AND user_id = #{userId}</if>" +
            "<if test='tenantUuid != null and tenantUuid != \"\"'> AND tenant_uuid = #{tenantUuid}</if>" +
            "<if test='operationType != null and operationType != \"\"'> AND operation_type = #{operationType}</if>" +
            "<if test='resourceType != null and resourceType != \"\"'> AND resource_type = #{resourceType}</if>" +
            "<if test='startTime != null'> AND created_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null'> AND created_time &lt;= #{endTime}</if>" +
            " ORDER BY created_time DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<AuditLog> selectByConditions(
            @Param("userId") Long userId,
            @Param("tenantUuid") String tenantUuid,
            @Param("operationType") String operationType,
            @Param("resourceType") String resourceType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Select("<script>" +
            "SELECT COUNT(*) FROM kes_audit_log WHERE 1=1" +
            "<if test='userId != null'> AND user_id = #{userId}</if>" +
            "<if test='tenantUuid != null and tenantUuid != \"\"'> AND tenant_uuid = #{tenantUuid}</if>" +
            "<if test='operationType != null and operationType != \"\"'> AND operation_type = #{operationType}</if>" +
            "<if test='resourceType != null and resourceType != \"\"'> AND resource_type = #{resourceType}</if>" +
            "<if test='startTime != null'> AND created_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null'> AND created_time &lt;= #{endTime}</if>" +
            "</script>")
    int countByConditions(
            @Param("userId") Long userId,
            @Param("tenantUuid") String tenantUuid,
            @Param("operationType") String operationType,
            @Param("resourceType") String resourceType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
