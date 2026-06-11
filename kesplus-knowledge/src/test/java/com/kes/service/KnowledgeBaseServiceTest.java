package com.kes.service;

import com.kes.entity.KnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeBaseServiceTest {

    private KnowledgeBase kb;

    @BeforeEach
    void setUp() {
        kb = new KnowledgeBase();
        kb.setTitle("Test KB");
        kb.setRemark("Test remark");
        kb.setIsPublic(false);
        kb.setIsStrict(false);
    }

    @Test
    void testKnowledgeBaseCreation() {
        assertNotNull(kb);
        assertEquals("Test KB", kb.getTitle());
        assertEquals("Test remark", kb.getRemark());
        assertFalse(kb.getIsPublic());
        assertFalse(kb.getIsStrict());
    }

    @Test
    void testKnowledgeBaseFields() {
        kb.setUuid("test-uuid");
        kb.setEmbeddingDimension(1024);
        kb.setRetrieveMaxResults(5);
        kb.setRetrieveMinScore(0.6);

        assertEquals("test-uuid", kb.getUuid());
        assertEquals(1024, kb.getEmbeddingDimension());
        assertEquals(5, kb.getRetrieveMaxResults());
        assertEquals(0.6, kb.getRetrieveMinScore());
    }
}