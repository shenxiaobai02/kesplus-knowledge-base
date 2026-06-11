package com.kes.service;

import com.kes.entity.Tenant;
import com.kes.mapper.TenantMapper;
import com.kes.util.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantMapper tenantMapper;

    @InjectMocks
    private TenantService tenantService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setUuid(UuidUtil.create());
        tenant.setName("Test Tenant");
        tenant.setCode("TEST");
        tenant.setDescription("Test description");
        tenant.setStatus("ACTIVE");
        tenant.setCreatedTime(LocalDateTime.now());
        tenant.setUpdatedTime(LocalDateTime.now());
    }

    @Test
    void testCreateTenant() {
        when(tenantMapper.insert(any(Tenant.class))).thenReturn(1);

        Tenant result = tenantService.create(tenant);

        assertNotNull(result);
        assertEquals("Test Tenant", result.getName());
        assertEquals("TEST", result.getCode());
        verify(tenantMapper, times(1)).insert(any(Tenant.class));
    }

    @Test
    void testGetById() {
        when(tenantMapper.selectById(eq(1L))).thenReturn(tenant);

        Tenant result = tenantService.getById(1L);

        assertNotNull(result);
        assertEquals("Test Tenant", result.getName());
    }

    @Test
    void testGetByUuid() {
        when(tenantMapper.selectByUuid(eq(tenant.getUuid()))).thenReturn(tenant);

        Tenant result = tenantService.getByUuid(tenant.getUuid());

        assertNotNull(result);
        assertEquals(tenant.getUuid(), result.getUuid());
    }

    @Test
    void testListAll() {
        when(tenantMapper.selectAllActive()).thenReturn(Arrays.asList(tenant));

        List<Tenant> result = tenantService.listAll();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateTenant() {
        when(tenantMapper.selectByUuid(eq(tenant.getUuid()))).thenReturn(tenant);
        when(tenantMapper.updateById(any(Tenant.class))).thenReturn(1);

        tenant.setName("Updated Tenant");
        Tenant result = tenantService.update(tenant);

        assertNotNull(result);
        assertEquals("Updated Tenant", result.getName());
        verify(tenantMapper, times(1)).updateById(any(Tenant.class));
    }

    @Test
    void testDeleteTenant() {
        when(tenantMapper.selectByUuid(eq(tenant.getUuid()))).thenReturn(tenant);
        when(tenantMapper.updateById(any(Tenant.class))).thenReturn(1);

        tenantService.delete(tenant.getUuid());

        verify(tenantMapper, times(1)).updateById(any(Tenant.class));
    }

    @Test
    void testGetByCode() {
        when(tenantMapper.selectByCode(eq("TEST"))).thenReturn(tenant);

        Tenant result = tenantService.getByCode("TEST");

        assertNotNull(result);
        assertEquals("TEST", result.getCode());
    }
}
