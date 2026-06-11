package com.kes.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kes.entity.Role;
import com.kes.entity.UserRole;
import com.kes.mapper.RoleMapper;
import com.kes.mapper.UserRoleMapper;
import com.kes.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Transactional
    public Role create(Role role) {
        role.setUuid(UuidUtil.create());
        role.setIsDeleted(false);
        role.setCreatedTime(LocalDateTime.now());
        role.setUpdatedTime(LocalDateTime.now());
        baseMapper.insert(role);
        log.info("Created role: {}", role.getName());
        return role;
    }

    @Transactional
    public Role update(Role role) {
        Role existing = getByUuid(role.getUuid());
        if (existing == null) {
            throw new RuntimeException("Role not found");
        }
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new RuntimeException("Cannot modify system role");
        }
        role.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(role);
        log.info("Updated role: {}", role.getName());
        return role;
    }

    @Transactional
    public void delete(String uuid) {
        Role role = getByUuid(uuid);
        if (role == null) {
            return;
        }
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new RuntimeException("Cannot delete system role");
        }
        role.setIsDeleted(true);
        role.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(role);

        userRoleMapper.deleteByRoleUuid(uuid);
        log.info("Deleted role: {}", uuid);
    }

    public Role getByUuid(String uuid) {
        return baseMapper.selectByUuid(uuid);
    }

    public Role getByCode(String code) {
        return baseMapper.selectByCode(code);
    }

    public List<Role> listByTenant(String tenantUuid) {
        return baseMapper.selectByTenantUuid(tenantUuid).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .collect(Collectors.toList());
    }

    public List<Role> listSystemRoles() {
        return baseMapper.selectSystemRoles().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignRole(Long userId, String roleUuid, String tenantUuid) {
        Role role = getByUuid(roleUuid);
        if (role == null) {
            throw new RuntimeException("Role not found");
        }

        List<String> existingRoles = userRoleMapper.selectRoleUuidsByUserIdAndTenant(userId, tenantUuid);
        if (existingRoles.contains(roleUuid)) {
            return;
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleUuid(roleUuid);
        userRole.setTenantUuid(tenantUuid);
        userRole.setCreatedTime(LocalDateTime.now());
        userRoleMapper.insert(userRole);
        log.info("Assigned role {} to user {} in tenant {}", roleUuid, userId, tenantUuid);
    }

    @Transactional
    public void revokeRole(Long userId, String roleUuid, String tenantUuid) {
        userRoleMapper.deleteByUserIdAndRoleUuid(userId, roleUuid);
        log.info("Revoked role {} from user {} in tenant {}", roleUuid, userId, tenantUuid);
    }

    public List<Role> getUserRoles(Long userId) {
        List<String> roleUuids = userRoleMapper.selectRoleUuidsByUserId(userId);
        List<Role> roles = new ArrayList<>();
        for (String uuid : roleUuids) {
            Role role = getByUuid(uuid);
            if (role != null && !Boolean.TRUE.equals(role.getIsDeleted())) {
                roles.add(role);
            }
        }
        return roles;
    }

    public List<Role> getUserRoles(Long userId, String tenantUuid) {
        List<String> roleUuids = userRoleMapper.selectRoleUuidsByUserIdAndTenant(userId, tenantUuid);
        List<Role> roles = new ArrayList<>();
        for (String uuid : roleUuids) {
            Role role = getByUuid(uuid);
            if (role != null && !Boolean.TRUE.equals(role.getIsDeleted())) {
                roles.add(role);
            }
        }
        return roles;
    }
}