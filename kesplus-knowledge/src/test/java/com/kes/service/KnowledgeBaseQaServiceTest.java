package com.kes.service;

import com.kes.dto.request.QaRequest;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseQaServiceTest {

    @Mock
    private KnowledgeBaseQaMapper qaMapper;

    @InjectMocks
    private KnowledgeBaseQaService qaService;

    private KnowledgeBaseQa qa;
    private QaRequest request;

    @BeforeEach
    void setUp() {
        qa = new KnowledgeBaseQa();
        qa.setId(1L);
        qa.setUuid(UuidUtil.generate());
        qa.setKbUuid(UuidUtil.generate());
        qa.setQuestion("What is AI?");
        qa.setAnswer("AI is artificial intelligence");
        qa.setPromptTokens(10);
        qa.setAnswerTokens(15);
        qa.setCreatedTime(LocalDateTime.now());

        request = new QaRequest();
        request.setKbUuid(qa.getKbUuid());
        request.setQuestion("What is AI?");
    }

    @Test
    void testAskQuestion() {
        when(qaMapper.insert(any(KnowledgeBaseQa.class))).thenReturn(1);

        QaResponse response = qaService.ask(request);

        assertNotNull(response);
        verify(qaMapper, times(1)).insert(any(KnowledgeBaseQa.class));
    }

    @Test
    void testGetById() {
        when(qaMapper.selectById(eq(1L))).thenReturn(qa);

        QaResponse response = qaService.getById(1L);

        assertNotNull(response);
        assertEquals("What is AI?", response.getQuestion());
    }

    @Test
    void testGetHistory() {
        when(qaMapper.selectList(any())).thenReturn(Arrays.asList(qa));

        List<QaResponse> result = qaService.getHistory(qa.getKbUuid());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("What is AI?", result.get(0).getQuestion());
    }

    @Test
    void testDelete() {
        when(qaMapper.selectById(eq(1L))).thenReturn(qa);
        when(qaMapper.updateById(any(KnowledgeBaseQa.class))).thenReturn(1);

        qaService.delete(1L);

        verify(qaMapper, times(1)).updateById(any(KnowledgeBaseQa.class));
    }

    @Test
    void testDeleteByKbUuid() {
        when(qaMapper.delete(any())).thenReturn(1);

        qaService.deleteByKbUuid(qa.getKbUuid());

        verify(qaMapper, times(1)).delete(any());
    }
}
