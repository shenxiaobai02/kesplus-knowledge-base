package com.kes.dto.request;

import lombok.Data;

@Data
public class TenantUpdateRequest {

    private String name;

    private String description;

    private String status;

    private String configJson;
}