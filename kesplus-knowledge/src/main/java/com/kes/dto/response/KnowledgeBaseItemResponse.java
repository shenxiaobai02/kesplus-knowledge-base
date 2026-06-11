package com.kes.dto.response;

import com.kes.entity.KnowledgeBaseItem;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseItemResponse {

    private Long id;
    private String uuid;
    private String kbUuid;
    private String title;
    private String brief;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static KnowledgeBaseItemResponse fromEntity(KnowledgeBaseItem item) {
        KnowledgeBaseItemResponse response = new KnowledgeBaseItemResponse();
        response.setId(item.getId());
        response.setUuid(item.getUuid());
        response.setKbUuid(item.getKbUuid());
        response.setTitle(item.getTitle());
        response.setBrief(item.getBrief());
        response.setRemark(item.getRemark());
        response.setCreatedTime(item.getCreatedTime());
        response.setUpdatedTime(item.getUpdatedTime());
        return response;
    }
}