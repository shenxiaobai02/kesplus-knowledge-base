package com.kes.controller;

import com.kes.common.ResponseWrapper;
import com.kes.dto.request.TenantCreateRequest;
import com.kes.dto.request.TenantUpdateRequest;
import com.kes.dto.response.TenantResponse;
import com.kes.entity.BusinessLine;
import com.kes.entity.Tenant;
import com.kes.service.TenantService;
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
@RequestMapping("/api/tenant")
@Tag(name = "租户管理", description = "租户的CRUD操作和业务线管理")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @PostMapping
    @Operation(summary = "创建租户", description = "创建一个新的租户")
    public ResponseWrapper<TenantResponse> create(@Valid @RequestBody TenantCreateRequest request) {
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setCode(request.getCode());
        tenant.setDescription(request.getDescription());
        tenant.setConfigJson(request.getConfigJson());

        Tenant created = tenantService.create(tenant);
        return ResponseWrapper.success(TenantResponse.fromEntity(created));
    }

    @GetMapping
    @Operation(summary = "查询租户列表", description = "获取所有租户列表")
    public ResponseWrapper<List<TenantResponse>> list() {
        List<Tenant> tenants = tenantService.listAll();
        List<TenantResponse> responses = tenants.stream()
                .map(TenantResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseWrapper.success(responses);
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "查询租户详情", description = "根据UUID获取租户详情")
    public ResponseWrapper<TenantResponse> get(
            @Parameter(description = "租户UUID") @PathVariable String uuid) {
        Tenant tenant = tenantService.getByUuid(uuid);
        if (tenant == null) {
            return ResponseWrapper.error("租户不存在");
        }
        return ResponseWrapper.success(TenantResponse.fromEntity(tenant));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "更新租户信息", description = "更新租户信息")
    public ResponseWrapper<TenantResponse> update(
            @Parameter(description = "租户UUID") @PathVariable String uuid,
            @RequestBody TenantUpdateRequest request) {
        Tenant tenant = new Tenant();
        tenant.setUuid(uuid);
        tenant.setName(request.getName());
        tenant.setDescription(request.getDescription());
        tenant.setStatus(request.getStatus());
        tenant.setConfigJson(request.getConfigJson());

        Tenant updated = tenantService.update(tenant);
        return ResponseWrapper.success(TenantResponse.fromEntity(updated));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "删除租户", description = "软删除租户")
    public ResponseWrapper<Void> delete(@Parameter(description = "租户UUID") @PathVariable String uuid) {
        tenantService.delete(uuid);
        return ResponseWrapper.success();
    }

    @PostMapping("/{tenantUuid}/business-line")
    @Operation(summary = "创建业务线", description = "为租户创建业务线")
    public ResponseWrapper<BusinessLine> createBusinessLine(
            @Parameter(description = "租户UUID") @PathVariable String tenantUuid,
            @RequestBody BusinessLine businessLine) {
        BusinessLine created = tenantService.createBusinessLine(tenantUuid, businessLine);
        return ResponseWrapper.success(created);
    }

    @GetMapping("/{tenantUuid}/business-line")
    @Operation(summary = "查询业务线列表", description = "获取租户下的业务线列表")
    public ResponseWrapper<List<BusinessLine>> listBusinessLines(
            @Parameter(description = "租户UUID") @PathVariable String tenantUuid) {
        List<BusinessLine> businessLines = tenantService.listBusinessLines(tenantUuid);
        return ResponseWrapper.success(businessLines);
    }

    @GetMapping("/{tenantUuid}/business-line/{blUuid}")
    @Operation(summary = "查询业务线详情", description = "获取业务线详情")
    public ResponseWrapper<BusinessLine> getBusinessLine(
            @Parameter(description = "租户UUID") @PathVariable String tenantUuid,
            @Parameter(description = "业务线UUID") @PathVariable String blUuid) {
        BusinessLine businessLine = tenantService.getBusinessLine(tenantUuid, blUuid);
        if (businessLine == null) {
            return ResponseWrapper.error("业务线不存在");
        }
        return ResponseWrapper.success(businessLine);
    }

    @PutMapping("/{tenantUuid}/business-line/{blUuid}")
    @Operation(summary = "更新业务线", description = "更新业务线信息")
    public ResponseWrapper<BusinessLine> updateBusinessLine(
            @Parameter(description = "租户UUID") @PathVariable String tenantUuid,
            @Parameter(description = "业务线UUID") @PathVariable String blUuid,
            @RequestBody BusinessLine businessLine) {
        BusinessLine updated = tenantService.updateBusinessLine(tenantUuid, blUuid, businessLine);
        return ResponseWrapper.success(updated);
    }

    @DeleteMapping("/{tenantUuid}/business-line/{blUuid}")
    @Operation(summary = "删除业务线", description = "软删除业务线")
    public ResponseWrapper<Void> deleteBusinessLine(
            @Parameter(description = "租户UUID") @PathVariable String tenantUuid,
            @Parameter(description = "业务线UUID") @PathVariable String blUuid) {
        tenantService.deleteBusinessLine(tenantUuid, blUuid);
        return ResponseWrapper.success();
    }
}