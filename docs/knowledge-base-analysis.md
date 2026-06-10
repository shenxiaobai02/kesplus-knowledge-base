# 知识库构建功能完整分析文档

## 一、功能概述

知识库构建是 AidDeepin 系统的核心功能之一，基于 **LangChain4j** 框架实现，支持将文档转化为向量和知识图谱两种形式进行存储和检索，实现智能问答能力。

## 二、核心架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           前端层 (Web/Admin)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  KnowledgeBaseController    KnowledgeBaseQAController                      │
└──────────────────────────┬──────────────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────────────┐
│                           业务服务层                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│  KnowledgeBaseService   KnowledgeBaseItemService   KnowledgeBaseQaService  │
└──────────────────────────┬──────────────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────────────┐
│                           RAG引擎层                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│  CompositeRag  ────────────────────────────────────────────────────────────┤
│       │                                                                    │
│       ├── EmbeddingRag (向量检索)                                          │
│       └── GraphRag (图谱检索)                                              │
└──────────────────────────┬──────────────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────────────┐
│                           存储层                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│  PostgreSQL (PgVector)    Neo4j/PostgreSQL(Graph)    Redis(缓存)           │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心实体关系

| 实体 | 说明 | 核心字段 |
|------|------|----------|
| `KnowledgeBase` | 知识库主实体 | title, uuid, ownerId, isPublic, isStrict |
| `KnowledgeBaseItem` | 知识库条目 | kbUuid, title, remark, embeddingStatus, graphicalStatus |
| `KnowledgeBaseQa` | 问答记录 | kbUuid, question, answer, promptTokens, answerTokens |
| `KnowledgeBaseEmbedding` | 向量存储 | kbUuid, kbItemUuid, embedding, text |
| `KnowledgeBaseGraphSegment` | 图谱分段 | kbUuid, kbItemUuid, remark |

## 三、功能模块详解

### 3.1 知识库管理

#### 3.1.1 创建/更新知识库

**入口**：`KnowledgeBaseController.saveOrUpdate()`

**核心逻辑** (`KnowledgeBaseService.saveOrUpdate()`):

```java
// 1. 复制请求参数到实体
KnowledgeBase knowledgeBase = new KnowledgeBase();
BeanUtils.copyProperties(kbEditReq, knowledgeBase, "id", "uuid", ...);

// 2. 设置LLM模型（用于图谱抽取）
if (kbEditReq.getIngestModelId() > 0) {
    knowledgeBase.setIngestModelName(aiModelService.getByIdOrThrow(...).getName());
} else {
    // 自动选择第一个可用的LLM
    LLMContext.getFirstEnableAndFree().ifPresent(llmService -> {
        knowledgeBase.setIngestModelName(llmService.getAiModel().getName());
    });
}

// 3. 新建或更新
if (kbEditReq.getId() == null) {
    knowledgeBase.setUuid(UuidUtil.createShort());
    knowledgeBase.setOwnerId(user.getId());
    baseMapper.insert(knowledgeBase);
} else {
    checkPrivilege(kbEditReq.getId(), null);
    baseMapper.updateById(knowledgeBase);
}
```

**关键设计点**：
- 支持自动选择LLM模型，无需用户手动配置
- 通过 `isStrict` 字段控制严格模式（搜索不到时是否报错）

#### 3.1.2 权限校验机制

```java
private void checkPrivilege(Long kbId, String kbUuid) {
    User user = ThreadContext.getCurrentUser();
    if (user.getIsAdmin()) {
        return;  // 管理员跳过校验
    }
    
    // 验证知识库是否属于当前用户
    LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(KnowledgeBase::getOwnerId, user.getId());
    // ... 省略条件设置
    
    boolean exists = baseMapper.exists(wrapper);
    if (!exists) {
        throw new BaseException(A_USER_NOT_AUTH);
    }
}
```

### 3.2 文档上传与解析

#### 3.2.1 上传流程

**入口**：`KnowledgeBaseController.upload()`

**处理流程**：

```
前端上传文件 → 文件存储 → 文档解析 → 创建知识库条目 → 可选索引
```

**核心代码** (`KnowledgeBaseService.uploadDoc()`):

