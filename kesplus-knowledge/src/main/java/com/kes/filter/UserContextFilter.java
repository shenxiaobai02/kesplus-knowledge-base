package com.kes.filter;

import com.kes.util.ThreadContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 用户上下文注入过滤器
 * <p>
 * 从请求头获取用户身份信息，注入到ThreadContext中供后续权限校验使用。
 * 支持的请求头字段：
 * - X-User-Id: 用户ID
 * - X-User-Name: 用户名称
 * - X-Tenant-Uuid: 租户UUID
 * - X-Is-Admin: 是否管理员（true/false）
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(1)
public class UserContextFilter extends OncePerRequestFilter {

    /**
     * 用户ID请求头字段名
     */
    private static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 用户名称请求头字段名
     */
    private static final String HEADER_USER_NAME = "X-User-Name";

    /**
     * 租户UUID请求头字段名
     */
    private static final String HEADER_TENANT_UUID = "X-Tenant-Uuid";

    /**
     * 是否管理员请求头字段名
     */
    private static final String HEADER_IS_ADMIN = "X-Is-Admin";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 生成请求ID用于链路追踪
            String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            ThreadContext.setRequestId(requestId);

            // 从请求头获取用户信息
            String userIdStr = request.getHeader(HEADER_USER_ID);
            String userName = request.getHeader(HEADER_USER_NAME);
            String tenantUuid = request.getHeader(HEADER_TENANT_UUID);
            String isAdminStr = request.getHeader(HEADER_IS_ADMIN);

            // 构建用户上下文
            ThreadContext.UserContext userContext = new ThreadContext.UserContext();
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    userContext.setUserId(Long.parseLong(userIdStr));
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-User-Id header value: {}", userIdStr);
                }
            }
            userContext.setUsername(userName != null ? userName : "anonymous");
            userContext.setTenantUuid(tenantUuid);
            userContext.setIsAdmin(Boolean.parseBoolean(isAdminStr));

            // 注入上下文
            ThreadContext.setUserContext(userContext);

            // 设置客户端IP和User-Agent
            ThreadContext.setClientIp(extractClientIp(request));
            ThreadContext.setUserAgent(request.getHeader("User-Agent"));

            log.debug("UserContext initialized: userId={}, tenantUuid={}, requestId={}",
                    userContext.getUserId(), userContext.getTenantUuid(), requestId);

            // 继续执行请求
            filterChain.doFilter(request, response);

        } finally {
            // 清理上下文，防止内存泄漏
            ThreadContext.clear();
            log.debug("UserContext cleared");
        }
    }

    /**
     * 提取客户端真实IP地址
     * <p>
     * 支持从代理服务器传递的头部获取真实IP
     * </p>
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String extractClientIp(HttpServletRequest request) {
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
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    /**
     * 判断是否需要过滤该请求
     * <p>
     * 静态资源、健康检查等路径不需要注入用户上下文
     * </p>
     *
     * @param request HTTP请求对象
     * @return true表示不过滤，false表示需要过滤
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 排除静态资源和健康检查端点
        return path.startsWith("/actuator/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/favicon.ico");
    }
}