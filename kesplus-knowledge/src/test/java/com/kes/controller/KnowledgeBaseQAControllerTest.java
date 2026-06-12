package com.kes.controller;

import com.kes.entity.EmbeddingModel;
import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseQa;
import com.kes.entity.Tenant;
import com.kes.mapper.EmbeddingModelMapper;
import com.kes.mapper.KnowledgeBaseMapper;
import com.kes.mapper.KnowledgeBaseQaMapper;
import com.kes.mapper.TenantMapper;
import com.kes.service.DynamicTableService;
import com.kes.util.UuidUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class KnowledgeBaseQAControllerTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KnowledgeBaseQAControllerTest.class);

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private KnowledgeBaseQaMapper knowledgeBaseQaMapper;

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private EmbeddingModelMapper embeddingModelMapper;

    @Autowired
    private DynamicTableService dynamicTableService;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Value("${rag.embedding.api-key}")
    private String embeddingApiKey;

    @Value("${rag.embedding.model-name}")
    private String embeddingModelName;

    @Value("${rag.embedding.model-type}")
    private String embeddingModelType;

    @Value("${rag.embedding.base-url}")
    private String embeddingBaseUrl;

    private Tenant tenant;
    private KnowledgeBase kb;
    private KnowledgeBaseQa qa;
    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @BeforeEach
    void setUp() throws Exception {
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:postgresql://152.136.30.130:5432/kesplus-knowledge", "root", "123456");
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public");
            stmt.execute("GRANT USAGE ON TYPE public.vector TO root");
            log.info("Vector extension ensured");
        } catch (Exception e) {
            log.warn("Failed to create vector extension, may already exist", e);
        }

        tenant = new Tenant();
        tenant.setUuid(UuidUtil.create());
        tenant.setName("Test Tenant");
        tenant.setCode("TEST");
        tenant.setDescription("Test description");
        tenantMapper.insert(tenant);

        embeddingModel = new EmbeddingModel();
        embeddingModel.setUuid(UuidUtil.create());
        embeddingModel.setModelName(embeddingModelName);
        embeddingModel.setEmbeddingDimension(1024);
        embeddingModel.setModelType(embeddingModelType);
        embeddingModel.setBaseUrl(embeddingBaseUrl);
        embeddingModel.setApiKey(embeddingApiKey);
        embeddingModel.setIsActive(true);
        embeddingModelMapper.insert(embeddingModel);

        kb = new KnowledgeBase();
        kb.setUuid(UuidUtil.create());
        kb.setTitle("Test KB");
        kb.setRemark("Test knowledge base");
        kb.setTenantUuid(tenant.getUuid());
        kb.setEmbeddingModelUuid(embeddingModel.getUuid());
        kb.setEmbeddingDimension(1024);
        knowledgeBaseMapper.insert(kb);

        dynamicTableService.createTableIfNotExists(1024);

        qa = new KnowledgeBaseQa();
        qa.setUuid(UuidUtil.create());
        qa.setKbUuid(kb.getUuid());
        qa.setQuestion("Test question");
        qa.setAnswer("Test answer");
        qa.setPromptTokens(10);
        qa.setAnswerTokens(15);
        qa.setCreatedTime(LocalDateTime.now());
        qa.setUpdatedTime(LocalDateTime.now());
        knowledgeBaseQaMapper.insert(qa);
    }

    @Test
    void testGetQAHistory() throws Exception {
        mockMvc.perform(get("/api/knowledge-base/{kbUuid}/qa/history", kb.getUuid())
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].question").value("Test question"))
                .andExpect(jsonPath("$.data[0].answer").value("Test answer"));
    }

    @Test
    void testGetQAHistoryWithInvalidKB() throws Exception {
        mockMvc.perform(get("/api/knowledge-base/{kbUuid}/qa/history", UuidUtil.create())
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testSaveQA() throws Exception {
        String json = "{\"question\":\"New question\",\"maxResults\":5,\"minScore\":0.6,\"temperature\":0.7}";
        mockMvc.perform(post("/api/knowledge-base/{kbUuid}/qa", kb.getUuid())
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testSaveQAWithInvalidKB() throws Exception {
        String json = "{\"question\":\"Question\",\"maxResults\":5,\"minScore\":0.6,\"temperature\":0.7}";
        mockMvc.perform(post("/api/knowledge-base/{kbUuid}/qa", UuidUtil.create())
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testGetQACount() throws Exception {
        mockMvc.perform(get("/api/knowledge-base/{kbUuid}/qa/count", kb.getUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber())
                .andExpect(jsonPath("$.data").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testGetQACountWithInvalidKB() throws Exception {
        mockMvc.perform(get("/api/knowledge-base/{kbUuid}/qa/count", UuidUtil.create())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }
}