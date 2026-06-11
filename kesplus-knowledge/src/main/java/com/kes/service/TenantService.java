package com.kes.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kes.entity.BusinessLine;
import com.kes.entity.Tenant;
import com.kes.mapper.BusinessLineMapper;
import com.kes.mapper.TenantMapper;
import com.kes.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TenantService extends ServiceImpl<TenantMapper, Tenant> {

    @Autowired
    private BusinessLineMapper businessLineMapper;

    @Transactional
    public Tenant create(Tenant tenant) {
        tenant.setUuid(UuidUtil.create());
        tenant.setStatus("ACTIVE");
        tenant.setIsDeleted(false);
        tenant.setCreatedTime(LocalDateTime.now());
        tenant.setUpdatedTime(LocalDateTime.now());
        baseMapper.insert(tenant);
        log.info("Created tenant: {}", tenant.getName());
        return tenant;
    }

    @Transactional
    public Tenant update(Tenant tenant) {
        Tenant existing = getByUuid(tenant.getUuid());
        if (existing == null) {
            throw new RuntimeException("Tenant not found");
        }
        tenant.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(tenant);
        log.info("Updated tenant: {}", tenant.getName());
        return tenant;
    }

    @Transactional
    public void delete(String uuid) {
        Tenant tenant = getByUuid(uuid);
        if (tenant == null) {
            return;
        }
        tenant.setIsDeleted(true);
        tenant.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(tenant);

        List<BusinessLine> businessLines = businessLineMapper.selectByTenantUuid(uuid);
        for (BusinessLine bl : businessLines) {
            bl.setIsDeleted(true);
            bl.setUpdatedTime(LocalDateTime.now());
            businessLineMapper.updateById(bl);
        }
        log.info("Deleted tenant: {}", uuid);
    }

    public Tenant getByUuid(String uuid) {
        return baseMapper.selectByUuid(uuid);
    }

    public Tenant getByCode(String code) {
        return baseMapper.selectByCode(code);
    }

    public List<Tenant> listAll() {
        return baseMapper.selectAllActive().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .collect(Collectors.toList());
    }

    @Transactional
    public BusinessLine createBusinessLine(String tenantUuid, BusinessLine businessLine) {
        Tenant tenant = getByUuid(tenantUuid);
        if (tenant == null) {
            throw new RuntimeException("Tenant not found");
        }
        businessLine.setUuid(UuidUtil.create());
        businessLine.setTenantUuid(tenantUuid);
        businessLine.setIsDeleted(false);
        businessLine.setCreatedTime(LocalDateTime.now());
        businessLine.setUpdatedTime(LocalDateTime.now());
        businessLineMapper.insert(businessLine);
        log.info("Created business line: {} for tenant: {}", businessLine.getName(), tenantUuid);
        return businessLine;
    }

    @Transactional
    public BusinessLine updateBusinessLine(String tenantUuid, String blUuid, BusinessLine businessLine) {
        BusinessLine existing = businessLineMapper.selectByUuid(blUuid);
        if (existing == null || !tenantUuid.equals(existing.getTenantUuid())) {
            throw new RuntimeException("Business line not found or not in tenant");
        }
        businessLine.setId(existing.getId());
        businessLine.setUuid(blUuid);
        businessLine.setTenantUuid(tenantUuid);
        businessLine.setUpdatedTime(LocalDateTime.now());
        businessLineMapper.updateById(businessLine);
        log.info("Updated business line: {}", blUuid);
        return businessLine;
    }

    @Transactional
    public void deleteBusinessLine(String tenantUuid, String blUuid) {
        BusinessLine bl = businessLineMapper.selectByUuid(blUuid);
        if (bl == null || !tenantUuid.equals(bl.getTenantUuid())) {
            return;
        }
        bl.setIsDeleted(true);
        bl.setUpdatedTime(LocalDateTime.now());
        businessLineMapper.updateById(bl);
        log.info("Deleted business line: {}", blUuid);
    }

    public BusinessLine getBusinessLine(String tenantUuid, String blUuid) {
        BusinessLine bl = businessLineMapper.selectByUuid(blUuid);
        if (bl != null && tenantUuid.equals(bl.getTenantUuid())) {
            return bl;
        }
        return null;
    }

    public List<BusinessLine> listBusinessLines(String tenantUuid) {
        return businessLineMapper.selectByTenantUuid(tenantUuid).stream()
                .filter(bl -> !Boolean.TRUE.equals(bl.getIsDeleted()))
                .collect(Collectors.toList());
    }
}