```java
// 1. 保存文件到存储服务
AdiFile adiFile = fileService.saveFile(doc, false);

// 2. 使用FileOperatorContext解析文档
Document document = FileOperatorContext.loadDocument(adiFile);
if (document == null) {
    log.warn("该文件类型无法解析");
    return adiFile;
}

// 3. 创建知识库条目
KnowledgeBaseItem knowledgeBaseItem = new KnowledgeBaseItem();
knowledgeBaseItem.setUuid(UuidUtil.createShort());
knowledgeBaseItem.setKbId(knowledgeBase.getId());
knowledgeBaseItem.setTitle(fileName);
knowledgeBaseItem.setBrief(StringUtils.substring(content, 0, 200));
knowledgeBaseItem.setRemark(content);
knowledgeBaseItemService.save(knowledgeBaseItem);

// 4. 可选：上传后立即索引
if (Boolean.TRUE.equals(indexAfterUpload)) {
    indexItems(List.of(uuid), indexTypes);
}
```

**支持的文档类型**：
- 文本类：txt, md
- Office文档：doc, docx, ppt, pptx, xls, xlsx
- PDF文件

### 3.3 文档索引（核心功能）

索引分为两种类型：**向量化索引** 和 **图谱化索引**。

#### 3.3.1 索引触发方式

| 触发方式 | API | 说明 |
|----------|-----|------|
| 上传时自动索引 | `/upload/{uuid}?indexAfterUpload=true` | 默认行为 |
| 手动索引整个知识库 | `/indexing/{uuid}` | 重新索引所有条目 |
| 批量索引 | `/item/indexing-list` | 指定条目UUID列表 |

#### 3.3.2 向量化索引流程

**入口**：`KnowledgeBaseItemService.indexingEmbedding()`

**技术原理**：将文档切分为固定大小的文本段，使用 Embedding 模型转换为向量，存储到向量数据库。

```java
private void indexingEmbedding(KnowledgeBase knowledgeBase, KnowledgeBaseItem kbItem, Document document) {
    // 1. 更新状态为"处理中"
    ChainWrappers.lambdaUpdateChain(baseMapper)
            .eq(KnowledgeBaseItem::getId, kbItem.getId())
            .set(KnowledgeBaseItem::getEmbeddingStatus, EmbeddingStatusEnum.DOING)
            .update();
    
    try {
        // 2. 使用EmbeddingRag进行向量化
        EmbeddingRagContext.get(KNOWLEDGE_BASE).ingest(
            document, 
            knowledgeBase.getIngestMaxOverlap(), 
            knowledgeBase.getIngestTokenEstimator(), 
            null
        );
        
        // 3. 更新状态为"完成"
        ChainWrappers.lambdaUpdateChain(baseMapper)
                .eq(KnowledgeBaseItem::getId, kbItem.getId())
                .set(KnowledgeBaseItem::getEmbeddingStatus, EmbeddingStatusEnum.DONE)
                .update();
    } catch (Exception e) {
        // 4. 更新状态为"失败"
        ChainWrappers.lambdaUpdateChain(baseMapper)
                .set(KnowledgeBaseItem::getEmbeddingStatus, EmbeddingStatusEnum.FAIL)
                .update();
    }
}
```

**EmbeddingRag 核心逻辑**：

```java
public void ingest(Document document, int overlap, String tokenEstimator, ChatModel ChatModel) {
    // 1. 创建文档切分器（递归切分，支持token估算）
    DocumentSplitter documentSplitter = DocumentSplitters.recursive(
        RAG_MAX_SEGMENT_SIZE_IN_TOKENS,  // 1000 tokens
        overlap,
        TokenEstimatorFactory.create(tokenEstimator)
    );
    
    // 2. 创建向量化摄入器
    EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(documentSplitter)
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build();
    
    // 3. 执行向量化并存储
    embeddingStoreIngestor.ingest(document);
}
```

**关键参数说明**：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `RAG_MAX_SEGMENT_SIZE_IN_TOKENS` | 1000 | 每个文本段的最大token数 |
| `ingestMaxOverlap` | - | 段与段之间的重叠token数 |
| `tokenEstimator` | openai | token估算器类型（openai/huggingface/qwen） |

#### 3.3.3 图谱化索引流程

