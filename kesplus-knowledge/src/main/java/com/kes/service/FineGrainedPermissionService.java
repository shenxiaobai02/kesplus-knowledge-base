package com.kes.service;

import com.kes.entity.Role;
import com.kes.exception.BaseException;
import com.kes.exception.ErrorCode;
import com.kes.mapper.KnowledgeBaseMapper;
import com.kes.util.ThreadContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 细粒度权限服务
 * <p>
 * 实现基于RBAC+ABAC混合模式的权限校验逻辑，
 * 支持资源级别和类型级别的权限控制
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Service
public class FineGrainedPermissionService {

    @Autowired
    private RoleService roleService;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 检查用户是否有指定资源的操作权限
     *
     * @param userId       用户ID
     * @param resourceType 资源类型（如KNOWLEDGE_BASE、ROLE等）
     * @param resourceUuid 资源UUID
     * @param action       操作类型（READ、WRITE、DELETE等）
     * @return true表示有权限，false表示无权限
     */
    public boolean hasPermission(Long userId, String resourceType, String resourceUuid, String action) {
        if (userId == null) {
            log.warn("Permission check failed: userId is null");
            return false;
        }

        // 管理员拥有所有权限
        if (ThreadContext.isAdmin()) {
            log.debug("Admin user {} has full permission", userId);
            return true;
        }

        return evaluateCondition(buildCondition(userId, resourceType, action),
                buildContext(userId, resourceType, resourceUuid));
    }

    /**
     * 检查访问权限，无权限时抛出异常
     *
     * @param userId       用户ID
     * @param resourceType 资源类型
     * @param resourceUuid 资源UUID
     * @param action       操作类型
     * @throws BaseException 无权限时抛出FORBIDDEN异常
     */
    public void checkAccess(Long userId, String resourceType, String resourceUuid, String action) {
        if (!hasPermission(userId, resourceType, resourceUuid, action)) {
            log.warn("Access denied for user {} to {} {} with action {}",
                    userId, resourceType, resourceUuid, action);
            throw new BaseException(ErrorCode.FORBIDDEN, "Permission denied");
        }
    }

    /**
     * 评估权限条件
     *
     * @param condition 权限条件字符串
     * @param context   评估上下文
     * @return true表示条件满足，false表示不满足
     */
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

    /**
     * 构建权限条件字符串
     *
     * @param userId       用户ID
     * @param resourceType 资源类型
     * @param action       操作类型
     * @return 条件字符串
     */
    private String buildCondition(Long userId, String resourceType, String action) {
        return action + "_" + resourceType + "_ALLOWED";
    }

    /**
     * 构建评估上下文
     *
     * @param userId       用户ID
     * @param resourceType 资源类型
     * @param resourceUuid 资源UUID
     * @return 上下文Map
     */
    private Map<String, Object> buildContext(Long userId, String resourceType, String resourceUuid) {
        return Map.of(
                "userId", userId,
                "resourceType", resourceType,
                "resourceUuid", resourceUuid
        );
    }

    /**
     * 执行权限表达式评估
     * <p>
     * 基于用户角色和资源类型进行权限判断：
     * 1. 获取用户在当前租户下的所有角色
     * 2. 检查角色权限中是否包含所需操作权限
     * 3. 对于知识库资源，额外检查资源级别的角色白名单
     * </p>
     *
     * @param condition 权限条件
     * @param context   评估上下文
     * @return true表示有权限
     */
    private boolean evaluateExpression(String condition, Map<String, Object> context) {
        Long userId = (Long) context.get("userId");
        String resourceType = (String) context.get("resourceType");
        String resourceUuid = (String) context.get("resourceUuid");

        // 获取当前租户UUID
        String tenantUuid = ThreadContext.getCurrentTenantUuid();

        // 获取用户角色列表
        List<Role> roles = roleService.getUserRoles(userId, tenantUuid);
        if (roles.isEmpty()) {
            log.debug("User {} has no roles in tenant {}", userId, tenantUuid);
            return false;
        }

        // 提取角色代码集合
        Set<String> roleCodes = roles.stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        // 检查类型级别权限（基于角色权限字段）
        for (Role role : roles) {
            if (checkRoleActionPermission(role, condition)) {
                log.debug("User {} has permission via role {}", userId, role.getCode());
                return true;
            }
        }

        // 对于知识库资源，检查资源级别权限
        if ("KNOWLEDGE_BASE".equals(resourceType)) {
            return checkKnowledgeBasePermission(userId, resourceUuid, roleCodes);
        }

        return false;
    }

    /**
     * 检查角色的操作权限
     *
     * @param role      角色对象
     * @param condition 权限条件（如READ_KNOWLEDGE_BASE_ALLOWED）
     * @return true表示角色有该权限
     */
    private boolean checkRoleActionPermission(Role role, String condition) {
        String permissions = role.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        // 解析权限条件获取操作类型
        // 格式: ACTION_RESOURCE_ALLOWED -> ACTION
        String action = condition.split("_")[0];

        // 检查权限字符串是否包含该操作
        // 权限格式示例: "READ,WRITE,DELETE"
        return permissions.contains(action);
    }

    /**
     * 检查知识库资源级别权限
     *
     * @param userId    用户ID
     * @param kbUuid    知识库UUID
     * @param roleCodes 用户角色代码集合
     * @return true表示有权限访问该知识库
     */
    private boolean checkKnowledgeBasePermission(Long userId, String kbUuid, Set<String> roleCodes) {
        var kb = knowledgeBaseMapper.selectByUuid(kbUuid);
        if (kb == null) {
            return false;
        }

        // 公开知识库所有人可访问
        if (Boolean.TRUE.equals(kb.getIsPublic())) {
            return true;
        }

        // 所有者有完全权限
        if (userId.equals(kb.getOwnerId())) {
            return true;
        }

        // 检查知识库的角色白名单
        String allowedRoleCodes = kb.getAllowedRoleCodes();
        if (allowedRoleCodes != null && !allowedRoleCodes.isEmpty()) {
            Set<String> allowedRoles = Set.of(allowedRoleCodes.split(","));
            for (String roleCode : roleCodes) {
                if (allowedRoles.contains(roleCode)) {
                    return true;
                }
            }
        }

        return false;
    }
}