package com.kes.dto.response;

import com.kes.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleResponse {

    private Long id;
    private String uuid;
    private String tenantUuid;
    private String name;
    private String code;
    private String description;
    private String permissions;
    private Boolean isSystem;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static RoleResponse fromEntity(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setUuid(role.getUuid());
        response.setTenantUuid(role.getTenantUuid());
        response.setName(role.getName());
        response.setCode(role.getCode());
        response.setDescription(role.getDescription());
        response.setPermissions(role.getPermissions());
        response.setIsSystem(role.getIsSystem());
        response.setCreatedTime(role.getCreatedTime());
        response.setUpdatedTime(role.getUpdatedTime());
        return response;
    }
}