**入口**：`KnowledgeBaseItemService.indexingGraph()`

**技术原理**：调用LLM从文本中抽取实体和关系，构建知识图谱。

```java
private void indexingGraph(User user, KnowledgeBase knowledgeBase, KnowledgeBaseItem kbItem, Document document) {
    // 1. 获取LLM服务
    AbstractLLMService llmService = LLMContext.getServiceById(knowledgeBase.getIngestModelId(), true);
    ChatModel chatModel = llmService.buildChatLLM(ChatModelBuilderProperties.builder()
            .temperature(knowledgeBase.getQueryLlmTemperature())
            .build());

    // 2. 使用GraphRag进行图谱化
    GraphRagContext.get(KNOWLEDGE_BASE).ingest(
        GraphIngestParams.builder()
                .user(user)
                .document(document)
                .overlap(knowledgeBase.getIngestMaxOverlap())
                .tokenEstimator(knowledgeBase.getIngestTokenEstimator())
                .ChatModel(chatModel)
                .identifyColumns(List.of(AdiConstant.MetadataKey.KB_UUID))
                .appendColumns(List.of(AdiConstant.MetadataKey.KB_ITEM_UUID))
                .isFreeToken(llmService.getAiModel().getIsFree())
                .build()
    );
}
```

**GraphRag 核心逻辑**：

```java
public void ingest(GraphIngestParams graphIngestParams) {
    DocumentSplitter documentSplitter = DocumentSplitters.recursive(
        RAG_MAX_SEGMENT_SIZE_IN_TOKENS, 
        graphIngestParams.getOverlap(), 
        TokenEstimatorFactory.create(graphIngestParams.getTokenEstimator())
    );
    
    GraphStoreIngestor ingestor = GraphStoreIngestor.builder()
            .documentSplitter(documentSplitter)
            .segmentsFunction(segments -> {
                List<Triple<TextSegment, String, String>> results = new ArrayList<>();
                for (TextSegment segment : segments) {
                    // 1. 保存分段到数据库
                    KnowledgeBaseGraphSegment graphSegment = new KnowledgeBaseGraphSegment();
                    graphSegment.setUuid(UuidUtil.createShort());
                    graphSegment.setRemark(segment.text());
                    getKnowledgeBaseGraphSegmentService().save(graphSegment);
                    
                    // 2. 调用LLM抽取实体关系
                    ChatResponse aiMessageResponse = graphIngestParams.getChatModel()
                        .chat(UserMessage.from(GraphExtractPrompt.GRAPH_EXTRACTION_PROMPT
                            .replace("{input_text}", segment.text())));
                    
                    results.add(Triple.of(segment, segmentId, aiMessageResponse.aiMessage().text()));
                }
                return results;
            })
            .graphStore(graphStore)
            .build();
    
    ingestor.ingest(graphIngestParams.getDocument());
}
```

**图谱抽取提示词**：通过 `GraphExtractPrompt.GRAPH_EXTRACTION_PROMPT` 定义，引导LLM输出结构化的实体和关系信息。

#### 3.3.4 异步索引机制

索引操作通过 `@Async` 注解实现异步执行，避免阻塞主线程：

```java
@Async
public void asyncIndex(User user, KnowledgeBase knowledgeBase, KnowledgeBaseItem kbItem, List<String> indexTypes) {
    String userIndexKey = MessageFormat.format(USER_INDEXING, knowledgeBase.getOwnerId());
    stringRedisTemplate.opsForValue().increment(userIndexKey);  // 增加索引计数器
    
    try {
        // 执行索引逻辑
        if (indexTypes.contains(DOC_INDEX_TYPE_EMBEDDING)) {
            indexingEmbedding(knowledgeBase, kbItem, document);
        }
        if (indexTypes.contains(DOC_INDEX_TYPE_GRAPHICAL)) {
            indexingGraph(user, knowledgeBase, kbItem, document);
        }
    } finally {
        // 减少计数器，完成后删除key
        Long remaining = stringRedisTemplate.opsForValue().decrement(userIndexKey);
        if (remaining != null && remaining <= 0) {
            stringRedisTemplate.delete(userIndexKey);
        }
    }
}
```

**并发控制**：通过 Redis 的 `USER_INDEXING:{userId}` key 防止同一用户同时执行多个索引任务。

