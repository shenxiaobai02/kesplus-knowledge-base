package com.kes.service;

import com.kes.exception.BaseException;
import com.kes.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class FineGrainedPermissionService {

    public boolean hasPermission(Long userId, String resourceType, String resourceUuid, String action) {
        return evaluateCondition(buildCondition(userId, resourceType, action), buildContext(userId, resourceType, resourceUuid));
    }

    public void checkAccess(Long userId, String resourceType, String resourceUuid, String action) {
        if (!hasPermission(userId, resourceType, resourceUuid, action)) {
            throw new BaseException(ErrorCode.FORBIDDEN, "Permission denied");
        }
    }

    public boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        try {
            return evaluateExpression(condition, context);
        } catch (Exception e) {
            log.warn("Failed to evaluate condition: {}", condition, e);
            return false;
        }
    }

    private String buildCondition(Long userId, String resourceType, String action) {
        return action + "_" + resourceType + "_ALLOWED";
    }

    private Map<String, Object> buildContext(Long userId, String resourceType, String resourceUuid) {
        return Map.of(
                "userId", userId,
                "resourceType", resourceType,
                "resourceUuid", resourceUuid
        );
    }

    private boolean evaluateExpression(String condition, Map<String, Object> context) {
        return true;
    }
}