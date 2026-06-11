package com.kes.controller;

import com.kes.dto.request.KnowledgeBaseCreateRequest;
import com.kes.dto.request.KnowledgeBaseUpdateRequest;
import com.kes.entity.KnowledgeBase;
import com.kes.service.KnowledgeBaseService;
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

@WebMvcTest(KnowledgeBaseController.class)
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeBaseService knowledgeBaseService;

    private KnowledgeBase kb;

    @BeforeEach
    void setUp() {
        kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setUuid(UuidUtil.generate());
        kb.setTitle("Test KB");
        kb.setRemark("Test remark");
        kb.setIsPublic(false);
        kb.setIsStrict(false);
    }

    @Test
    void testCreateKnowledgeBase() throws Exception {
        KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest();
        request.setTitle("Test KB");
        request.setRemark("Test remark");
        request.setIsPublic(false);

        Mockito.when(knowledgeBaseService.create(any(KnowledgeBaseCreateRequest.class))).thenReturn(kb);

        mockMvc.perform(post("/knowledge-base")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Test KB"));
    }

    @Test
    void testGetKnowledgeBaseById() throws Exception {
        Mockito.when(knowledgeBaseService.findById(eq(1L))).thenReturn(kb);

        mockMvc.perform(get("/knowledge-base/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Test KB"));
    }

    @Test
    void testGetKnowledgeBaseByUuid() throws Exception {
        Mockito.when(knowledgeBaseService.findByUuid(eq(kb.getUuid()))).thenReturn(kb);

        mockMvc.perform(get("/knowledge-base/uuid/{uuid}", kb.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Test KB"));
    }

    @Test
    void testGetAllKnowledgeBases() throws Exception {
        List<KnowledgeBase> list = Arrays.asList(kb);
        Mockito.when(knowledgeBaseService.findAll()).thenReturn(list);

        mockMvc.perform(get("/knowledge-base"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void testUpdateKnowledgeBase() throws Exception {
        KnowledgeBaseUpdateRequest request = new KnowledgeBaseUpdateRequest();
        request.setTitle("Updated KB");
        request.setRemark("Updated remark");

        kb.setTitle("Updated KB");
        Mockito.when(knowledgeBaseService.update(eq(kb.getUuid()), any(KnowledgeBaseUpdateRequest.class))).thenReturn(kb);

        mockMvc.perform(put("/knowledge-base/{uuid}", kb.getUuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Updated KB"));
    }

    @Test
    void testDeleteKnowledgeBase() throws Exception {
        Mockito.doNothing().when(knowledgeBaseService).delete(eq(kb.getUuid()));

        mockMvc.perform(delete("/knowledge-base/{uuid}", kb.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSearchKnowledgeBases() throws Exception {
        List<KnowledgeBase> list = Arrays.asList(kb);
        Mockito.when(knowledgeBaseService.search(eq("Test"), eq(null))).thenReturn(list);

        mockMvc.perform(get("/knowledge-base/search")
                        .param("keyword", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
