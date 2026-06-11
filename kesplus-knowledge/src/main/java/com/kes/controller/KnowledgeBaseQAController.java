package com.kes.controller;

import com.kes.common.ResponseWrapper;
import com.kes.dto.request.QaRequest;
import com.kes.dto.response.QaResponse;
import com.kes.service.KnowledgeBaseQaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/knowledge-base/{kbUuid}/qa")
@Tag(name = "知识库问答", description = "基于知识库的问答功能，支持同步和流式响应")
public class KnowledgeBaseQAController {

    @Autowired
    private KnowledgeBaseQaService knowledgeBaseQaService;

    @PostMapping
    @Operation(summary = "同步问答", description = "基于知识库进行问答，返回完整答案")
    public ResponseWrapper<QaResponse> qa(
            @Parameter(description = "知识库UUID") @PathVariable String kbUuid,
            @Valid @RequestBody QaRequest request) {
        QaResponse response = knowledgeBaseQaService.qa(
            kbUuid,
            request.getQuestion(),
            request.getMaxResults(),
            request.getMinScore(),
            request.getTemperature()
        );
        return ResponseWrapper.success(response);
    }

    @PostMapping("/stream")
    @Operation(summary = "流式问答", description = "基于知识库进行问答，返回SSE流式响应")
    public SseEmitter streamQa(
            @Parameter(description = "知识库UUID") @PathVariable String kbUuid,
            @Valid @RequestBody QaRequest request) {
        
        SseEmitter emitter = new SseEmitter(30000L);
        
        knowledgeBaseQaService.streamQa(
            kbUuid,
            request.getQuestion(),
            request.getMaxResults(),
            request.getMinScore(),
            request.getTemperature(),
            emitter
        );
        
        return emitter;
    }

    @GetMapping("/history")
    @Operation(summary = "查询问答历史", description = "获取知识库的问答历史记录")
    public ResponseWrapper<List<QaResponse>> getHistory(
            @Parameter(description = "知识库UUID") @PathVariable String kbUuid,
            @Parameter(description = "返回条数限制") @RequestParam(defaultValue = "10") int limit) {
        List<QaResponse> history = knowledgeBaseQaService.getHistory(kbUuid, limit);
        return ResponseWrapper.success(history);
    }
}