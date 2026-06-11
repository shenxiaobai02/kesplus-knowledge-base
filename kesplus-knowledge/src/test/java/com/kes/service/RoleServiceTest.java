package com.kes.service;

import com.kes.entity.Role;
import com.kes.entity.UserRole;
import com.kes.mapper.RoleMapper;
import com.kes.mapper.UserRoleMapper;
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
class RoleServiceTest {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @InjectMocks
    private RoleService roleService;

    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(1L);
        role.setUuid(UuidUtil.create());
        role.setName("Test Role");
        role.setCode("TEST_ROLE");
        role.setDescription("Test description");
        role.setTenantUuid(UuidUtil.create());
        role.setCreatedTime(LocalDateTime.now());
        role.setUpdatedTime(LocalDateTime.now());
    }

    @Test
    void testCreateRole() {
        when(roleMapper.insert(any(Role.class))).thenReturn(1);

        Role result = roleService.create(role);

        assertNotNull(result);
        assertEquals("Test Role", result.getName());
        assertEquals("TEST_ROLE", result.getCode());
        verify(roleMapper, times(1)).insert(any(Role.class));
    }

    @Test
    void testGetById() {
        when(roleMapper.selectById(eq(1L))).thenReturn(role);

        Role result = roleService.getById(1L);

        assertNotNull(result);
        assertEquals("Test Role", result.getName());
    }

    @Test
    void testGetByUuid() {
        when(roleMapper.selectByUuid(eq(role.getUuid()))).thenReturn(role);

        Role result = roleService.getByUuid(role.getUuid());

        assertNotNull(result);
        assertEquals(role.getUuid(), result.getUuid());
    }

    @Test
    void testListByTenant() {
        when(roleMapper.selectByTenantUuid(eq(role.getTenantUuid()))).thenReturn(Arrays.asList(role));

        List<Role> result = roleService.listByTenant(role.getTenantUuid());

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateRole() {
        when(roleMapper.selectByUuid(eq(role.getUuid()))).thenReturn(role);
        when(roleMapper.updateById(any(Role.class))).thenReturn(1);

        role.setName("Updated Role");
        Role result = roleService.update(role);

        assertNotNull(result);
        assertEquals("Updated Role", result.getName());
        verify(roleMapper, times(1)).updateById(any(Role.class));
    }

    @Test
    void testDeleteRole() {
        when(roleMapper.selectByUuid(eq(role.getUuid()))).thenReturn(role);
        when(roleMapper.updateById(any(Role.class))).thenReturn(1);

        roleService.delete(role.getUuid());

        verify(roleMapper, times(1)).updateById(any(Role.class));
    }

    @Test
    void testAssignRole() {
        when(roleMapper.selectByUuid(eq(role.getUuid()))).thenReturn(role);
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

        roleService.assignRole(1L, role.getUuid(), role.getTenantUuid());

        verify(userRoleMapper, times(1)).insert(any(UserRole.class));
    }

    @Test
    void testRevokeRole() {
        doNothing().when(userRoleMapper).deleteByUserIdAndRoleUuid(eq(1L), eq(role.getUuid()));

        roleService.revokeRole(1L, role.getUuid(), role.getTenantUuid());

        verify(userRoleMapper, times(1)).deleteByUserIdAndRoleUuid(eq(1L), eq(role.getUuid()));
    }

    @Test
    void testGetUserRoles() {
        when(userRoleMapper.selectRoleUuidsByUserId(eq(1L))).thenReturn(Arrays.asList(role.getUuid()));
        when(roleMapper.selectByUuid(eq(role.getUuid()))).thenReturn(role);

        List<Role> result = roleService.getUserRoles(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Role", result.get(0).getName());
    }
}
