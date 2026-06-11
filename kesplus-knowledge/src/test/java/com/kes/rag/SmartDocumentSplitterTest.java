package com.kes.rag;

import dev.langchain4j.data.document.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SmartDocumentSplitterTest {

    @Test
    void testSplitWithSmallContent() {
        SmartDocumentSplitter splitter = new SmartDocumentSplitter();
        
        String content = "Small content";
        Document doc = Document.from(content);
        
        List<Document> chunks = splitter.split(doc);
        
        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertEquals(content, chunks.get(0).text());
    }

    @Test
    void testSplitWithLargeContent() {
        SmartDocumentSplitter splitter = new SmartDocumentSplitter();
        
        String content = "Token ".repeat(1500);
        Document doc = Document.from(content);
        
        List<Document> chunks = splitter.splitWithTokenEstimation(doc, 1000);
        
        assertNotNull(chunks);
        assertTrue(chunks.size() >= 2);
    }

    @Test
    void testSplitStrategies() {
        SmartDocumentSplitter splitter = new SmartDocumentSplitter();
        
        String content = "Content".repeat(50);
        Document doc = Document.from(content);
        
        assertNotNull(splitter.split(doc, SplitStrategy.SEMANTIC));
        assertNotNull(splitter.split(doc, SplitStrategy.STRUCTURAL));
        assertNotNull(splitter.split(doc, SplitStrategy.HYBRID));
    }
}