### 3.4 知识库问答（RAG检索）

#### 3.4.1 问答流程

**入口**：`KnowledgeBaseQAController.sseAsk()`

**完整链路**：

```
用户提问 → 创建QA记录 → 构建Retriever → RAG检索 → LLM回答 → 更新记录
```

**核心代码** (`KnowledgeBaseService.retrieveAndPushToLLM()`):

```java
@Async
public void retrieveAndPushToLLM(User user, SseEmitter sseEmitter, String qaRecordUuid) {
    // 1. 获取问答记录和知识库配置
    KnowledgeBaseQa qaRecord = knowledgeBaseQaRecordService.getOrThrow(qaRecordUuid);
    KnowledgeBase knowledgeBase = getOrThrow(qaRecord.getKbUuid());
    AiModel aiModel = aiModelService.getByIdOrThrow(qaRecord.getAiModelId());
    
    // 2. 计算最大召回数量（根据模型token窗口）
    int maxResults = knowledgeBase.getRetrieveMaxResults();
    if (maxResults < 1) {
        maxResults = EmbeddingRag.getRetrieveMaxResults(qaRecord.getQuestion(), maxInputTokens);
    }
    
    // 3. 构建Retriever参数
    RetrieverCreateParam createParam = RetrieverCreateParam.builder()
            .chatModel(chatModel)
            .filter(new IsEqualTo(AdiConstant.MetadataKey.KB_UUID, qaRecord.getKbUuid()))
            .maxResults(maxResults)
            .minScore(knowledgeBase.getRetrieveMinScore())
            .breakIfSearchMissed(knowledgeBase.getIsStrict())
            .build();
    
    // 4. 创建CompositeRag并执行检索
    CompositeRag compositeRag = new CompositeRag(KNOWLEDGE_BASE);
    List<RetrieverWrapper> retrieverWrappers = compositeRag.createRetriever(createParam);
    
    // 5. 执行RAG问答
    compositeRag.ragChat(retrievers, sseAskParams, (response, promptMeta, answerMeta) -> {
        sseEmitterHelper.sendComplete(user.getId(), sseEmitter);
        updateQaRecord(...);  // 更新问答记录和token消耗
    });
}
```

#### 3.4.2 CompositeRag 设计

`CompositeRag` 是组合模式的实现，同时支持向量检索和图谱检索：

```java
public class CompositeRag {
    private final EmbeddingRag embeddingRag;
    private final GraphRag graphRag;
    
    public CompositeRag(String retrieverName) {
        this.embeddingRag = EmbeddingRagContext.get(retrieverName);
        this.graphRag = GraphRagContext.get(retrieverName);
    }
    
    public List<RetrieverWrapper> createRetriever(RetrieverCreateParam param) {
        List<RetrieverWrapper> retrievers = new ArrayList<>();
        
        if (embeddingRag != null) {
            ContentRetriever embeddingRetriever = embeddingRag.createRetriever(param);
            retrievers.add(RetrieverWrapper.builder()
                    .contentFrom(embeddingRag.getName())
                    .retriever(embeddingRetriever)
                    .build());
        }
        
        if (graphRag != null) {
            ContentRetriever graphRetriever = graphRag.createRetriever(param);
            retrievers.add(RetrieverWrapper.builder()
                    .contentFrom(graphRag.getName())
                    .retriever(graphRetriever)
                    .build());
        }
        
        return retrievers;
    }
}
```

#### 3.4.3 RAG查询流程

