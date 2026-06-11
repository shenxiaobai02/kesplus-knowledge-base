package com.kes.service;

import com.kes.dto.response.QaResponse;
import com.kes.entity.KnowledgeBaseQa;
import com.kes.mapper.KnowledgeBaseQaMapper;
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
class KnowledgeBaseQaServiceTest {

    @Mock
    private KnowledgeBaseQaMapper qaMapper;

    @InjectMocks
    private KnowledgeBaseQaService qaService;

    private KnowledgeBaseQa qa;

    @BeforeEach
    void setUp() {
        qa = new KnowledgeBaseQa();
        qa.setId(1L);
        qa.setUuid(UuidUtil.create());
        qa.setKbUuid(UuidUtil.create());
        qa.setQuestion("What is AI?");
        qa.setAnswer("AI is artificial intelligence");
        qa.setPromptTokens(10);
        qa.setAnswerTokens(15);
        qa.setCreatedTime(LocalDateTime.now());
    }

    @Test
    void testSaveQaRecord() {
        when(qaMapper.insert(any(KnowledgeBaseQa.class))).thenReturn(1);

        KnowledgeBaseQa result = qaService.saveQaRecord(
            qa.getKbUuid(),
            "What is AI?",
            "AI is artificial intelligence",
            10,
            15
        );

        assertNotNull(result);
        assertEquals("What is AI?", result.getQuestion());
        verify(qaMapper, times(1)).insert(any(KnowledgeBaseQa.class));
    }

    @Test
    void testGetHistory() {
        when(qaMapper.selectByKbUuid(eq(qa.getKbUuid()))).thenReturn(Arrays.asList(qa));

        List<QaResponse> result = qaService.getHistory(qa.getKbUuid(), 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("What is AI?", result.get(0).getQuestion());
    }

    @Test
    void testGetById() {
        when(qaMapper.selectById(eq(1L))).thenReturn(qa);

        KnowledgeBaseQa result = qaService.getById(1L);

        assertNotNull(result);
        assertEquals("What is AI?", result.getQuestion());
    }
}
