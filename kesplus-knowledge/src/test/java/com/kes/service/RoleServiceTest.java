package com.kes.service;

import com.kes.entity.Role;
import com.kes.entity.UserRole;
import com.kes.mapper.UserRoleMapper;
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
class RoleServiceTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleMapper userRoleMapper;

    private Role role;
    private String tenantUuid;

    @BeforeEach
    void setUp() {
        tenantUuid = UuidUtil.create();
        
        role = new Role();
        role.setName("Test Role");
        role.setCode("TEST_ROLE");
        role.setDescription("Test description");
        role.setTenantUuid(tenantUuid);
    }

    @Test
    void testCreateRole() {
        Role result = roleService.create(role);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getUuid());
        assertEquals("Test Role", result.getName());
        assertEquals("TEST_ROLE", result.getCode());
        assertFalse(result.getIsDeleted());
    }

    @Test
    void testGetById() {
        Role created = roleService.create(role);

        Role result = roleService.getById(created.getId());

        assertNotNull(result);
        assertEquals(created.getId(), result.getId());
        assertEquals("Test Role", result.getName());
    }

    @Test
    void testGetByUuid() {
        Role created = roleService.create(role);

        Role result = roleService.getByUuid(created.getUuid());

        assertNotNull(result);
        assertEquals(created.getUuid(), result.getUuid());
        assertEquals("Test Role", result.getName());
    }

    @Test
    void testListByTenant() {
        roleService.create(role);

        Role role2 = new Role();
        role2.setName("Test Role 2");
        role2.setCode("TEST_ROLE_2");
        role2.setDescription("Test description 2");
        role2.setTenantUuid(tenantUuid);
        roleService.create(role2);

        List<Role> result = roleService.listByTenant(tenantUuid);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testUpdateRole() {
        Role created = roleService.create(role);

        created.setName("Updated Role");
        created.setDescription("Updated description");
        Role result = roleService.update(created);

        assertNotNull(result);
        assertEquals("Updated Role", result.getName());
        assertEquals("Updated description", result.getDescription());
    }

    @Test
    void testUpdateRoleNotFound() {
        role.setUuid(UuidUtil.create());

        assertThrows(RuntimeException.class, () -> roleService.update(role));
    }

    @Test
    void testDeleteRole() {
        Role created = roleService.create(role);
        String uuid = created.getUuid();

        roleService.delete(uuid);

        Role result = roleService.getByUuid(uuid);
        assertTrue(result.getIsDeleted());
    }

    @Test
    void testDeleteRoleNotFound() {
        assertDoesNotThrow(() -> roleService.delete(UuidUtil.create()));
    }

    @Test
    void testAssignRole() {
        Role created = roleService.create(role);

        roleService.assignRole(1L, created.getUuid(), tenantUuid);

        List<String> roleUuids = userRoleMapper.selectRoleUuidsByUserId(1L);
        assertFalse(roleUuids.isEmpty());
        assertEquals(created.getUuid(), roleUuids.get(0));
    }

    @Test
    void testAssignRoleAlreadyExists() {
        Role created = roleService.create(role);

        roleService.assignRole(1L, created.getUuid(), tenantUuid);
        roleService.assignRole(1L, created.getUuid(), tenantUuid);

        List<String> roleUuids = userRoleMapper.selectRoleUuidsByUserId(1L);
        assertEquals(1, roleUuids.size());
    }

    @Test
    void testRevokeRole() {
        Role created = roleService.create(role);
        roleService.assignRole(1L, created.getUuid(), tenantUuid);

        roleService.revokeRole(1L, created.getUuid(), tenantUuid);

        List<String> roleUuids = userRoleMapper.selectRoleUuidsByUserId(1L);
        assertTrue(roleUuids.isEmpty());
    }

    @Test
    void testGetUserRoles() {
        Role created = roleService.create(role);
        roleService.assignRole(1L, created.getUuid(), tenantUuid);

        List<Role> result = roleService.getUserRoles(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Role", result.get(0).getName());
    }

    @Test
    void testGetUserRolesEmpty() {
        List<Role> result = roleService.getUserRoles(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}