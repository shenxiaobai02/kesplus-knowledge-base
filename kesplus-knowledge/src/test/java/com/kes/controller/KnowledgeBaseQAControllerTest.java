package com.kes.controller;

import com.kes.dto.request.QaRequest;
import com.kes.dto.response.QaResponse;
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

    @BeforeEach
    void setUp() {
        qaResponse = new QaResponse();
        qaResponse.setQuestion("What is AI?");
        qaResponse.setAnswer("AI is artificial intelligence");
        qaResponse.setPromptTokens(10);
        qaResponse.setAnswerTokens(15);
    }

    @Test
    void testAskQuestion() throws Exception {
        QaRequest request = new QaRequest();
        request.setKbUuid("test-kb-uuid");
        request.setQuestion("What is AI?");

        Mockito.when(knowledgeBaseQaService.ask(any(QaRequest.class))).thenReturn(qaResponse);

        mockMvc.perform(post("/qa/ask")
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
        Mockito.when(knowledgeBaseQaService.getHistory(eq("test-kb-uuid"))).thenReturn(list);

        mockMvc.perform(get("/qa/history/{kbUuid}", "test-kb-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void testGetQAById() throws Exception {
        Mockito.when(knowledgeBaseQaService.getById(eq(1L))).thenReturn(qaResponse);

        mockMvc.perform(get("/qa/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.question").value("What is AI?"));
    }

    @Test
    void testDeleteQA() throws Exception {
        Mockito.doNothing().when(knowledgeBaseQaService).delete(eq(1L));

        mockMvc.perform(delete("/qa/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