```java
private void query(List<ContentRetriever> retrievers, SseAskParams params, TriConsumer<String, PromptMeta, AnswerMeta> consumer) {
    AbstractLLMService llmService = LLMContext.getServiceOrDefault(params.getModelPlatform(), params.getModelName());
    
    // 1. 创建查询路由器
    QueryRouter queryRouter = new DefaultQueryRouter(retrievers);
    
    // 2. 创建查询转换器（可选，压缩长查询）
    QueryTransformer queryTransformer = new CompressingQueryTransformer(llmService.buildChatLLM(params.getModelProperties()));
    
    // 3. 创建检索增强器
    RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
            .queryTransformer(queryTransformer)
            .queryRouter(queryRouter)
            .build();
    
    // 4. 构建流式聊天助手
    IStreamingChatAssistant assistant = AiServices.builder(IStreamingChatAssistant.class)
            .streamingChatModel(llmService.buildStreamingChatModel(params.getModelProperties()))
            .retrievalAugmentor(retrievalAugmentor)
            .chatMemoryProvider(chatMemoryProvider)
            .build();
    
    // 5. 执行流式问答
    TokenStream tokenStream = assistant.chatWithSystem(
        chatModelRequestParams.getMemoryId(),
        chatModelRequestParams.getSystemMessage(),
        chatModelRequestParams.getUserMessage(),
        new ArrayList<>()
    );
    
    // 6. 处理响应
    tokenStream
            .onPartialResponse(content -> SSEEmitterHelper.parseAndSendPartialMsg(params.getSseEmitter(), content))
            .onCompleteResponse(response -> {
                Pair<PromptMeta, AnswerMeta> pair = SSEEmitterHelper.calculateToken(response, params.getUuid());
                consumer.accept(response.aiMessage().text(), pair.getLeft(), pair.getRight());
            })
            .onError(error -> SSEEmitterHelper.errorAndShutdown(error, params.getSseEmitter()))
            .start();
}
```

**关键设计点**：
- 使用 `QueryRouter` 路由查询到多个Retriever
- 使用 `CompressingQueryTransformer` 压缩过长的用户问题
- 使用 `DefaultRetrievalAugmentor` 自动将检索到的内容注入到prompt中

#### 3.4.4 Token消耗计算

问答完成后，系统会计算并记录token消耗：

```java
private void updateQaRecord(UpdateQaParams updateQaParams) {
    // 1. 从Redis获取token消耗
    Pair<Integer, Integer> inputOutputTokenCost = LLMTokenUtil.calAllTokenCostByUuid(
        stringRedisTemplate, 
        updateQaParams.getSseAskParams().getUuid()
    );
    
    // 2. 更新问答记录
    KnowledgeBaseQa updateRecord = new KnowledgeBaseQa();
    updateRecord.setId(qaRecord.getId());
    updateRecord.setPromptTokens(inputOutputTokenCost.getLeft());
    updateRecord.setAnswer(updateQaParams.getResponse());
    updateRecord.setAnswerTokens(inputOutputTokenCost.getRight());
    knowledgeBaseQaRecordService.updateById(updateRecord);
    
    // 3. 创建引用记录（向量/图谱）
    createRef(updateQaParams.getRetrievers(), user, qaRecord.getId());
    
    // 4. 更新用户当日消耗
    int allToken = inputOutputTokenCost.getLeft() + inputOutputTokenCost.getRight();
    if (allToken > 0) {
        userDayCostService.appendCostToUser(user, allToken, updateQaParams.isTokenFree());
    }
}
```

## 四、调用链路详解

### 4.1 创建知识库链路

```
POST /knowledge-base/saveOrUpdate
    │
    ▼
KnowledgeBaseController.saveOrUpdate()
    │
    ▼
KnowledgeBaseService.saveOrUpdate()
    │
    ├── 设置LLM模型
    ├── 设置Token估算器
    └── baseMapper.insert() / updateById()
```

### 4.2 上传文档链路

```
POST /knowledge-base/upload/{uuid}
    │
    ▼
KnowledgeBaseController.upload()
    │
    ▼
KnowledgeBaseService.uploadDoc()
    │
    ├── fileService.saveFile()          // 文件存储
    ├── FileOperatorContext.loadDocument()  // 文档解析
    ├── knowledgeBaseItemService.save() // 创建条目
    │
    └── [可选] indexItems()             // 索引
            │
            ▼
        KnowledgeBaseItemService.checkAndIndexing()
            │
            ▼
        asyncIndex()
            │
            ├── indexingEmbedding()     // 向量化
            └── indexingGraph()         // 图谱化
```

### 4.3 问答链路

