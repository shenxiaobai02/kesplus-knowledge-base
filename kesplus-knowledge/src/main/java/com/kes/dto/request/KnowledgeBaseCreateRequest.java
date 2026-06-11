package com.kes.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeBaseCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String remark;

    private Boolean isPublic = false;

    private Boolean isStrict = false;

    private String embeddingModelUuid;

    private Integer retrieveMaxResults;

    private Double retrieveMinScore;

    private Double queryLlmTemperature;

    private String querySystemMessage;
}