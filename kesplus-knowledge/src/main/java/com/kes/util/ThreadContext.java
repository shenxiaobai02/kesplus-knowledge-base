package com.kes.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 线程上下文工具类
 * <p>
 * 用于存储和获取请求相关的上下文信息，包括用户信息、请求ID、客户端IP等。
 * 基于 ThreadLocal 实现，确保线程隔离。
 * </p>
 */
public class ThreadContext {

    private static final ThreadLocal<UserContext> USER_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_AGENT = new ThreadLocal<>();

    @Data
    public static class UserContext {
        private Long userId;
        private String username;
        private String email;
        private Boolean isAdmin;
        private String tenantUuid;
    }

    public static void setUserContext(UserContext context) {
        USER_CONTEXT.set(context);
    }

    public static UserContext getUserContext() {
        return USER_CONTEXT.get();
    }

    public static Long getCurrentUserId() {
        UserContext context = USER_CONTEXT.get();
        return context != null ? context.getUserId() : null;
    }

    public static String getCurrentUsername() {
        UserContext context = USER_CONTEXT.get();
        return context != null ? context.getUsername() : null;
    }

    public static Boolean isAdmin() {
        UserContext context = USER_CONTEXT.get();
        return context != null && Boolean.TRUE.equals(context.getIsAdmin());
    }

    public static String getCurrentTenantUuid() {
        UserContext context = USER_CONTEXT.get();
        return context != null ? context.getTenantUuid() : null;
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void setClientIp(String ip) {
        CLIENT_IP.set(ip);
    }

    public static String getClientIp() {
        String ip = CLIENT_IP.get();
        if (ip == null) {
            ip = getClientIpFromRequest();
            CLIENT_IP.set(ip);
        }
        return ip;
    }

    public static void setUserAgent(String userAgent) {
        USER_AGENT.set(userAgent);
    }

    public static String getUserAgent() {
        String agent = USER_AGENT.get();
        if (agent == null) {
            agent = getUserAgentFromRequest();
            USER_AGENT.set(agent);
        }
        return agent;
    }

    private static String getClientIpFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "unknown";
    }

    private static String getUserAgentFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "unknown";
    }

    public static void clear() {
        USER_CONTEXT.remove();
        REQUEST_ID.remove();
        CLIENT_IP.remove();
        USER_AGENT.remove();
    }
}
