package com.kes.controller;

import com.kes.dto.request.QaRequest;
import com.kes.dto.response.QaResponse;
import com.kes.entity.KnowledgeBaseQa;
import com.kes.service.KnowledgeBaseQaService;
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

@WebMvcTest(KnowledgeBaseQAController.class)
class KnowledgeBaseQAControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeBaseQaService knowledgeBaseQaService;

    private QaResponse qaResponse;
    private KnowledgeBaseQa qaEntity;

    @BeforeEach
    void setUp() {
        qaResponse = new QaResponse();
        qaResponse.setQuestion("What is AI?");
        qaResponse.setAnswer("AI is artificial intelligence");
        qaResponse.setPromptTokens(10);
        qaResponse.setAnswerTokens(15);

        qaEntity = new KnowledgeBaseQa();
        qaEntity.setId(1L);
        qaEntity.setQuestion("What is AI?");
        qaEntity.setAnswer("AI is artificial intelligence");
        qaEntity.setPromptTokens(10);
        qaEntity.setAnswerTokens(15);
    }

    @Test
    void testAskQuestion() throws Exception {
        QaRequest request = new QaRequest();
        request.setQuestion("What is AI?");

        Mockito.when(knowledgeBaseQaService.qa(
            eq("test-kb-uuid"),
            any(String.class),
            any(Integer.class),
            any(Double.class),
            any(Double.class)
        )).thenReturn(qaResponse);

        mockMvc.perform(post("/api/knowledge-base/{kbUuid}/qa", "test-kb-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.question").value("What is AI?"))
                .andExpect(jsonPath("$.data.answer").value("AI is artificial intelligence"));
    }

    @Test
    void testGetQAHistory() throws Exception {
        List<QaResponse> list = Arrays.asList(qaResponse);
        Mockito.when(knowledgeBaseQaService.getHistory(eq("test-kb-uuid"), eq(10))).thenReturn(list);

        mockMvc.perform(get("/api/knowledge-base/{kbUuid}/qa/history", "test-kb-uuid")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void testGetQAById() throws Exception {
        Mockito.when(knowledgeBaseQaService.getById(eq(1L))).thenReturn(qaEntity);

        mockMvc.perform(get("/api/knowledge-base/{kbUuid}/qa/{id}", "test-kb-uuid", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.question").value("What is AI?"));
    }
}
