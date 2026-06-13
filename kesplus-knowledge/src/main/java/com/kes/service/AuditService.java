package com.kes.service;

import com.kes.entity.AuditLog;
import com.kes.mapper.AuditLogMapper;
import com.kes.util.ThreadContext;
import com.kes.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志服务
 * <p>
 * 提供审计日志记录和查询功能，支持记录用户操作、资源变更等信息。
 * 所有日志记录自动从ThreadContext获取用户上下文信息。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Service
public class AuditService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    /**
     * 记录简单审计日志（自动获取用户上下文）
     *
     * @param operationType 操作类型
     * @param resourceType  资源类型
     * @param resourceUuid  资源UUID
     * @param resourceName  资源名称
     */
    @Transactional
    public void log(String operationType, String resourceType, String resourceUuid, String resourceName) {
        log(operationType, resourceType, resourceUuid, resourceName, true, null);
    }

    /**
     * 记录审计日志（自动获取用户上下文）
     *
     * @param operationType 操作类型
     * @param resourceType  资源类型
     * @param resourceUuid  资源UUID
     * @param resourceName  资源名称
     * @param success       操作是否成功
     * @param errorMessage  错误信息（失败时记录）
     */
    @Transactional
    public void log(String operationType, String resourceType, String resourceUuid, String resourceName,
                    boolean success, String errorMessage) {
        // 从ThreadContext获取用户信息
        Long userId = ThreadContext.getCurrentUserId();
        String username = ThreadContext.getCurrentUsername();
        String tenantUuid = ThreadContext.getCurrentTenantUuid();
        String requestId = ThreadContext.getRequestId();
        String clientIp = ThreadContext.getClientIp();
        String userAgent = ThreadContext.getUserAgent();

        log(operationType, resourceType, resourceUuid, resourceName, success, errorMessage,
                null, userId, username, tenantUuid, requestId, clientIp, null);
    }

    /**
     * 记录完整审计日志
     *
     * @param operationType 操作类型
     * @param resourceType  资源类型
     * @param resourceUuid  资源UUID
     * @param resourceName  资源名称
     * @param success       操作是否成功
     * @param errorMessage  错误信息
     * @param duration      操作耗时（毫秒）
     * @param userId        用户ID
     * @param username      用户名
     * @param tenantUuid    租户UUID
     * @param requestId     请求ID
     * @param clientIp      客户端IP
     * @param description   操作描述
     */
    @Transactional
    public void log(String operationType, String resourceType, String resourceUuid, String resourceName,
                    boolean success, String errorMessage, Long duration,
                    Long userId, String username, String tenantUuid,
                    String requestId, String clientIp, String description) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUuid(UuidUtil.create());
        auditLog.setUserId(userId);
        auditLog.setUserName(username);
        auditLog.setTenantUuid(tenantUuid);
        auditLog.setOperationType(operationType);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceUuid(resourceUuid);
        auditLog.setResourceName(resourceName);
        auditLog.setSuccess(success);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setDuration(duration);
        auditLog.setRequestId(requestId);
        auditLog.setClientIp(clientIp);
        auditLog.setDescription(description);
        auditLog.setCreatedTime(LocalDateTime.now());

        auditLogMapper.insert(auditLog);

        log.info("Audit log recorded: {} - {} - {} by user {} (tenant: {}, ip: {}, requestId: {})",
                operationType, resourceType, resourceUuid, userId, tenantUuid, clientIp, requestId);
    }

    /**
     * 查询审计日志列表
     *
     * @param conditions 查询条件
     * @param page       页码
     * @param size       每页大小
     * @return 日志列表
     */
    public List<AuditLog> queryLogs(Map<String, Object> conditions, int page, int size) {
        Long userId = (Long) conditions.get("userId");
        String tenantUuid = (String) conditions.get("tenantUuid");
        String operationType = (String) conditions.get("operationType");
        String resourceType = (String) conditions.get("resourceType");
        LocalDateTime startTime = (LocalDateTime) conditions.get("startTime");
        LocalDateTime endTime = (LocalDateTime) conditions.get("endTime");

        int offset = (page - 1) * size;
        return auditLogMapper.selectByConditions(
                userId, tenantUuid, operationType, resourceType, startTime, endTime, offset, size
        );
    }

    /**
     * 统计审计日志数量
     *
     * @param conditions 查询条件
     * @return 日志总数
     */
    public int countLogs(Map<String, Object> conditions) {
        Long userId = (Long) conditions.get("userId");
        String tenantUuid = (String) conditions.get("tenantUuid");
        String operationType = (String) conditions.get("operationType");
        String resourceType = (String) conditions.get("resourceType");
        LocalDateTime startTime = (LocalDateTime) conditions.get("startTime");
        LocalDateTime endTime = (LocalDateTime) conditions.get("endTime");

        return auditLogMapper.countByConditions(
                userId, tenantUuid, operationType, resourceType, startTime, endTime
        );
    }

    /**
     * 分页查询审计日志
     *
     * @param conditions 查询条件
     * @param page       页码
     * @param size       每页大小
     * @return 分页结果
     */
    public Map<String, Object> queryLogsWithPagination(Map<String, Object> conditions, int page, int size) {
        List<AuditLog> logs = queryLogs(conditions, page, size);
        int total = countLogs(conditions);
        int totalPages = (int) Math.ceil((double) total / size);

        Map<String, Object> result = new HashMap<>();
        result.put("data", logs);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", totalPages);

        return result;
    }
}