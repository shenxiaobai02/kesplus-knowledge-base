package com.kes.service;

import com.kes.entity.AuditLog;
import com.kes.mapper.AuditLogMapper;
import com.kes.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AuditService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Transactional
    public void log(String operationType, String resourceType, String resourceUuid, String resourceName) {
        log(operationType, resourceType, resourceUuid, resourceName, true, null);
    }

    @Transactional
    public void log(String operationType, String resourceType, String resourceUuid, String resourceName, boolean success, String errorMessage) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUuid(UuidUtil.create());
        auditLog.setOperationType(operationType);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceUuid(resourceUuid);
        auditLog.setResourceName(resourceName);
        auditLog.setSuccess(success);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setCreatedTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
        log.info("Audit log recorded: {} - {} - {}", operationType, resourceType, resourceUuid);
    }

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