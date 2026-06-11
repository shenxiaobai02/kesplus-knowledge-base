package com.kes.service;

import com.kes.dto.request.TenantCreateRequest;
import com.kes.dto.request.TenantUpdateRequest;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantMapper tenantMapper;

    @InjectMocks
    private TenantService tenantService;

    private Tenant tenant;
    private TenantCreateRequest createRequest;
    private TenantUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setUuid(UuidUtil.generate());
        tenant.setName("Test Tenant");
        tenant.setCode("TEST");
        tenant.setDescription("Test description");
        tenant.setStatus("ACTIVE");
        tenant.setCreatedTime(LocalDateTime.now());
        tenant.setUpdatedTime(LocalDateTime.now());

        createRequest = new TenantCreateRequest();
        createRequest.setName("Test Tenant");
        createRequest.setCode("TEST");
        createRequest.setDescription("Test description");

        updateRequest = new TenantUpdateRequest();
        updateRequest.setName("Updated Tenant");
        updateRequest.setDescription("Updated description");
    }

    @Test
    void testCreateTenant() {
        when(tenantMapper.insert(any(Tenant.class))).thenReturn(1);
        when(tenantMapper.selectById(any(Long.class))).thenReturn(tenant);

        Tenant result = tenantService.create(createRequest);

        assertNotNull(result);
        assertEquals("Test Tenant", result.getName());
        assertEquals("TEST", result.getCode());
        verify(tenantMapper, times(1)).insert(any(Tenant.class));
    }

    @Test
    void testFindById() {
        when(tenantMapper.selectById(eq(1L))).thenReturn(tenant);

        Tenant result = tenantService.findById(1L);

        assertNotNull(result);
        assertEquals("Test Tenant", result.getName());
    }

    @Test
    void testFindByUuid() {
        when(tenantMapper.selectOne(any())).thenReturn(tenant);

        Tenant result = tenantService.findByUuid(tenant.getUuid());

        assertNotNull(result);
        assertEquals(tenant.getUuid(), result.getUuid());
    }

    @Test
    void testFindAll() {
        when(tenantMapper.selectList(any())).thenReturn(Arrays.asList(tenant));

        List<Tenant> result = tenantService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateTenant() {
        when(tenantMapper.selectOne(any())).thenReturn(tenant);
        when(tenantMapper.updateById(any(Tenant.class))).thenReturn(1);

        Tenant result = tenantService.update(tenant.getUuid(), updateRequest);

        assertNotNull(result);
        assertEquals("Updated Tenant", result.getName());
        verify(tenantMapper, times(1)).updateById(any(Tenant.class));
    }

    @Test
    void testDeleteTenant() {
        when(tenantMapper.selectOne(any())).thenReturn(tenant);
        when(tenantMapper.updateById(any(Tenant.class))).thenReturn(1);

        tenantService.delete(tenant.getUuid());

        verify(tenantMapper, times(1)).updateById(any(Tenant.class));
    }

    @Test
    void testSearch() {
        when(tenantMapper.selectList(any())).thenReturn(Arrays.asList(tenant));

        List<Tenant> result = tenantService.search("Test");

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
