package com.kes.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QaRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    private Integer maxResults;

    private Double minScore;

    private Double temperature;
}