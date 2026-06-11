package com.kes.controller;

import com.kes.common.ResponseWrapper;
import com.kes.dto.request.RoleCreateRequest;
import com.kes.dto.request.RoleUpdateRequest;
import com.kes.dto.response.RoleResponse;
import com.kes.entity.Role;
import com.kes.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/role")
@Tag(name = "角色管理", description = "角色的CRUD操作和用户角色分配")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @PostMapping
    @Operation(summary = "创建角色", description = "创建一个新的角色")
    public ResponseWrapper<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        Role role = new Role();
        role.setTenantUuid(request.getTenantUuid());
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setPermissions(request.getPermissions());
        role.setIsSystem(request.getIsSystem());

        Role created = roleService.create(role);
        return ResponseWrapper.success(RoleResponse.fromEntity(created));
    }

    @GetMapping
    @Operation(summary = "查询角色列表", description = "获取角色列表，可按租户过滤")
    public ResponseWrapper<List<RoleResponse>> list(
            @Parameter(description = "租户UUID") @RequestParam(required = false) String tenantUuid) {
        List<Role> roles;
        if (tenantUuid != null && !tenantUuid.isEmpty()) {
            roles = roleService.listByTenant(tenantUuid);
        } else {
            roles = roleService.listSystemRoles();
        }
        List<RoleResponse> responses = roles.stream()
                .map(RoleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseWrapper.success(responses);
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "查询角色详情", description = "根据UUID获取角色详情")
    public ResponseWrapper<RoleResponse> get(
            @Parameter(description = "角色UUID") @PathVariable String uuid) {
        Role role = roleService.getByUuid(uuid);
        if (role == null) {
            return ResponseWrapper.error("角色不存在");
        }
        return ResponseWrapper.success(RoleResponse.fromEntity(role));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "更新角色信息", description = "更新角色信息")
    public ResponseWrapper<RoleResponse> update(
            @Parameter(description = "角色UUID") @PathVariable String uuid,
            @RequestBody RoleUpdateRequest request) {
        Role role = new Role();
        role.setUuid(uuid);
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setPermissions(request.getPermissions());

        Role updated = roleService.update(role);
        return ResponseWrapper.success(RoleResponse.fromEntity(updated));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "删除角色", description = "软删除角色")
    public ResponseWrapper<Void> delete(@Parameter(description = "角色UUID") @PathVariable String uuid) {
        roleService.delete(uuid);
        return ResponseWrapper.success();
    }

    @PostMapping("/{roleUuid}/assign")
    @Operation(summary = "分配角色给用户", description = "将角色分配给指定用户")
    public ResponseWrapper<Void> assignRole(
            @Parameter(description = "角色UUID") @PathVariable String roleUuid,
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "租户UUID") @RequestParam String tenantUuid) {
        roleService.assignRole(userId, roleUuid, tenantUuid);
        return ResponseWrapper.success();
    }

    @PostMapping("/{roleUuid}/revoke")
    @Operation(summary = "撤销用户角色", description = "从用户撤销指定角色")
    public ResponseWrapper<Void> revokeRole(
            @Parameter(description = "角色UUID") @PathVariable String roleUuid,
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "租户UUID") @RequestParam String tenantUuid) {
        roleService.revokeRole(userId, roleUuid, tenantUuid);
        return ResponseWrapper.success();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户角色列表", description = "获取用户拥有的角色列表")
    public ResponseWrapper<List<RoleResponse>> getUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "租户UUID") @RequestParam(required = false) String tenantUuid) {
        List<Role> roles;
        if (tenantUuid != null && !tenantUuid.isEmpty()) {
            roles = roleService.getUserRoles(userId, tenantUuid);
        } else {
            roles = roleService.getUserRoles(userId);
        }
        List<RoleResponse> responses = roles.stream()
                .map(RoleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseWrapper.success(responses);
    }
}