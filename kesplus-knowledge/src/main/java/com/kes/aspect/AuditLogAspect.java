package com.kes.aspect;

import com.kes.annotation.AuditLog;
import com.kes.service.AuditService;
import com.kes.util.ThreadContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 审计日志切面
 * <p>
 * 自动拦截标记@AuditLog注解的方法，记录操作审计日志。
 * 支持记录操作类型、资源类型、操作结果、耗时等信息。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditService auditService;

    /**
     * 拦截标记@AuditLog注解的方法，记录审计日志
     *
     * @param joinPoint 切入点
     * @param auditLog  审计日志注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String operationType = auditLog.operationType();
        String resourceType = auditLog.resourceType();
        String description = auditLog.description();

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // 提取资源UUID（从方法参数中获取）
        String resourceUuid = extractResourceUuid(joinPoint);
        String resourceName = extractResourceName(joinPoint);

        // 获取用户上下文信息
        Long userId = ThreadContext.getCurrentUserId();
        String username = ThreadContext.getCurrentUsername();
        String tenantUuid = ThreadContext.getCurrentTenantUuid();
        String requestId = ThreadContext.getRequestId();
        String clientIp = ThreadContext.getClientIp();

        log.debug("AuditLog started: operation={}, resource={}, method={}, userId={}",
                operationType, resourceType, methodName, userId);

        Object result = null;
        boolean success = true;
        String errorMessage = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();

            // 计算耗时
            long duration = System.currentTimeMillis() - startTime;

            // 记录成功日志
            auditService.log(
                    operationType,
                    resourceType,
                    resourceUuid,
                    resourceName,
                    true,
                    null,
                    duration,
                    userId,
                    username,
                    tenantUuid,
                    requestId,
                    clientIp,
                    description
            );

            log.info("AuditLog success: operation={}, resource={}, duration={}ms, userId={}",
                    operationType, resourceType, duration, userId);

            return result;

        } catch (Throwable e) {
            // 计算耗时
            long duration = System.currentTimeMillis() - startTime;
            success = false;
            errorMessage = e.getMessage();

            // 记录失败日志
            auditService.log(
                    operationType,
                    resourceType,
                    resourceUuid,
                    resourceName,
                    false,
                    errorMessage,
                    duration,
                    userId,
                    username,
                    tenantUuid,
                    requestId,
                    clientIp,
                    description
            );

            log.warn("AuditLog failed: operation={}, resource={}, error={}, duration={}ms, userId={}",
                    operationType, resourceType, errorMessage, duration, userId);

            // 继续抛出异常，不影响原有业务逻辑
            throw e;
        }
    }

    /**
     * 从方法参数中提取资源UUID
     * <p>
     * 支持从PathVariable参数中提取uuid字段
     * </p>
     *
     * @param joinPoint 切入点
     * @return 资源UUID，未找到时返回null
     */
    private String extractResourceUuid(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                String paramName = parameterNames[i];
                Object argValue = args[i];

                // 查找uuid相关参数
                if ("uuid".equals(paramName) || "kbUuid".equals(paramName) || "id".equals(paramName)) {
                    return argValue != null ? argValue.toString() : null;
                }
            }
        }

        return null;
    }

    /**
     * 从方法参数中提取资源名称
     * <p>
     * 支持从请求体参数中提取name/title字段
     * </p>
     *
     * @param joinPoint 切入点
     * @return 资源名称，未找到时返回null
     */
    private String extractResourceName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                Object argValue = args[i];

                // 查找请求体对象中的title/name字段
                if (argValue != null) {
                    try {
                        // 尝试获取title字段
                        java.lang.reflect.Field titleField = argValue.getClass().getDeclaredField("title");
                        titleField.setAccessible(true);
                        Object titleValue = titleField.get(argValue);
                        if (titleValue != null) {
                            return titleValue.toString();
                        }
                    } catch (NoSuchFieldException e) {
                        log.trace("Field 'title' not found in {}", argValue.getClass().getSimpleName());
                    } catch (IllegalAccessException e) {
                        log.warn("Cannot access field 'title' in {}: {}", argValue.getClass().getSimpleName(), e.getMessage());
                    }

                    try {
                        // 尝试获取name字段
                        java.lang.reflect.Field nameField = argValue.getClass().getDeclaredField("name");
                        nameField.setAccessible(true);
                        Object nameValue = nameField.get(argValue);
                        if (nameValue != null) {
                            return nameValue.toString();
                        }
                    } catch (NoSuchFieldException e) {
                        log.trace("Field 'name' not found in {}", argValue.getClass().getSimpleName());
                    } catch (IllegalAccessException e) {
                        log.warn("Cannot access field 'name' in {}: {}", argValue.getClass().getSimpleName(), e.getMessage());
                    }
                }
            }
        }

        return null;
    }
}