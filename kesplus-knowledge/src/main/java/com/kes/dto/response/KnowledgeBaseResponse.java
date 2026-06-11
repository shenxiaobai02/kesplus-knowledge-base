package com.kes.dto.response;

import com.kes.entity.KnowledgeBase;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseResponse {

    private Long id;
    private String uuid;
    private String title;
    private String remark;
    private Boolean isPublic;
    private Boolean isStrict;
    private Integer itemCount;
    private Integer embeddingCount;
    private String embeddingModelUuid;
    private Integer embeddingDimension;
    private Integer retrieveMaxResults;
    private Double retrieveMinScore;
    private Double queryLlmTemperature;
    private String querySystemMessage;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static KnowledgeBaseResponse fromEntity(KnowledgeBase kb) {
        KnowledgeBaseResponse response = new KnowledgeBaseResponse();
        response.setId(kb.getId());
        response.setUuid(kb.getUuid());
        response.setTitle(kb.getTitle());
        response.setRemark(kb.getRemark());
        response.setIsPublic(kb.getIsPublic());
        response.setIsStrict(kb.getIsStrict());
        response.setItemCount(kb.getItemCount());
        response.setEmbeddingCount(kb.getEmbeddingCount());
        response.setEmbeddingModelUuid(kb.getEmbeddingModelUuid());
        response.setEmbeddingDimension(kb.getEmbeddingDimension());
        response.setRetrieveMaxResults(kb.getRetrieveMaxResults());
        response.setRetrieveMinScore(kb.getRetrieveMinScore());
        response.setQueryLlmTemperature(kb.getQueryLlmTemperature());
        response.setQuerySystemMessage(kb.getQuerySystemMessage());
        response.setCreatedTime(kb.getCreatedTime());
        response.setUpdatedTime(kb.getUpdatedTime());
        return response;
    }
}