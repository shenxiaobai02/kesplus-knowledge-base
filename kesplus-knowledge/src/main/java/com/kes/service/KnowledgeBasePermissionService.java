package com.kes.service;

import com.kes.entity.KnowledgeBase;
import com.kes.entity.Role;
import com.kes.mapper.KnowledgeBaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBasePermissionService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private RoleService roleService;

    public boolean hasPermission(Long userId, String kbUuid) {
        return hasPermission(userId, kbUuid, "READ");
    }

    public boolean hasPermission(Long userId, String kbUuid, String action) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByUuid(kbUuid);
        if (kb == null) {
            return false;
        }

        if (Boolean.TRUE.equals(kb.getIsDeleted())) {
            return false;
        }

        if (Boolean.TRUE.equals(kb.getIsPublic())) {
            return true;
        }

        if (userId == null) {
            return false;
        }

        if (userId.equals(kb.getOwnerId())) {
            return true;
        }

        List<Role> roles = roleService.getUserRoles(userId, kb.getTenantUuid());
        if (roles.isEmpty()) {
            return false;
        }

        Set<String> userRoleCodes = roles.stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        if (kb.getAllowedRoleCodes() != null && !kb.getAllowedRoleCodes().isEmpty()) {
            Set<String> allowedRoles = new HashSet<>(
                    Arrays.asList(kb.getAllowedRoleCodes().split(","))
            );
            for (String userRole : userRoleCodes) {
                if (allowedRoles.contains(userRole)) {
                    return true;
                }
            }
        }

        for (Role role : roles) {
            if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
                if (checkRolePermission(role.getPermissions(), action)) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<KnowledgeBase> filterAccessibleKbs(Long userId, List<KnowledgeBase> kbs) {
        if (userId == null) {
            return kbs.stream()
                    .filter(kb -> Boolean.TRUE.equals(kb.getIsPublic()) && !Boolean.TRUE.equals(kb.getIsDeleted()))
                    .collect(Collectors.toList());
        }

        return kbs.stream()
                .filter(kb -> !Boolean.TRUE.equals(kb.getIsDeleted()))
                .filter(kb -> hasPermission(userId, kb.getUuid()))
                .collect(Collectors.toList());
    }

    private boolean checkRolePermission(String permissionsJson, String action) {
        return permissionsJson.contains(action);
    }
}