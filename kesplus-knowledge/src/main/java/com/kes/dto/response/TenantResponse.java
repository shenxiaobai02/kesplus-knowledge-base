package com.kes.dto.response;

import com.kes.entity.Tenant;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantResponse {

    private Long id;
    private String uuid;
    private String name;
    private String code;
    private String description;
    private String status;
    private String configJson;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static TenantResponse fromEntity(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setUuid(tenant.getUuid());
        response.setName(tenant.getName());
        response.setCode(tenant.getCode());
        response.setDescription(tenant.getDescription());
        response.setStatus(tenant.getStatus());
        response.setConfigJson(tenant.getConfigJson());
        response.setCreatedTime(tenant.getCreatedTime());
        response.setUpdatedTime(tenant.getUpdatedTime());
        return response;
    }
}