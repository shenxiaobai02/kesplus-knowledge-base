package com.kes.controller;

import com.kes.entity.KnowledgeBase;
import com.kes.entity.Tenant;
import com.kes.mapper.KnowledgeBaseMapper;
import com.kes.mapper.TenantMapper;
import com.kes.util.ThreadContext;
import com.kes.util.UuidUtil;
import com.kes.dto.request.KnowledgeBaseCreateRequest;
import com.kes.dto.request.KnowledgeBaseUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class KnowledgeBaseControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private Tenant tenant;
    private KnowledgeBase kb;

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

        ThreadContext.UserContext userContext = new ThreadContext.UserContext();
        userContext.setUserId(1L);
        userContext.setUsername("test-user");
        userContext.setTenantUuid(tenant.getUuid());
        ThreadContext.setUserContext(userContext);
        ThreadContext.setRequestId(UuidUtil.create());
        ThreadContext.setClientIp("127.0.0.1");

        kb = new KnowledgeBase();
        kb.setUuid(UuidUtil.create());
        kb.setTitle("Test KB");
        kb.setRemark("Test knowledge base");
        kb.setTenantUuid(tenant.getUuid());
        kb.setOwnerId(1L);
        knowledgeBaseMapper.insert(kb);
    }

    @AfterEach
    void tearDown() {
        ThreadContext.clear();
    }

    @Test
    void testGetKnowledgeBase() throws Exception {
        mockMvc.perform(get("/api/knowledge-base/{uuid}", kb.getUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(kb.getUuid()))
                .andExpect(jsonPath("$.data.title").value("Test KB"));
    }

    @Test
    void testGetKnowledgeBaseNotFound() throws Exception {
        mockMvc.perform(get("/api/knowledge-base/{uuid}", UuidUtil.create())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void testListKnowledgeBases() throws Exception {
        mockMvc.perform(get("/api/knowledge-base")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testCreateKnowledgeBase() throws Exception {
        KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest();
        request.setTitle("New KB");
        request.setRemark("New knowledge base");

        mockMvc.perform(post("/api/knowledge-base")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("New KB"))
                .andExpect(jsonPath("$.data.remark").value("New knowledge base"));
    }

    @Test
    void testUpdateKnowledgeBase() throws Exception {
        KnowledgeBaseUpdateRequest request = new KnowledgeBaseUpdateRequest();
        request.setTitle("Updated KB");
        request.setRemark("Updated description");

        mockMvc.perform(put("/api/knowledge-base/{uuid}", kb.getUuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated KB"))
                .andExpect(jsonPath("$.data.remark").value("Updated description"));
    }

    @Test
    void testUpdateKnowledgeBaseNotFound() throws Exception {
        KnowledgeBaseUpdateRequest request = new KnowledgeBaseUpdateRequest();
        request.setTitle("Updated KB");

        mockMvc.perform(put("/api/knowledge-base/{uuid}", UuidUtil.create())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testDeleteKnowledgeBase() throws Exception {
        mockMvc.perform(delete("/api/knowledge-base/{uuid}", kb.getUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteKnowledgeBaseNotFound() throws Exception {
        mockMvc.perform(delete("/api/knowledge-base/{uuid}", UuidUtil.create())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testGetKnowledgeBasesByTenant() throws Exception {
        mockMvc.perform(get("/api/knowledge-base/tenant/{tenantUuid}", tenant.getUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }
}