```
POST /knowledge-base/qa/add/{kbUuid}     // 第一步：创建QA记录
    │
    ▼
KnowledgeBaseQAController.add()
    │
    ▼
KnowledgeBaseQaService.add()
    │
    └── 返回 qaRecordUuid

POST /knowledge-base/qa/process/{qaRecordUuid}  // 第二步：处理问答
    │
    ▼
KnowledgeBaseQAController.sseAsk()
    │
    ▼
KnowledgeBaseService.sseAsk()
    │
    ▼
retrieveAndPushToLLM() [@Async]
    │
    ├── 获取知识库配置
    ├── 计算maxResults
    │
    ├── CompositeRag.createRetriever()
    │       │
    │       ├── EmbeddingRag.createRetriever()
    │       └── GraphRag.createRetriever()
    │
    └── CompositeRag.ragChat()
            │
            ▼
        query()
            │
            ├── 创建QueryRouter
            ├── 创建RetrievalAugmentor
            ├── 构建StreamingChatAssistant
            └── 执行流式问答
                    │
                    ├── SSE推送响应
                    └── updateQaRecord()
                            ├── 更新token消耗
                            └── 创建引用记录
```

## 五、核心设计理念

### 5.1 可扩展性设计

#### 5.1.1 多存储后端支持

系统支持多种向量存储后端：
- **PgVector**：基于PostgreSQL的向量存储（默认）
- **Neo4j**：图数据库存储

通过 `IKnowledgeEmbeddingService` 接口抽象：

```java
public interface IKnowledgeEmbeddingService {
    List<KbItemEmbeddingDto> listByEmbeddingIds(List<String> embeddingIds);
    Page<KbItemEmbeddingDto> listByItemUuid(String kbItemUuid, int currentPage, int pageSize);
    boolean deleteByItemUuid(String kbItemUuid);
    Integer countByKbUuid(String kbUuid);
}
```

实现类：
- `pgvector.KnowledgeEmbeddingService`
- `neo4j.KnowledgeEmbeddingService`

#### 5.1.2 多LLM平台支持

通过 `LLMContext` 管理不同平台的LLM服务：
- OpenAI
- DeepSeek
- DashScope（阿里云）
- Ollama（本地）
- SiliconFlow

```java
AbstractLLMService llmService = LLMContext.getServiceById(knowledgeBase.getIngestModelId(), true);
```

### 5.2 性能优化策略

#### 5.2.1 异步索引

索引操作通过 `@Async` 注解异步执行，避免阻塞HTTP响应。

#### 5.2.2 Redis缓存计数

使用Redis维护索引任务计数，防止并发冲突：

```java
stringRedisTemplate.opsForValue().increment(userIndexKey);  // 增加计数
stringRedisTemplate.opsForValue().decrement(userIndexKey);  // 减少计数
```

#### 5.2.3 Token估算优化

根据模型的token窗口自动计算召回数量：

```java
public static int getRetrieveMaxResults(String userQuestion, int maxInputTokens) {
    InputAdaptorMsg inputAdaptorMsg = InputAdaptor.isQuestionValid(userQuestion, maxInputTokens);
    
    if (inputAdaptorMsg.getTokenTooMuch() == TOKEN_TOO_MUCH_QUESTION) {
        return 0;  // 问题过长，不召回
    }
    
    int maxRetrieveDocLength = maxInputTokens - inputAdaptorMsg.getUserQuestionTokenCount();
    return maxRetrieveDocLength / RAG_MAX_SEGMENT_SIZE_IN_TOKENS;
}
```

### 5.3 安全与权限设计

#### 5.3.1 所有权校验

每个知识库绑定到创建者，非管理员只能访问自己的知识库：

```java
wrapper.eq(KnowledgeBase::getOwnerId, user.getId());
```

#### 5.3.2 请求限额

通过Redis实现每日问答限额：

```java
private void checkRequestTimesOrThrow() {
    String key = MessageFormat.format(RedisKeyConstant.AQ_ASK_TIMES, userId, date);
    String askTimes = stringRedisTemplate.opsForValue().get(key);
    String askQuota = SysConfigService.getByKey(QUOTA_BY_QA_ASK_DAILY);
    
    if (Integer.parseInt(askTimes) >= Integer.parseInt(askQuota)) {
        throw new BaseException(A_QA_ASK_LIMIT);
    }
    
    stringRedisTemplate.opsForValue().increment(key);
    stringRedisTemplate.expire(key, Duration.ofDays(1));
}
```

