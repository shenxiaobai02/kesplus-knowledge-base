# 阶段P1：基础RAG功能实现 - 实现计划

## 一、当前状态分析

### 已完成的任务
| 任务 | 状态 | 文件路径 |
|------|------|----------|
| KnowledgeBase实体 | ✅ | entity/KnowledgeBase.java |
| KnowledgeBaseItem实体 | ✅ | entity/KnowledgeBaseItem.java |
| KnowledgeBaseEmbedding实体 | ✅ | entity/KnowledgeBaseEmbedding.java |
| KnowledgeBaseMapper | ✅ | mapper/KnowledgeBaseMapper.java |
| EmbeddingMapper | ✅ | mapper/EmbeddingMapper.java |
| EmbeddingMapper.xml | ✅ | resources/mapper/EmbeddingMapper.xml |
| EmbeddingModelService | ✅ | service/EmbeddingModelService.java |
| EmbeddingRagService | ✅ | service/EmbeddingRagService.java |
| KnowledgeBaseService | ✅ | service/KnowledgeBaseService.java |
| DynamicTableService | ✅ | service/DynamicTableService.java |

### 需要新增的任务
| 任务 | 状态 | 说明 |
|------|------|------|
| KnowledgeBaseItemMapper | ❌ | 知识点数据访问层 |
| FileStorageService | ❌ | 文件上传存储服务 |
| SmartDocumentSplitter | ❌ | 智能文档切分器 |
| KnowledgeBaseQaService | ❌ | 问答服务 |
| KnowledgeBaseController | ❌ | 知识库控制器 |
| KnowledgeBaseQAController | ❌ | 问答控制器 |
| SSE响应封装 | ❌ | 流式响应工具 |
| 异步索引配置 | ❌ | @Async配置 |

## 二、实现计划

### 任务1：KnowledgeBaseItemMapper
- 创建Mapper接口
- 创建Mapper XML配置

### 任务2：FileStorageService
- 文件上传处理
- 文件解析（支持txt、md、doc、docx、pdf）
- 文件存储管理

### 任务3：SmartDocumentSplitter
- 智能切分策略
- 支持语义切分、结构切分、混合切分
- 配置切分大小（1000 tokens）

### 任务4：KnowledgeBaseItemService
- 知识点CRUD操作
- 索引管理

### 任务5：KnowledgeBaseQaService
- RAG问答逻辑
- 检索+生成流程
- 引用记录管理

### 任务6：KnowledgeBaseController
- 知识库管理API
- 文档上传API
- 索引API

### 任务7：KnowledgeBaseQAController
- 问答API
- SSE流式响应

### 任务8：异步配置
- AsyncConfig配置类
- 线程池配置

### 任务9：SSE工具类
- SSEEmitter封装
- 流式响应管理

### 任务10：测试用例
- 单元测试
- 集成测试

## 三、文件结构规划

```
src/main/java/com/kes/
├── controller/
│   ├── HealthController.java
│   ├── KnowledgeBaseController.java    # 新增
│   └── KnowledgeBaseQAController.java  # 新增
├── service/
│   ├── DynamicTableService.java
│   ├── EmbeddingModelService.java
│   ├── EmbeddingRagService.java
│   ├── FileStorageService.java         # 新增
│   ├── KnowledgeBaseItemService.java   # 新增
│   ├── KnowledgeBaseQaService.java     # 新增
│   └── KnowledgeBaseService.java
├── config/
│   ├── AsyncConfig.java                # 新增
│   ├── DataSourceConfig.java
│   ├── LangChain4jConfig.java
│   └── RagConfig.java
├── util/
│   ├── JsonUtil.java
│   ├── SseEmitterHelper.java           # 新增
│   ├── ThreadContext.java
│   ├── UuidUtil.java
│   └── ValidationUtil.java
├── rag/
│   ├── SmartDocumentSplitter.java      # 新增
│   └── SplitStrategy.java              # 新增
├── mapper/
│   ├── EmbeddingMapper.java
│   ├── EmbeddingModelMapper.java
│   ├── KnowledgeBaseItemMapper.java    # 新增
│   └── KnowledgeBaseMapper.java
└── entity/
    ├── KnowledgeBaseQa.java            # 新增
    ├── ...
```

## 四、技术要点

1. **文档解析**：使用Apache POI处理doc/docx，iText处理PDF，CommonMark处理Markdown
2. **异步索引**：使用@Async注解，配置线程池
3. **SSE响应**：使用Spring SseEmitter实现流式问答
4. **向量检索**：使用PgVector进行相似度搜索
5. **Token估算**：使用OpenAI Tokenizer估算文本长度

## 五、API接口规划

| API路径 | 方法 | 功能 |
|---------|------|------|
| /api/knowledge-base | POST | 创建知识库 |
| /api/knowledge-base/{uuid} | GET | 获取知识库详情 |
| /api/knowledge-base/{uuid} | PUT | 更新知识库 |
| /api/knowledge-base/{uuid} | DELETE | 删除知识库 |
| /api/knowledge-base/upload | POST | 上传文档 |
| /api/knowledge-base/{uuid}/index | POST | 索引文档 |
| /api/knowledge-base/qa | POST | 问答（同步） |
| /api/knowledge-base/qa/sse | POST | 问答（流式） |

## 六、依赖要求

```xml
<!-- 文档解析 -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
</dependency>
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
</dependency>
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark</artifactId>
</dependency>
```

## 七、数据库表设计

### KnowledgeBaseQa 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| uuid | VARCHAR(36) | 唯一标识 |
| kb_uuid | VARCHAR(36) | 知识库UUID |
| question | TEXT | 问题 |
| answer | TEXT | 答案 |
| prompt_tokens | INT | 提示词token数 |
| answer_tokens | INT | 回答token数 |
| created_time | TIMESTAMP | 创建时间 |

## 八、执行顺序

1. 创建实体类 KnowledgeBaseQa
2. 创建 Mapper 层
3. 创建 Service 层
4. 创建 Controller 层
5. 创建配置类
6. 创建工具类
7. 编写测试用例