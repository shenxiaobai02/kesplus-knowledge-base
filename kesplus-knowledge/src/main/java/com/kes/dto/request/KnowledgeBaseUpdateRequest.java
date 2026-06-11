package com.kes.dto.request;

import lombok.Data;

@Data
public class KnowledgeBaseUpdateRequest {

    private String title;

    private String remark;

    private Boolean isPublic;

    private Boolean isStrict;

    private Integer retrieveMaxResults;

    private Double retrieveMinScore;

    private Double queryLlmTemperature;

    private String querySystemMessage;
}