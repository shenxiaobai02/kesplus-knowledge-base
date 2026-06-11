# 阶段P1：基础RAG功能实现 - 详细实现计划

## 一、项目现状分析

### 1.1 已完成的组件

| 组件 | 状态 | 说明 |
|------|------|------|
| 实体类 | ✅ | KnowledgeBase、KnowledgeBaseItem、KnowledgeBaseEmbedding、KnowledgeBaseQa |
| Mapper层 | ✅ | KnowledgeBaseMapper、KnowledgeBaseItemMapper、EmbeddingMapper、KnowledgeBaseQaMapper |
| Async配置 | ✅ | AsyncConfig配置了indexExecutor和qaExecutor线程池 |
| SmartDocumentSplitter | ✅ | 支持SEMANTIC、STRUCTURAL、HYBRID三种切分策略 |
| DynamicTableService | ✅ | 动态创建向量表，支持多维度模型 |
| EmbeddingModelService | ✅ | 嵌入模型管理服务 |
| KnowledgeBaseService | ✅ | 知识库管理服务 |
| EmbeddingRagService | ✅ | 向量检索服务 |

### 1.2 需要实现的组件

| 组件 | 状态 | 说明 |
|------|------|------|
| FileStorageService | ❌ | 文件上传、存储、解析功能 |
| KnowledgeBaseItemService | ❌ | 知识库条目管理服务 |
| KnowledgeBaseQaService | ❌ | 问答逻辑服务 |
| KnowledgeBaseController | ❌ | 知识库管理API控制器 |
| KnowledgeBaseQAController | ❌ | 问答API控制器 |
| SSE响应 | ❌ | 流式问答响应实现 |
| 测试用例 | ❌ | 单元测试和集成测试 |

---

## 二、实现任务清单

### 2.1 任务分解

| 序号 | 任务 | 描述 | 优先级 |
|------|------|------|--------|
| P1-01 | FileStorageService | 实现文件上传、存储、解析功能，支持txt、md、doc、docx、pdf | 高 |
| P1-02 | KnowledgeBaseItemService | 实现知识库条目管理，包括增删改查 | 高 |
| P1-03 | KnowledgeBaseQaService | 实现问答逻辑，包括检索+生成流程 | 高 |
| P1-04 | KnowledgeBaseController | 知识库CRUD API | 高 |
| P1-05 | KnowledgeBaseQAController | 问答API，支持同步和SSE流式响应 | 高 |
| P1-06 | SseEmitterHelper | SSE流式响应工具类 | 高 |
| P1-07 | DTO类创建 | 创建请求/响应DTO | 中 |
| P1-08 | 依赖添加 | 添加Apache POI、PDF解析等依赖 | 中 |
| P1-09 | 单元测试 | 编写核心服务的单元测试 | 中 |

---

## 三、文件结构规划

```
kesplus-knowledge/src/main/java/com/kes/
├── controller/
│   ├── KnowledgeBaseController.java    # 知识库管理控制器
│   └── KnowledgeBaseQAController.java  # 问答控制器
├── service/
│   ├── FileStorageService.java         # 文件存储服务
│   ├── KnowledgeBaseItemService.java   # 知识库条目服务
│   └── KnowledgeBaseQaService.java     # 问答服务
├── dto/
│   ├── request/
│   │   ├── KnowledgeBaseCreateRequest.java
│   │   ├── KnowledgeBaseUpdateRequest.java
│   │   ├── DocumentUploadRequest.java
│   │   └── QaRequest.java
│   └── response/
│       ├── KnowledgeBaseResponse.java
│       ├── KnowledgeBaseItemResponse.java
│       ├── QaResponse.java
│       └── DocumentUploadResponse.java
└── util/
    └── SseEmitterHelper.java           # SSE工具类
```

---

## 四、API接口设计

### 4.1 知识库管理接口

| API路径 | HTTP方法 | Controller | 功能描述 |
|---------|----------|------------|----------|
| /api/knowledge-base | POST | KnowledgeBaseController | 创建知识库 |
| /api/knowledge-base | GET | KnowledgeBaseController | 查询知识库列表 |
| /api/knowledge-base/{uuid} | GET | KnowledgeBaseController | 查询知识库详情 |
| /api/knowledge-base/{uuid} | PUT | KnowledgeBaseController | 更新知识库 |
| /api/knowledge-base/{uuid} | DELETE | KnowledgeBaseController | 删除知识库 |
| /api/knowledge-base/{uuid}/documents | POST | KnowledgeBaseController | 上传文档 |
| /api/knowledge-base/{uuid}/documents | GET | KnowledgeBaseController | 查询文档列表 |
| /api/knowledge-base/{uuid}/documents/{docUuid} | DELETE | KnowledgeBaseController | 删除文档 |

