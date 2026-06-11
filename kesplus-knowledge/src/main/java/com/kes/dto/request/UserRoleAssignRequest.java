package com.kes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleAssignRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "角色UUID不能为空")
    private String roleUuid;

    @NotBlank(message = "租户UUID不能为空")
    private String tenantUuid;
}