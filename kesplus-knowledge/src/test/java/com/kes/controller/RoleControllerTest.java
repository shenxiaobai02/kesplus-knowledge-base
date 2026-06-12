package com.kes.controller;

import com.kes.entity.Role;
import com.kes.entity.Tenant;
import com.kes.mapper.RoleMapper;
import com.kes.mapper.TenantMapper;
import com.kes.service.RoleService;
import com.kes.util.UuidUtil;
import com.kes.dto.request.RoleCreateRequest;
import com.kes.dto.request.RoleUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class RoleControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private Tenant tenant;
    private Role role;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setUuid(UuidUtil.create());
        tenant.setName("Test Tenant");
        tenant.setCode("TEST");
        tenant.setDescription("Test description");
        tenantMapper.insert(tenant);

        role = new Role();
        role.setUuid(UuidUtil.create());
        role.setName("Admin Role");
        role.setCode("ADMIN");
        role.setDescription("Administrator role");
        role.setTenantUuid(tenant.getUuid());
        roleMapper.insert(role);
    }

    @Test
    void testGetRole() throws Exception {
        mockMvc.perform(get("/api/role/{uuid}", role.getUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(role.getUuid()))
                .andExpect(jsonPath("$.data.name").value("Admin Role"))
                .andExpect(jsonPath("$.data.code").value("ADMIN"));
    }

    @Test
    void testGetRoleNotFound() throws Exception {
        mockMvc.perform(get("/api/role/{uuid}", UuidUtil.create())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void testListRoles() throws Exception {
        mockMvc.perform(get("/api/role")
                        .param("tenantUuid", tenant.getUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].name").value("Admin Role"));
    }

    @Test
    void testCreateRole() throws Exception {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setName("New Role");
        request.setCode("NEW_ROLE");
        request.setDescription("New role description");
        request.setTenantUuid(tenant.getUuid());

        mockMvc.perform(post("/api/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Role"))
                .andExpect(jsonPath("$.data.code").value("NEW_ROLE"))
                .andExpect(jsonPath("$.data.description").value("New role description"));
    }

    @Test
    void testCreateRoleDuplicateCode() throws Exception {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setName("Duplicate Role");
        request.setCode("ADMIN");
        request.setDescription("Duplicate code");
        request.setTenantUuid(tenant.getUuid());

        mockMvc.perform(post("/api/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testUpdateRole() throws Exception {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setName("Updated Role");
        request.setDescription("Updated description");

        mockMvc.perform(put("/api/role/{uuid}", role.getUuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Role"))
                .andExpect(jsonPath("$.data.description").value("Updated description"));
    }

    @Test
    void testUpdateRoleNotFound() throws Exception {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setName("Updated Role");

        mockMvc.perform(put("/api/role/{uuid}", UuidUtil.create())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testDeleteRole() throws Exception {
        mockMvc.perform(delete("/api/role/{uuid}", role.getUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteRoleNotFound() throws Exception {
        mockMvc.perform(delete("/api/role/{uuid}", UuidUtil.create())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testAssignRole() throws Exception {
        mockMvc.perform(post("/api/role/{uuid}/assign", role.getUuid())
                        .param("userId", "1")
                        .param("tenantUuid", tenant.getUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testGetRolesByTenant() throws Exception {
        mockMvc.perform(get("/api/role")
                        .param("tenantUuid", tenant.getUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].tenantUuid").value(tenant.getUuid()));
    }
}