### 4.2 问答接口

| API路径 | HTTP方法 | Controller | 功能描述 |
|---------|----------|------------|----------|
| /api/knowledge-base/{uuid}/qa | POST | KnowledgeBaseQAController | 同步问答 |
| /api/knowledge-base/{uuid}/qa/stream | POST | KnowledgeBaseQAController | SSE流式问答 |
| /api/knowledge-base/{uuid}/qa/history | GET | KnowledgeBaseQAController | 查询问答历史 |

---

## 五、关键实现细节

### 5.1 文件存储服务设计

```
FileStorageService
├── upload(MultipartFile, kbUuid) → String  # 上传文件返回UUID
├── parse(String filePath) → Document       # 解析文件为Document对象
├── getStoragePath() → String               # 获取存储路径
├── delete(String fileUuid) → void          # 删除文件
└── listFiles(kbUuid) → List<FileInfo>      # 列出知识库文件
```

### 5.2 问答服务流程

```
问答流程：
1. 用户提问 → QaController
2. 检索相关文档 → EmbeddingRagService.retrieve()
3. 构建Prompt → 拼接检索结果
4. LLM生成 → Ollama/LangChain4j
5. 返回结果 → 同步响应或SSE流式响应
```

### 5.3 SSE响应设计

使用Spring的SseEmitter实现流式响应：
- 设置超时时间：30秒
- 支持错误处理和异常关闭
- 分块发送LLM生成的内容

---

## 六、依赖说明

需要添加以下Maven依赖：

| 依赖 | GroupId | ArtifactId | 版本 | 用途 |
|------|---------|------------|------|------|
| Apache POI | org.apache.poi | poi | 5.2.5 | Word文档解析(doc) |
| Apache POI OOXML | org.apache.poi | poi-ooxml | 5.2.5 | Word文档解析(docx) |
| PDFBox | org.apache.pdfbox | pdfbox | 3.0.2 | PDF文档解析 |
| CommonMark | org.commonmark | commonmark | 0.21.0 | Markdown解析 |

---

## 七、数据库表设计

### 7.1 已存在的表

| 表名 | 说明 |
|------|------|
| kes_knowledge_base | 知识库主表 |
| kes_knowledge_base_item | 知识库条目表 |
| kes_knowledge_base_embedding_{dimension} | 向量表（动态创建） |
| kes_knowledge_base_qa | 问答记录表 |

### 7.2 表结构概要

**kes_knowledge_base**：
- id, uuid, title, remark, is_public, is_strict, embedding_dimension, ...

**kes_knowledge_base_item**：
- id, uuid, kb_uuid, title, brief, is_deleted, ...

**kes_knowledge_base_embedding_{dim}**：
- id, uuid, kb_uuid, kb_item_uuid, embedding (vector), text, metadata_json, ...

---

## 八、测试计划

### 8.1 单元测试覆盖

| 测试类 | 测试方法 | 覆盖功能 |
|--------|----------|----------|
| KnowledgeBaseServiceTest | testCreate, testUpdate, testDelete | 知识库CRUD |
| EmbeddingRagServiceTest | testIngest, testRetrieve | 向量索引和检索 |
| SmartDocumentSplitterTest | testSplit | 文档切分 |
| KnowledgeBaseQaServiceTest | testQa, testStreamQa | 问答逻辑 |

### 8.2 集成测试

- 文档上传测试：支持txt、md、doc、docx、pdf
- 端到端问答测试：上传文档→索引→问答

---

## 九、验收标准

| 指标 | 目标值 | 验证方式 |
|------|--------|----------|
| 文档上传成功率 | ≥99% | 测试上传100个不同格式文件 |
| 向量检索响应时间 | <2s | JMeter压测 |
| 问答准确率 | ≥80% | 人工评估或自动化评估 |
| 单元测试覆盖率 | ≥80% | JaCoCo报告 |

---

## 十、风险与应对

| 风险 | 描述 | 应对措施 |
|------|------|----------|
| 文件解析失败 | 某些文档格式解析失败 | 添加异常处理和重试机制 |
| 向量索引耗时 | 大文件索引时间长 | 使用@Async异步索引 |
| LLM服务不可用 | Ollama服务宕机 | 添加熔断降级和错误处理 |
| 内存溢出 | 大文件处理导致OOM | 流式读取和分批处理 |

---

**文档版本**: v1.0  
**生成时间**: 2026-06-10  
**适用项目**: kesplus-knowledge-base