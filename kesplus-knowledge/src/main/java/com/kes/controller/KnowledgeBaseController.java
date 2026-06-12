package com.kes.controller;

import com.kes.common.ResponseWrapper;
import com.kes.dto.request.KnowledgeBaseCreateRequest;
import com.kes.dto.request.KnowledgeBaseUpdateRequest;
import com.kes.dto.response.KnowledgeBaseItemResponse;
import com.kes.dto.response.KnowledgeBaseResponse;
import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseItem;
import com.kes.rag.SmartDocumentSplitter;
import com.kes.service.EmbeddingModelService;
import com.kes.service.EmbeddingRagService;
import com.kes.service.FileStorageService;
import com.kes.service.KnowledgeBaseItemService;
import com.kes.service.KnowledgeBaseService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/knowledge-base")
@Tag(name = "知识库管理", description = "知识库的CRUD操作和文档上传")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeBaseItemService knowledgeBaseItemService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmbeddingRagService embeddingRagService;

    @Autowired
    private EmbeddingModelService embeddingModelService;

    @Autowired
    private SmartDocumentSplitter smartDocumentSplitter;

    @PostMapping
    @Operation(summary = "创建知识库", description = "创建一个新的知识库")
    public ResponseWrapper<KnowledgeBaseResponse> create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setTitle(request.getTitle());
        kb.setRemark(request.getRemark());
        kb.setIsPublic(request.getIsPublic());
        kb.setIsStrict(request.getIsStrict());
        kb.setRetrieveMaxResults(request.getRetrieveMaxResults());
        kb.setRetrieveMinScore(request.getRetrieveMinScore());
        kb.setQueryLlmTemperature(request.getQueryLlmTemperature());
        kb.setQuerySystemMessage(request.getQuerySystemMessage());

        KnowledgeBase created = knowledgeBaseService.create(kb, request.getEmbeddingModelUuid());
        return ResponseWrapper.success(KnowledgeBaseResponse.fromEntity(created));
    }

    @GetMapping
    @Operation(summary = "查询知识库列表", description = "获取所有知识库列表")
    public ResponseWrapper<List<KnowledgeBaseResponse>> list() {
        List<KnowledgeBase> list = knowledgeBaseService.list();
        List<KnowledgeBaseResponse> responses = list.stream()
            .filter(kb -> !Boolean.TRUE.equals(kb.getIsDeleted()))
            .map(KnowledgeBaseResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseWrapper.success(responses);
    }

    @GetMapping("/tenant/{tenantUuid}")
    @Operation(summary = "按租户查询知识库列表", description = "根据租户UUID获取知识库列表")
    public ResponseWrapper<List<KnowledgeBaseResponse>> listByTenant(
            @Parameter(description = "租户UUID") @PathVariable String tenantUuid) {
        List<KnowledgeBase> list = knowledgeBaseService.listByTenant(tenantUuid);
        List<KnowledgeBaseResponse> responses = list.stream()
            .map(KnowledgeBaseResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseWrapper.success(responses);
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "查询知识库详情", description = "根据UUID获取知识库详情")
    public ResponseWrapper<KnowledgeBaseResponse> get(
            @Parameter(description = "知识库UUID") @PathVariable String uuid) {
        KnowledgeBase kb = knowledgeBaseService.getByUuid(uuid);
        if (kb == null) {
            return ResponseWrapper.error("知识库不存在");
        }
        return ResponseWrapper.success(KnowledgeBaseResponse.fromEntity(kb));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "更新知识库", description = "更新知识库信息")
    public ResponseWrapper<KnowledgeBaseResponse> update(
            @Parameter(description = "知识库UUID") @PathVariable String uuid,
            @RequestBody KnowledgeBaseUpdateRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUuid(uuid);
        kb.setTitle(request.getTitle());
        kb.setRemark(request.getRemark());
        kb.setIsPublic(request.getIsPublic());
        kb.setIsStrict(request.getIsStrict());
        kb.setRetrieveMaxResults(request.getRetrieveMaxResults());
        kb.setRetrieveMinScore(request.getRetrieveMinScore());
        kb.setQueryLlmTemperature(request.getQueryLlmTemperature());
        kb.setQuerySystemMessage(request.getQuerySystemMessage());

        KnowledgeBase updated = knowledgeBaseService.update(kb);
        return ResponseWrapper.success(KnowledgeBaseResponse.fromEntity(updated));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "删除知识库", description = "软删除知识库")
    public ResponseWrapper<Void> delete(@Parameter(description = "知识库UUID") @PathVariable String uuid) {
        knowledgeBaseService.delete(uuid);
        return ResponseWrapper.success();
    }

    @PostMapping("/{uuid}/documents")
    @Operation(summary = "上传文档", description = "上传文档到知识库并进行向量化索引")
    public ResponseWrapper<Void> uploadDocument(
            @Parameter(description = "知识库UUID") @PathVariable String uuid,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        KnowledgeBase kb = knowledgeBaseService.getByUuid(uuid);
        if (kb == null) {
            return ResponseWrapper.error("知识库不存在");
        }

        String fileUuid = fileStorageService.upload(file, uuid);
        Document document = fileStorageService.parse(uuid, fileUuid);
        
        List<Document> chunks = smartDocumentSplitter.split(document);
        
        com.kes.entity.EmbeddingModel entityModel = embeddingModelService.getByUuid(kb.getEmbeddingModelUuid());
        if (entityModel == null) {
            return ResponseWrapper.error("嵌入模型不存在");
        }
        
        EmbeddingModel embeddingModel = embeddingModelService.createLangChainEmbeddingModel(entityModel);
        if (embeddingModel == null) {
            return ResponseWrapper.error("嵌入模型创建失败");
        }

        embeddingRagService.batchIngest(kb, chunks, embeddingModel);
        
        knowledgeBaseItemService.create(uuid, file.getOriginalFilename(), 
            document.text().substring(0, Math.min(document.text().length(), 200)), null);
        
        knowledgeBaseService.updateEmbeddingCount(uuid);
        
        log.info("Document uploaded and indexed: {} for kb: {}", file.getOriginalFilename(), uuid);
        return ResponseWrapper.success();
    }

    @GetMapping("/{uuid}/documents")
    @Operation(summary = "查询文档列表", description = "获取知识库中的文档列表")
    public ResponseWrapper<List<FileStorageService.FileInfo>> listDocuments(
            @Parameter(description = "知识库UUID") @PathVariable String uuid) {
        List<FileStorageService.FileInfo> files = fileStorageService.listFiles(uuid);
        return ResponseWrapper.success(files);
    }

    @DeleteMapping("/{uuid}/documents/{fileUuid}")
    @Operation(summary = "删除文档", description = "删除知识库中的文档")
    public ResponseWrapper<Void> deleteDocument(
            @Parameter(description = "知识库UUID") @PathVariable String uuid,
            @Parameter(description = "文件UUID") @PathVariable String fileUuid) throws IOException {
        fileStorageService.delete(uuid, fileUuid);
        return ResponseWrapper.success();
    }

    @GetMapping("/{uuid}/items")
    @Operation(summary = "查询条目列表", description = "获取知识库中的条目列表")
    public ResponseWrapper<List<KnowledgeBaseItemResponse>> listItems(
            @Parameter(description = "知识库UUID") @PathVariable String uuid) {
        List<KnowledgeBaseItem> items = knowledgeBaseItemService.listByKbUuid(uuid);
        List<KnowledgeBaseItemResponse> responses = items.stream()
            .map(KnowledgeBaseItemResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseWrapper.success(responses);
    }
}