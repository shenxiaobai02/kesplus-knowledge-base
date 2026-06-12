package com.kes.service;

import com.kes.entity.Tenant;
import com.kes.util.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TenantServiceTest {

    @Autowired
    private TenantService tenantService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setCode("TEST");
        tenant.setDescription("Test description");
    }

    @Test
    void testCreateTenant() {
        Tenant result = tenantService.create(tenant);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getUuid());
        assertEquals("Test Tenant", result.getName());
        assertEquals("TEST", result.getCode());
        assertEquals("ACTIVE", result.getStatus());
        assertFalse(result.getIsDeleted());
    }

    @Test
    void testGetById() {
        Tenant created = tenantService.create(tenant);

        Tenant result = tenantService.getById(created.getId());

        assertNotNull(result);
        assertEquals(created.getId(), result.getId());
        assertEquals("Test Tenant", result.getName());
    }

    @Test
    void testGetByUuid() {
        Tenant created = tenantService.create(tenant);

        Tenant result = tenantService.getByUuid(created.getUuid());

        assertNotNull(result);
        assertEquals(created.getUuid(), result.getUuid());
        assertEquals("Test Tenant", result.getName());
    }

    @Test
    void testGetByCode() {
        tenantService.create(tenant);

        Tenant result = tenantService.getByCode("TEST");

        assertNotNull(result);
        assertEquals("TEST", result.getCode());
        assertEquals("Test Tenant", result.getName());
    }

    @Test
    void testListAll() {
        tenantService.create(tenant);

        Tenant tenant2 = new Tenant();
        tenant2.setName("Test Tenant 2");
        tenant2.setCode("TEST2");
        tenant2.setDescription("Test description 2");
        tenantService.create(tenant2);

        List<Tenant> result = tenantService.listAll();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testUpdateTenant() {
        Tenant created = tenantService.create(tenant);

        created.setName("Updated Tenant");
        created.setDescription("Updated description");
        Tenant result = tenantService.update(created);

        assertNotNull(result);
        assertEquals("Updated Tenant", result.getName());
        assertEquals("Updated description", result.getDescription());
    }

    @Test
    void testUpdateTenantNotFound() {
        tenant.setUuid(UuidUtil.create());
        
        assertThrows(RuntimeException.class, () -> tenantService.update(tenant));
    }

    @Test
    void testDeleteTenant() {
        Tenant created = tenantService.create(tenant);
        String uuid = created.getUuid();

        tenantService.delete(uuid);

        Tenant result = tenantService.getByUuid(uuid);
        assertTrue(result.getIsDeleted());
    }

    @Test
    void testDeleteTenantNotFound() {
        assertDoesNotThrow(() -> tenantService.delete(UuidUtil.create()));
    }
}
