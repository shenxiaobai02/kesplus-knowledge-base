package com.kes.dto.response;

import com.kes.entity.KnowledgeBaseQa;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QaResponse {

    private String uuid;
    private String kbUuid;
    private String question;
    private String answer;
    private Integer promptTokens;
    private Integer answerTokens;
    private List<ReferenceDocument> references;
    private LocalDateTime createdTime;

    @Data
    public static class ReferenceDocument {
        private String text;
        private Double score;
        private String metadataJson;
    }

    public static QaResponse fromEntity(KnowledgeBaseQa qa) {
        QaResponse response = new QaResponse();
        response.setUuid(qa.getUuid());
        response.setKbUuid(qa.getKbUuid());
        response.setQuestion(qa.getQuestion());
        response.setAnswer(qa.getAnswer());
        response.setPromptTokens(qa.getPromptTokens());
        response.setAnswerTokens(qa.getAnswerTokens());
        response.setCreatedTime(qa.getCreatedTime());
        return response;
    }
}