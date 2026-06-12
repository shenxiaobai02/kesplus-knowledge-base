package com.kes.service;

import com.kes.dto.response.QaResponse;
import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseQa;
import com.kes.mapper.KnowledgeBaseMapper;
import com.kes.util.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class KnowledgeBaseQaServiceTest {

    @Autowired
    private KnowledgeBaseQaService qaService;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    private KnowledgeBase kb;
    private String kbUuid;

    @BeforeEach
    void setUp() {
        kb = new KnowledgeBase();
        kb.setTitle("Test KB");
        kb.setRemark("Test remark");
        kb.setIsPublic(false);
        kb.setIsStrict(false);
        kb.setEmbeddingDimension(1024);
        kb.setIsDeleted(false);
        kb.setUuid(UuidUtil.create());
        
        knowledgeBaseMapper.insert(kb);
        kbUuid = kb.getUuid();
    }

    @Test
    void testSaveQaRecord() {
        KnowledgeBaseQa result = qaService.saveQaRecord(
            kbUuid,
            "What is AI?",
            "AI is artificial intelligence",
            10,
            15
        );

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getUuid());
        assertEquals("What is AI?", result.getQuestion());
        assertEquals("AI is artificial intelligence", result.getAnswer());
        assertEquals(10, result.getPromptTokens());
        assertEquals(15, result.getAnswerTokens());
        assertFalse(result.getIsDeleted());
    }

    @Test
    void testGetHistory() {
        qaService.saveQaRecord(kbUuid, "Question 1", "Answer 1", 10, 15);
        qaService.saveQaRecord(kbUuid, "Question 2", "Answer 2", 12, 18);
        qaService.saveQaRecord(kbUuid, "Question 3", "Answer 3", 8, 12);

        List<QaResponse> result = qaService.getHistory(kbUuid, 10);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Question 3", result.get(0).getQuestion());
        assertEquals("Answer 3", result.get(0).getAnswer());
        assertEquals("Question 1", result.get(2).getQuestion());
        assertEquals("Answer 1", result.get(2).getAnswer());
    }

    @Test
    void testGetHistoryWithLimit() {
        qaService.saveQaRecord(kbUuid, "Question 1", "Answer 1", 10, 15);
        qaService.saveQaRecord(kbUuid, "Question 2", "Answer 2", 12, 18);
        qaService.saveQaRecord(kbUuid, "Question 3", "Answer 3", 8, 12);

        List<QaResponse> result = qaService.getHistory(kbUuid, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetHistoryEmpty() {
        List<QaResponse> result = qaService.getHistory(kbUuid, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetById() {
        KnowledgeBaseQa saved = qaService.saveQaRecord(
            kbUuid,
            "What is AI?",
            "AI is artificial intelligence",
            10,
            15
        );

        KnowledgeBaseQa result = qaService.getById(saved.getId());

        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
        assertEquals("What is AI?", result.getQuestion());
        assertEquals("AI is artificial intelligence", result.getAnswer());
    }

    @Test
    void testGetByIdNotFound() {
        KnowledgeBaseQa result = qaService.getById(999L);

        assertNull(result);
    }
}