package com.kes.controller;

import com.kes.common.ResponseWrapper;
import com.kes.dto.request.QaRequest;
import com.kes.dto.response.QaResponse;
import com.kes.service.KnowledgeBasePermissionService;
import com.kes.service.KnowledgeBaseQaService;
import com.kes.util.ThreadContext;
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

    @Autowired
    private KnowledgeBasePermissionService permissionService;

    @PostMapping
    @Operation(summary = "同步问答", description = "基于知识库进行问答，返回完整答案")
    public ResponseWrapper<QaResponse> qa(
            @Parameter(description = "知识库UUID") @PathVariable String kbUuid,
            @Valid @RequestBody QaRequest request) {
        Long userId = ThreadContext.getCurrentUserId();
        
        // 权限校验
        if (!permissionService.hasPermission(userId, kbUuid, "READ")) {
            log.warn("User {} has no permission to query knowledge base {}", userId, kbUuid);
            return ResponseWrapper.error("FORBIDDEN", "无权限访问该知识库");
        }
        
        try {
            QaResponse response = knowledgeBaseQaService.qa(
                kbUuid,
                request.getQuestion(),
                request.getMaxResults(),
                request.getMinScore(),
                request.getTemperature()
            );
            return ResponseWrapper.success(response);
        } catch (Exception e) {
            log.error("QA failed for kb: {}", kbUuid, e);
            return ResponseWrapper.error("INTERNAL_ERROR", "问答服务异常");
        }
    }

    @PostMapping("/stream")
    @Operation(summary = "流式问答", description = "基于知识库进行问答，返回SSE流式响应")
    public SseEmitter streamQa(
            @Parameter(description = "知识库UUID") @PathVariable String kbUuid,
            @Valid @RequestBody QaRequest request) {
        Long userId = ThreadContext.getCurrentUserId();
        
        // 权限校验
        if (!permissionService.hasPermission(userId, kbUuid, "READ")) {
            log.warn("User {} has no permission to stream query knowledge base {}", userId, kbUuid);
            SseEmitter emitter = new SseEmitter(30000L);
            try {
                emitter.send(SseEmitter.event().name("error").data("无权限访问该知识库"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Failed to send error event", e);
            }
            return emitter;
        }
        
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
        Long userId = ThreadContext.getCurrentUserId();
        
        // 权限校验（如果知识库不存在，hasPermission会返回false）
        if (!permissionService.hasPermission(userId, kbUuid, "READ")) {
            log.warn("User {} has no permission to view history of knowledge base {}", userId, kbUuid);
            return ResponseWrapper.success(java.util.Collections.emptyList());
        }
        
        List<QaResponse> history = knowledgeBaseQaService.getHistory(kbUuid, limit);
        return ResponseWrapper.success(history);
    }

    @GetMapping("/count")
    @Operation(summary = "查询问答数量", description = "获取知识库的问答记录数量")
    public ResponseWrapper<Integer> getCount(
            @Parameter(description = "知识库UUID") @PathVariable String kbUuid) {
        Long userId = ThreadContext.getCurrentUserId();
        
        // 权限校验（如果知识库不存在，hasPermission会返回false）
        if (!permissionService.hasPermission(userId, kbUuid, "READ")) {
            log.warn("User {} has no permission to view count of knowledge base {}", userId, kbUuid);
            return ResponseWrapper.success(0);
        }
        
        int count = knowledgeBaseQaService.countByKbUuid(kbUuid);
        return ResponseWrapper.success(count);
    }
}