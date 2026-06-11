package com.kes.service;

import com.kes.dto.request.RoleCreateRequest;
import com.kes.dto.request.RoleUpdateRequest;
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
    private RoleCreateRequest createRequest;
    private RoleUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(1L);
        role.setUuid(UuidUtil.generate());
        role.setName("Test Role");
        role.setCode("TEST_ROLE");
        role.setDescription("Test description");
        role.setTenantUuid(UuidUtil.generate());
        role.setCreatedTime(LocalDateTime.now());
        role.setUpdatedTime(LocalDateTime.now());

        createRequest = new RoleCreateRequest();
        createRequest.setName("Test Role");
        createRequest.setCode("TEST_ROLE");
        createRequest.setDescription("Test description");
        createRequest.setTenantUuid(UuidUtil.generate());

        updateRequest = new RoleUpdateRequest();
        updateRequest.setName("Updated Role");
        updateRequest.setDescription("Updated description");
    }

    @Test
    void testCreateRole() {
        when(roleMapper.insert(any(Role.class))).thenReturn(1);
        when(roleMapper.selectById(any(Long.class))).thenReturn(role);

        Role result = roleService.create(createRequest);

        assertNotNull(result);
        assertEquals("Test Role", result.getName());
        assertEquals("TEST_ROLE", result.getCode());
        verify(roleMapper, times(1)).insert(any(Role.class));
    }

    @Test
    void testFindById() {
        when(roleMapper.selectById(eq(1L))).thenReturn(role);

        Role result = roleService.findById(1L);

        assertNotNull(result);
        assertEquals("Test Role", result.getName());
    }

    @Test
    void testFindByUuid() {
        when(roleMapper.selectOne(any())).thenReturn(role);

        Role result = roleService.findByUuid(role.getUuid());

        assertNotNull(result);
        assertEquals(role.getUuid(), result.getUuid());
    }

    @Test
    void testFindAll() {
        when(roleMapper.selectList(any())).thenReturn(Arrays.asList(role));

        List<Role> result = roleService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateRole() {
        when(roleMapper.selectOne(any())).thenReturn(tenant);
        when(roleMapper.updateById(any(Role.class))).thenReturn(1);

        Role result = roleService.update(role.getUuid(), updateRequest);

        assertNotNull(result);
        assertEquals("Updated Role", result.getName());
        verify(roleMapper, times(1)).updateById(any(Role.class));
    }

    @Test
    void testDeleteRole() {
        when(roleMapper.selectOne(any())).thenReturn(role);
        when(roleMapper.updateById(any(Role.class))).thenReturn(1);

        roleService.delete(role.getUuid());

        verify(roleMapper, times(1)).updateById(any(Role.class));
    }

    @Test
    void testAssignRoleToUser() {
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

        roleService.assignRoleToUser(1L, role.getUuid());

        verify(userRoleMapper, times(1)).insert(any(UserRole.class));
    }

    @Test
    void testRemoveRoleFromUser() {
        when(userRoleMapper.delete(any())).thenReturn(1);

        roleService.removeRoleFromUser(1L, role.getUuid());

        verify(userRoleMapper, times(1)).delete(any());
    }
}
