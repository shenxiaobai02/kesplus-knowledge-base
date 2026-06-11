package com.kes.controller;

import com.kes.dto.request.RoleCreateRequest;
import com.kes.dto.request.RoleUpdateRequest;
import com.kes.entity.Role;
import com.kes.service.RoleService;
import com.kes.util.UuidUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
    }

    @Test
    void testCreateRole() throws Exception {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setName("Test Role");
        request.setCode("TEST_ROLE");
        request.setDescription("Test description");
        request.setTenantUuid(UuidUtil.create());

        Mockito.when(roleService.create(any(Role.class))).thenReturn(role);

        mockMvc.perform(post("/api/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Role"));
    }

    @Test
    void testGetRoleById() throws Exception {
        Mockito.when(roleService.getById(eq(1L))).thenReturn(role);

        mockMvc.perform(get("/api/role/{uuid}", role.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Role"));
    }

    @Test
    void testGetRoleByUuid() throws Exception {
        Mockito.when(roleService.getByUuid(eq(role.getUuid()))).thenReturn(role);

        mockMvc.perform(get("/api/role/{uuid}", role.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Role"));
    }

    @Test
    void testGetAllRoles() throws Exception {
        List<Role> list = Arrays.asList(role);
        Mockito.when(roleService.listByTenant(eq("test-tenant-uuid"))).thenReturn(list);

        mockMvc.perform(get("/api/role")
                        .param("tenantUuid", "test-tenant-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void testUpdateRole() throws Exception {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setName("Updated Role");
        request.setDescription("Updated description");

        role.setName("Updated Role");
        Mockito.when(roleService.update(any(Role.class))).thenReturn(role);

        mockMvc.perform(put("/api/role/{uuid}", role.getUuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Updated Role"));
    }

    @Test
    void testDeleteRole() throws Exception {
        Mockito.doNothing().when(roleService).delete(eq(role.getUuid()));

        mockMvc.perform(delete("/api/role/{uuid}", role.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testAssignRoleToUser() throws Exception {
        Mockito.doNothing().when(roleService).assignRole(eq(1L), eq(role.getUuid()), eq("test-tenant-uuid"));

        mockMvc.perform(post("/api/role/{roleUuid}/assign", role.getUuid())
                        .param("userId", "1")
                        .param("tenantUuid", "test-tenant-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testRemoveRoleFromUser() throws Exception {
        Mockito.doNothing().when(roleService).revokeRole(eq(1L), eq(role.getUuid()), eq("test-tenant-uuid"));

        mockMvc.perform(post("/api/role/{roleUuid}/revoke", role.getUuid())
                        .param("userId", "1")
                        .param("tenantUuid", "test-tenant-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
