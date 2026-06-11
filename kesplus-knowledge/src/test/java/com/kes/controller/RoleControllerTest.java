package com.kes.controller;

import com.kes.dto.request.RoleCreateRequest;
import com.kes.dto.request.RoleUpdateRequest;
import com.kes.dto.request.UserRoleAssignRequest;
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
        role.setUuid(UuidUtil.generate());
        role.setName("Test Role");
        role.setCode("TEST_ROLE");
        role.setDescription("Test description");
        role.setTenantUuid(UuidUtil.generate());
    }

    @Test
    void testCreateRole() throws Exception {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setName("Test Role");
        request.setCode("TEST_ROLE");
        request.setDescription("Test description");
        request.setTenantUuid(UuidUtil.generate());

        Mockito.when(roleService.create(any(RoleCreateRequest.class))).thenReturn(role);

        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Role"));
    }

    @Test
    void testGetRoleById() throws Exception {
        Mockito.when(roleService.findById(eq(1L))).thenReturn(role);

        mockMvc.perform(get("/role/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Role"));
    }

    @Test
    void testGetRoleByUuid() throws Exception {
        Mockito.when(roleService.findByUuid(eq(role.getUuid()))).thenReturn(role);

        mockMvc.perform(get("/role/uuid/{uuid}", role.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Role"));
    }

    @Test
    void testGetAllRoles() throws Exception {
        List<Role> list = Arrays.asList(role);
        Mockito.when(roleService.findAll()).thenReturn(list);

        mockMvc.perform(get("/role"))
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
        Mockito.when(roleService.update(eq(role.getUuid()), any(RoleUpdateRequest.class))).thenReturn(role);

        mockMvc.perform(put("/role/{uuid}", role.getUuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Updated Role"));
    }

    @Test
    void testDeleteRole() throws Exception {
        Mockito.doNothing().when(roleService).delete(eq(role.getUuid()));

        mockMvc.perform(delete("/role/{uuid}", role.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testAssignRoleToUser() throws Exception {
        UserRoleAssignRequest request = new UserRoleAssignRequest();
        request.setUserId(1L);
        request.setRoleUuid(role.getUuid());

        mockMvc.perform(post("/role/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testRemoveRoleFromUser() throws Exception {
        mockMvc.perform(delete("/role/unassign")
                        .param("userId", "1")
                        .param("roleUuid", role.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
