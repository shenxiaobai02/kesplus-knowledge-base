package com.kes.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SmartDocumentSplitter {

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_CHUNK_OVERLAP = 100;

    public List<Document> split(Document document, SplitStrategy strategy, int chunkSize, int chunkOverlap) {
        DocumentSplitter splitter = createSplitter(strategy, chunkSize, chunkOverlap);
        List<TextSegment> segments = splitter.split(document);
        return segments.stream()
                .map(s -> Document.from(s.text(), s.metadata()))
                .collect(Collectors.toList());
    }

    public List<Document> split(Document document, SplitStrategy strategy) {
        return split(document, strategy, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    public List<Document> split(Document document) {
        return split(document, SplitStrategy.HYBRID, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    private DocumentSplitter createSplitter(SplitStrategy strategy, int chunkSize, int chunkOverlap) {
        return switch (strategy) {
            case SEMANTIC -> DocumentSplitters.recursive(chunkSize, chunkOverlap);
            case STRUCTURAL -> DocumentSplitters.recursive(chunkSize, chunkOverlap);
            case HYBRID -> DocumentSplitters.recursive(chunkSize, chunkOverlap);
        };
    }

    public List<Document> splitWithTokenEstimation(Document document, int maxTokens) {
        List<TextSegment> segments = DocumentSplitters.recursive(maxTokens, maxTokens / 10).split(document);
        return segments.stream()
                .map(s -> Document.from(s.text(), s.metadata()))
                .collect(Collectors.toList());
    }
}