package com.kes.dto.request;

import lombok.Data;

@Data
public class RoleUpdateRequest {

    private String name;

    private String description;

    private String permissions;
}