### 5.4 严格模式与宽松模式

通过 `isStrict` 字段控制检索失败时的行为：

| 模式 | isStrict | 检索失败时 |
|------|----------|------------|
| 严格模式 | true | 直接返回错误提示 |
| 宽松模式 | false | 继续请求LLM（不使用知识库） |

```java
if (Boolean.TRUE.equals(knowledgeBase.getIsStrict())) {
    sseEmitterHelper.sendErrorAndComplete(user.getId(), sseEmitter, "提问内容过长...");
} else {
    // 宽松模式：直接调用LLM
    sseEmitterHelper.call(sseAskParams, ...);
}
```

## 六、配置参数说明

### 6.1 知识库配置参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | String | - | 知识库名称 |
| `isPublic` | Boolean | false | 是否公开 |
| `isStrict` | Boolean | false | 是否严格模式 |
| `ingestMaxOverlap` | Integer | - | 文档切分重叠token数 |
| `ingestModelId` | Long | - | 图谱抽取使用的LLM ID |
| `ingestTokenEstimator` | String | openai | Token估算器类型 |
| `retrieveMaxResults` | Integer | 0 | 召回最大数量（0=自动计算） |
| `retrieveMinScore` | Double | 0.6 | 向量搜索最低分数 |
| `queryLlmTemperature` | Double | 0.7 | LLM温度参数 |
| `querySystemMessage` | String | - | 系统提示词 |

### 6.2 RAG常量配置

```java
public static final int RAG_MAX_SEGMENT_SIZE_IN_TOKENS = 1000;  // 每段最大token数
public static final int RAG_RETRIEVE_NUMBER_DEFAULT = 3;        // 默认召回数量
public static final int RAG_RETRIEVE_NUMBER_MAX = 5;            // 最大召回数量
public static final double RAG_MIN_SCORE = 0.6;                 // 最低匹配分数
```

## 七、错误处理机制

### 7.1 常见错误码

| 错误码 | 错误信息 | 触发场景 |
|--------|----------|----------|
| `A_DATA_NOT_FOUND` | 数据不存在 | 查询不存在的知识库/条目 |
| `A_USER_NOT_AUTH` | 用户无权限 | 访问他人知识库 |
| `A_QA_ASK_LIMIT` | 问答次数超限 | 每日问答超过限额 |
| `A_DOC_INDEX_DOING` | 文档正在索引中 | 重复触发索引 |
| `A_UPLOAD_FAIL` | 上传失败 | 文件上传异常 |

### 7.2 状态追踪

每个知识库条目维护索引状态：

```java
public enum EmbeddingStatusEnum {
    NONE,    // 未索引
    DOING,   // 处理中
    DONE,    // 完成
    FAIL     // 失败
}

public enum GraphicalStatusEnum {
    NONE,    // 未图谱化
    DOING,   // 处理中
    DONE,    // 完成
    FAIL     // 失败
}
```

## 八、总结

### 8.1 核心价值

1. **多模态检索**：同时支持向量检索和图谱检索，提升问答准确性
2. **异步处理**：索引操作异步执行，不阻塞用户操作
3. **灵活配置**：支持多种LLM平台、多种存储后端
4. **智能限流**：根据模型token窗口自动计算召回策略
5. **权限控制**：严格的所有权校验和请求限额

### 8.2 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| 框架 | Spring Boot | 后端框架 |
| ORM | MyBatis Plus | 数据库访问 |
| RAG | LangChain4j | 检索增强生成 |
| 向量存储 | PgVector | PostgreSQL向量扩展 |
| 图谱存储 | Neo4j/PostgreSQL | 知识图谱存储 |
| 缓存 | Redis | 限流、计数 |
| 前端 | Vue3 + TypeScript | 用户界面 |

### 8.3 扩展建议

1. **支持更多文档格式**：增加对PDF、图片OCR等格式的支持
2. **增量索引**：支持文档更新后的增量索引
3. **Rerank优化**：增加重排序模型提升检索准确性
4. **多知识库联合检索**：支持同时检索多个知识库
5. **知识库版本管理**：支持知识库的版本控制和回滚

---

**文档版本**：v1.0  
**生成时间**：2026-06-10  
**适用项目**：LangChain4j-AidDeepin
