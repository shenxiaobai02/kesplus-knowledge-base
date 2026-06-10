# Advanced RAG 功能与知识库角色隔离设计方案 (增强版)

## 一、设计概述

### 1.1 项目背景

基于现有 `langchain4j-aideepin` 项目的知识库功能，结合 LangChain4j 框架，设计支持以下核心能力的后端系统：

| 能力维度 | 描述 |
|---------|------|
| Advanced RAG | 支持多种RAG技术组合，从基础到高级逐步升级 |
| 角色隔离 | 面向业务和角色的知识库权限控制 |
| 多租户支持 | 支持不同组织/业务线的知识库隔离 |
| 高可用架构 | 支持熔断降级、多级缓存、水平扩展 |
| 可观测性 | 完善的监控指标和日志体系 |

### 1.2 设计原则

遵循生产环境部署黄金法则：
1. 先从基础RAG开始，建立基准线
2. 用评估工具找出RAG在哪个环节出问题
3. 针对性地添加对应的Advanced RAG技术
4. 每次只加一个技术，评估效果后再继续
5. 架构设计遵循高内聚低耦合，支持灵活扩展

---

## 二、Advanced RAG 技术选型与架构

### 2.1 RAG技术分层架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        用户提问层 (User Query)                              │
│    ├── API Gateway                                                        │
│    │     ├── Rate Limiting    ├── Authentication    ├── Authorization     │
├─────────────────────────────────────────────────────────────────────────────┤
│                        查询增强层 (Query Enhancement)                        │
│    ├── 查询改写    ├── 查询扩展    ├── 查询重排序    ├── 意图识别          │
├─────────────────────────────────────────────────────────────────────────────┤
│                      检索层 (Retrieval Layer)                              │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐                   │
│  │ Vector RAG    │  │ Graph RAG     │  │ Hybrid RAG    │                   │
│  │ (向量检索)     │  │ (图谱检索)     │  │ (混合检索)     │                   │
│  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘                   │
│          │                  │                  │                           │
│          └──────────────────┼──────────────────┘                           │
│                             ▼                                             │
│                  ┌───────────────────┐                                   │
│                  │  Rerank (重排序)  │                                   │
│                  └────────┬──────────┘                                   │
├───────────────────────────┼───────────────────────────────────────────────┤
│                    增强层 (Augmentation Layer)                             │
│    ├── Context Injection (上下文注入)    ├── Self-RAG (自评估)            │
├─────────────────────────────────────────────────────────────────────────────┤
│                    生成层 (Generation Layer)                              │
│    ├── LLM Integration    ├── Response Streaming                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                        存储层 (Storage)                                   │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐                  │
│  │ PostgreSQL    │  │ PgVector      │  │ Redis         │                  │
│  │ (业务数据)     │  │ (向量数据)     │  │ (缓存/限流)   │                  │
│  └───────────────┘  └───────────────┘  └───────────────┘                  │
│                     ┌───────────────┐                                     │
│                     │ Neo4j         │                                     │
│                     │ (图谱数据)     │                                     │
│                     └───────────────┘                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                        基础设施层                                          │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐                  │
│  │ 监控告警      │  │ 配置中心      │  │ 熔断降级      │                  │
│  │ (Prometheus)  │  │ (Nacos)       │  │ (Resilience4j)│                  │
│  └───────────────┘  └───────────────┘  └───────────────┘                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 RAG技术选择矩阵

| 问题类型 | 推荐技术 | 复杂度 | 实现优先级 |
|---------|---------|--------|-----------|
| 简单问答，准确率要求不高 | 基础RAG | ★☆☆☆☆ | P0 |
| 检索结果经常不相关 | 混合检索 + 重排序 | ★★☆☆☆ | P1 |
| 经常出现上下文断裂 | 智能分块 + 上下文注入 | ★★☆☆☆ | P1 |
| 用户不会提问 | 查询增强 | ★★★☆☆ | P2 |
| 需要回答多跳问题 | Graph RAG | ★★★★☆ | P2 |
| 很多问题不需要检索 | Self-RAG | ★★★☆☆ | P3 |
| 需要实时信息 | CRAG | ★★★★☆ | P3 |
| 系统稳定性要求高 | 熔断降级 + 多级缓存 | ★★★☆☆ | P1 |

---

## 三、核心功能设计

### 3.1 查询增强 (Query Enhancement)

#### 3.1.1 功能说明

对用户原始查询进行预处理，提升检索效果：
- **查询改写**：将模糊问题转化为更精确的查询
- **查询扩展**：添加同义词、相关概念
- **意图识别**：识别用户查询意图类型

#### 3.1.2 核心类设计

```java
public interface QueryEnhancer {
    
    List<String> enhance(String originalQuery, QueryContext context);
    
    QueryIntent recognizeIntent(String query);
}

public enum QueryIntent {
    FACTS,           // 事实性问题
    PROCEDURE,       // 流程性问题
    COMPARISON,      // 比较性问题
    ANALYSIS,        // 分析性问题
    CREATIVE,        // 创意性问题
    UNKNOWN          // 未知意图
}

public class QueryContext {
    private String kbUuid;
    private List<String> historyQuestions;
    private User user;
    private QueryIntent intent;
}
```

### 3.2 混合检索 (Hybrid Retrieval)

#### 3.2.1 功能说明

组合向量检索和图谱检索，通过重排序提升结果相关性。

#### 3.2.2 核心类设计

```java
public class HybridRetriever implements ContentRetriever {
    
    private final EmbeddingRag embeddingRag;
    private final GraphRag graphRag;
    private final Reranker reranker;
    private final RagMetricsService metricsService;
    
    @Override
    public List<Content> retrieve(String query) {
        // 并行执行向量检索和图谱检索
        CompletableFuture<List<Content>> vectorFuture = CompletableFuture.supplyAsync(
            () -> embeddingRag.retrieve(query));
        CompletableFuture<List<Content>> graphFuture = CompletableFuture.supplyAsync(
            () -> graphRag.retrieve(query));
        
        List<Content> vectorResults = vectorFuture.join();
        List<Content> graphResults = graphFuture.join();
        
        // 合并结果
        List<Content> mergedResults = mergeResults(vectorResults, graphResults);
        
        // 重排序
        List<Content> rerankedResults = reranker.rerank(query, mergedResults);
        
        // 记录指标
        rerankedResults.forEach(c -> metricsService.recordScore(c.metadata().getScore()));
        
        return rerankedResults;
    }
}

public interface Reranker {
    List<Content> rerank(String query, List<Content> contents);
}
```

### 3.3 智能分块与上下文注入

#### 3.3.1 功能说明

根据文档内容结构智能切分，保留上下文关联性。

#### 3.3.2 核心类设计

```java
public class SmartDocumentSplitter {
    
    private final EmbeddingModel embeddingModel;
    
    public List<TextSegment> split(Document document, SplitStrategy strategy) {
        DocumentSplitter splitter = createSplitter(strategy);
        return splitter.split(document);
    }
    
    private DocumentSplitter createSplitter(SplitStrategy strategy) {
        return switch (strategy) {
            case SEMANTIC -> new SemanticDocumentSplitter(embeddingModel);
            case STRUCTURAL -> new StructuralDocumentSplitter();
            case HYBRID -> new HybridDocumentSplitter(embeddingModel);
        };
    }
}

public enum SplitStrategy {
    SEMANTIC,    // 语义切分
    STRUCTURAL,  // 结构切分（按章节）
    HYBRID       // 混合切分
}
```

### 3.4 Self-RAG 自评估机制

#### 3.4.1 功能说明

让LLM自动评估是否需要检索，避免不必要的知识库查询。

#### 3.4.2 核心类设计

```java
public class SelfRagEvaluator {
    
    private final ChatModel chatModel;
    
    public RetrievalDecision evaluate(String query) {
        String prompt = buildEvaluationPrompt(query);
        String response = chatModel.generate(prompt).content();
        return parseDecision(response);
    }
    
    private String buildEvaluationPrompt(String query) {
        return """
            请判断以下问题是否需要检索知识库：
            问题：{query}
            
            回答格式：
            NEED_RETRIEVAL: true/false
            REASON: 简要说明理由
            KEYWORDS: 检索关键词（用逗号分隔）
            """.replace("{query}", query);
    }
}

public class RetrievalDecision {
    private boolean needRetrieval;
    private String reason;
    private List<String> keywords;
}
```

### 3.5 多级缓存架构

#### 3.5.1 功能说明

实现L1（本地缓存）+ L2（分布式缓存）的多级缓存策略，减少重复计算。

#### 3.5.2 核心类设计

```java
@Service
public class RagCacheService {
    
    private final StringRedisTemplate redisTemplate;
    private final Cache<String, List<Content>> localCache;
    
    private static final String CACHE_PREFIX = "rag:cache:";
    private static final long CACHE_TTL_MINUTES = 30;
    
    public List<Content> getFromCache(String query, String kbUuid) {
        String cacheKey = buildCacheKey(query, kbUuid);
        
        // 先查L1缓存
        List<Content> result = localCache.get(cacheKey);
        if (result != null) {
            return result;
        }
        
        // 再查L2缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            result = JsonUtil.fromJson(cached, new TypeReference<List<Content>>() {});
            localCache.put(cacheKey, result);
            return result;
        }
        
        return null;
    }
    
    public void putToCache(String query, String kbUuid, List<Content> contents) {
        String cacheKey = buildCacheKey(query, kbUuid);
        localCache.put(cacheKey, contents);
        redisTemplate.opsForValue().set(cacheKey, JsonUtil.toJson(contents), 
                Duration.ofMinutes(CACHE_TTL_MINUTES));
    }
}
```

### 3.6 插件化组件注册

#### 3.6.1 功能说明

支持动态注册和扩展RAG组件，实现插件化架构。

#### 3.6.2 核心类设计

```java
public interface RagComponent<T> {
    String getType();
    T getComponent();
}

@Component
public class RagComponentRegistry {
    
    private final Map<String, RagComponent<?>> components = new ConcurrentHashMap<>();
    
    @Autowired
    public RagComponentRegistry(List<RagComponent<?>> componentList) {
        componentList.forEach(c -> components.put(c.getType(), c));
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getComponent(String type, Class<T> clazz) {
        RagComponent<?> component = components.get(type);
        return component != null ? clazz.cast(component.getComponent()) : null;
    }
    
    public <T> void register(String type, T component) {
        components.put(type, () -> component);
    }
}
```

### 3.7 文档版本管理

#### 3.7.1 功能说明

支持文档版本控制、历史追溯和回滚功能。

#### 3.7.2 核心类设计

```java
@Data
@TableName("adi_knowledge_item_version")
public class KnowledgeItemVersion extends BaseEntity {
    private String kbItemUuid;
    private String version;
    private String content;
    private String changeLog;
    private String previousVersion;
}

@Service
public class VersionService {
    
    public KnowledgeItemVersion createVersion(String kbItemUuid, String content, String changeLog) {
        KnowledgeItemVersion version = new KnowledgeItemVersion();
        version.setKbItemUuid(kbItemUuid);
        version.setVersion(generateVersion(kbItemUuid));
        version.setContent(content);
        version.setChangeLog(changeLog);
        version.setPreviousVersion(getLatestVersion(kbItemUuid));
        versionMapper.insert(version);
        return version;
    }
    
    public KnowledgeItemVersion rollback(String kbItemUuid, String version) {
        KnowledgeItemVersion targetVersion = versionMapper.selectByKbItemUuidAndVersion(kbItemUuid, version);
        knowledgeBaseItemService.updateContent(kbItemUuid, targetVersion.getContent());
        return targetVersion;
    }
}
```

---

## 四、知识库角色隔离设计

### 4.1 权限模型设计

#### 4.1.1 权限层次结构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          权限层次结构                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│  租户层 (Tenant)                                                           │
│    │                                                                      │
│    ├── 业务线层 (Business Line)                                            │
│    │     │                                                                │
│    │     ├── 知识库层 (Knowledge Base)                                     │
│    │     │     │                                                          │
│    │     │     ├── 条目层 (Knowledge Item)                                │
│    │     │     │     │                                                     │
│    │     │     │     └── 版本层 (Version)                                 │
│    │     │     │                                                          │
│    │     │     └── 向量/图谱数据                                           │
│    │     │                                                                │
│    │     └── 角色权限 (Role Permission)                                    │
│    │                                                                      │
│    └── 管理员权限 (Admin)                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 4.1.2 角色定义

| 角色 | 权限说明 | 适用场景 |
|------|---------|---------|
| `SUPER_ADMIN` | 系统管理员，拥有所有权限 | 平台运维 |
| `TENANT_ADMIN` | 租户管理员，管理租户内所有资源 | 企业IT管理员 |
| `BUSINESS_ADMIN` | 业务线管理员，管理业务线内知识库 | 部门负责人 |
| `KB_OWNER` | 知识库所有者，完全控制知识库 | 知识库创建者 |
| `KB_EDITOR` | 知识库编辑者，可编辑知识库内容 | 内容维护人员 |
| `KB_READER` | 知识库读者，仅可查看和查询 | 普通员工 |
| `KB_GUEST` | 知识库访客，仅可查询公开内容 | 外部用户 |

### 4.2 权限实体设计

#### 4.2.1 新增实体

```java
@Data
@TableName("adi_tenant")
public class Tenant extends BaseEntity {
    private String uuid;
    private String name;
    private String domain;
    private String description;
    private Boolean isActive;
    private String configJson;  // 租户级别配置
}

@Data
@TableName("adi_business_line")
public class BusinessLine extends BaseEntity {
    private String uuid;
    private String name;
    private String tenantUuid;
    private String description;
}

@Data
@TableName("adi_role")
public class Role extends BaseEntity {
    private String uuid;
    private String name;
    private String code;
    private String description;
    private List<String> permissions;  // 权限列表(JSON)
}

@Data
@TableName("adi_user_role")
public class UserRole extends BaseEntity {
    private Long userId;
    private String roleUuid;
    private String kbUuid;
    private String businessLineUuid;
}
```

#### 4.2.2 细粒度权限服务

```java
@Service
public class FineGrainedPermissionService {
    
    public boolean hasPermission(Long userId, String resourceType, 
                                  String resourceUuid, String action) {
        // 检查RBAC角色权限
        if (checkRolePermission(userId, resourceType, action)) {
            return true;
        }
        // 检查ABAC属性权限
        return checkAttributePermission(userId, resourceType, resourceUuid, action);
    }
    
    private boolean checkAttributePermission(Long userId, String resourceType, 
                                              String resourceUuid, String action) {
        Map<String, Object> resourceAttrs = getResourceAttributes(resourceType, resourceUuid);
        Map<String, Object> userAttrs = getUserAttributes(userId);
        return evaluatePolicy(userAttrs, resourceAttrs, action);
    }
}
```

### 4.3 权限校验机制

```java
@Service
public class KnowledgeBasePermissionService {
    
    public boolean hasPermission(Long userId, String kbUuid) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByUuid(kbUuid);
        if (kb == null || kb.getIsDeleted()) {
            throw new BaseException(A_DATA_NOT_FOUND);
        }
        
        if (!checkVisibility(userId, kb)) {
            return false;
        }
        
        return checkRolePermission(userId, kb);
    }
    
    private boolean checkVisibility(Long userId, KnowledgeBase kb) {
        return switch (kb.getVisibility()) {
            case "PUBLIC" -> true;
            case "INTERNAL" -> isInternalUser(userId, kb.getTenantUuid());
            case "PRIVATE" -> isOwnerOrAdmin(userId, kb);
        };
    }
}
```

---

## 五、容错与可靠性设计

### 5.1 熔断降级机制

```java
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreaker retrievalCircuitBreaker() {
        return CircuitBreaker.of("retrieval", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(10)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(100)
                .build());
    }
    
    @Bean
    public Retry retrievalRetry() {
        return Retry.of("retrieval", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(RuntimeException.class)
                .build());
    }
    
    @Bean
    public Bulkhead retrievalBulkhead() {
        return Bulkhead.of("retrieval", BulkheadConfig.custom()
                .maxConcurrentCalls(50)
                .build());
    }
}

@Service
public class ResilientRetriever {
    
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;
    
    public List<Content> retrieveWithFallback(String query, Supplier<List<Content>> fallback) {
        return Try.ofSupplier(
            Bulkhead.decorateSupplier(bulkhead,
                Retry.decorateSupplier(retry,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                        () -> embeddingRag.retrieve(query)))))
            .recover(fallback)
            .get();
    }
}
```

### 5.2 监控与可观测性

```java
@Service
public class RagMetricsService {
    
    private final Counter retrievalCounter = Counter.builder("rag.retrieval.count")
            .tag("type", "embedding")
            .description("Total retrieval operations")
            .register(Metrics.globalRegistry);
    
    private final Timer retrievalTimer = Timer.builder("rag.retrieval.duration")
            .description("Retrieval duration")
            .register(Metrics.globalRegistry);
    
    private final Histogram retrievalScore = Histogram.builder("rag.retrieval.score")
            .description("Retrieval match scores")
            .register(Metrics.globalRegistry);
    
    private final Gauge retrievalCacheHitRatio = Gauge.builder("rag.cache.hit.ratio", 
            () -> calculateHitRatio())
            .description("Cache hit ratio")
            .register(Metrics.globalRegistry);
    
    public <T> T recordRetrieval(String retrieverType, Supplier<T> operation) {
        retrievalCounter.increment();
        return retrievalTimer.record(operation);
    }
}
```

---

## 六、统一配置管理

### 6.1 RAG配置类

```java
@ConfigurationProperties(prefix = "rag")
@Data
public class RagConfig {
    private int maxSegmentSize = 1000;
    private int maxRetrieveResults = 5;
    private double minScore = 0.6;
    private boolean enableGraphRag = true;
    private boolean enableSelfRag = false;
    private boolean enableCache = true;
    private RetrieverConfig retriever = new RetrieverConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private CacheConfig cache = new CacheConfig();
    
    @Data
    public static class RetrieverConfig {
        private int timeoutMs = 30000;
        private int retryCount = 3;
        private int batchSize = 100;
        private int parallelism = 4;
    }
    
    @Data
    public static class EmbeddingConfig {
        private String modelType = "ollama";
        private String baseUrl = "http://localhost:11434";
        private String modelName = "all-minilm";
        private int batchSize = 32;
    }
    
    @Data
    public static class CacheConfig {
        private long ttlMinutes = 30;
        private int maxSize = 10000;
        private String cacheType = "caffeine";
    }
}
```

---

## 七、数据库设计

### 7.1 动态表设计（按向量维度分表）

#### 7.1.1 设计原理

当向量模型维度发生变化时，需要动态创建对应维度的向量表。每个维度使用独立的表存储，命名规则为：`kes_knowledge_base_embedding_{dimension}`

**设计优势：**
- 支持多模型多维度共存
- 优化向量索引效率（同维度向量存储在一起）
- 便于数据迁移和维护

#### 7.1.2 向量表结构（动态创建）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| uuid | VARCHAR(36) | UNIQUE NOT NULL | 唯一标识 |
| kb_uuid | VARCHAR(36) | NOT NULL | 关联知识库 |
| kb_item_uuid | VARCHAR(36) | | 关联知识点 |
| embedding | vector(N) | NOT NULL | 向量数据(N为维度) |
| text | TEXT | NOT NULL | 原始文本 |
| metadata_json | TEXT | | 元数据(JSON) |
| created_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引设计：**
```sql
-- 知识库索引
CREATE INDEX idx_{table}_kb_uuid ON {table}(kb_uuid);
-- HNSW向量索引（用于相似度搜索）
CREATE INDEX idx_{table}_vector ON {table} USING hnsw (embedding vector_cosine_ops);
```

#### 7.1.3 嵌入模型配置表 (kes_embedding_model)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| uuid | VARCHAR(36) | UNIQUE NOT NULL | 唯一标识 |
| model_name | VARCHAR(100) | NOT NULL | 模型名称 |
| embedding_dimension | INT | NOT NULL | 向量维度 |
| model_type | VARCHAR(50) | | 模型类型(huggingface/ollama/openai) |
| base_url | VARCHAR(255) | | API地址 |
| api_key | VARCHAR(255) | | API密钥 |
| config_json | TEXT | | 配置(JSON) |
| is_active | BOOLEAN | DEFAULT true | 是否启用 |
| created_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | | 更新时间 |

#### 7.1.4 动态表管理流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                    动态表管理流程                                  │
├─────────────────────────────────────────────────────────────────────┤
│  1. 创建嵌入模型                                                  │
│     ↓                                                            │
│  2. 检查对应维度表是否存在                                         │
│     ↓                                                            │
│  3. 不存在则创建表（含索引）                                        │
│     ↓                                                            │
│  4. 创建知识库时关联模型                                          │
│     ↓                                                            │
│  5. 向量化时写入对应维度表                                         │
│     ↓                                                            │
│  6. 查询时根据知识库配置选择对应表                                   │
└─────────────────────────────────────────────────────────────────────┘
```

#### 7.1.5 图知识库说明

**图知识库不需要按维度分表**，原因如下：

| 维度 | 向量知识库 | 图知识库 |
|------|-----------|---------|
| 数据结构 | 固定维度向量 | 节点和边，结构灵活 |
| 索引方式 | HNSW索引，依赖维度 | 图遍历，不依赖维度 |
| 维度影响 | 直接影响表结构和索引 | 无直接影响 |
| 存储方式 | 每个维度一张表 | 统一的图存储 |

### 7.2 新增表结构

#### 7.2.1 租户表 (adi_tenant)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY | 主键 |
| uuid | VARCHAR(36) | UNIQUE NOT NULL | 租户唯一标识 |
| name | VARCHAR(100) | NOT NULL | 租户名称 |
| domain | VARCHAR(255) | UNIQUE | 租户域名 |
| description | TEXT | | 描述 |
| is_active | BOOLEAN | DEFAULT true | 是否启用 |
| config_json | TEXT | | 租户配置(JSON) |
| created_time | DATETIME | NOT NULL | 创建时间 |
| updated_time | DATETIME | | 更新时间 |

#### 7.1.2 业务线表 (adi_business_line)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY | 主键 |
| uuid | VARCHAR(36) | UNIQUE NOT NULL | 业务线唯一标识 |
| name | VARCHAR(100) | NOT NULL | 业务线名称 |
| tenant_uuid | VARCHAR(36) | FOREIGN KEY | 关联租户 |
| description | TEXT | | 描述 |

#### 7.1.3 角色表 (adi_role)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY | 主键 |
| uuid | VARCHAR(36) | UNIQUE NOT NULL | 角色唯一标识 |
| name | VARCHAR(50) | NOT NULL | 角色名称 |
| code | VARCHAR(50) | UNIQUE NOT NULL | 角色编码 |
| description | TEXT | | 描述 |
| permissions | TEXT | | 权限列表(JSON) |

#### 7.1.4 用户角色关联表 (adi_user_role)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY | 主键 |
| user_id | BIGINT | FOREIGN KEY | 关联用户 |
| role_uuid | VARCHAR(36) | FOREIGN KEY | 关联角色 |
| kb_uuid | VARCHAR(36) | FOREIGN KEY | 限定知识库(可选) |
| business_line_uuid | VARCHAR(36) | FOREIGN KEY | 限定业务线(可选) |

#### 7.1.5 文档版本表 (adi_knowledge_item_version)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY | 主键 |
| kb_item_uuid | VARCHAR(36) | FOREIGN KEY | 关联文档条目 |
| version | VARCHAR(20) | NOT NULL | 版本号 |
| content | TEXT | | 版本内容 |
| change_log | TEXT | | 变更说明 |
| previous_version | VARCHAR(20) | | 上一版本号 |
| created_time | DATETIME | NOT NULL | 创建时间 |

### 7.2 知识库表扩展

| 新增字段名 | 类型 | 约束 | 说明 |
|-----------|------|------|------|
| tenant_uuid | VARCHAR(36) | FOREIGN KEY | 所属租户 |
| business_line_uuid | VARCHAR(36) | FOREIGN KEY | 所属业务线 |
| visibility | VARCHAR(20) | DEFAULT 'PRIVATE' | 可见性 |
| allowed_role_codes | TEXT | | 允许角色列表(JSON) |
| config_json | TEXT | | 知识库配置(JSON) |

---

## 八、API接口设计

### 8.1 权限管理接口

| API路径 | HTTP方法 | 功能描述 |
|---------|---------|---------|
| `/api/admin/tenant` | POST | 创建租户 |
| `/api/admin/tenant/{uuid}` | GET | 获取租户详情 |
| `/api/admin/tenant/{uuid}` | PUT | 更新租户 |
| `/api/admin/tenant/{uuid}` | DELETE | 删除租户 |
| `/api/admin/business-line` | POST | 创建业务线 |
| `/api/admin/business-line/{uuid}` | GET | 获取业务线详情 |
| `/api/admin/role` | POST | 创建角色 |
| `/api/admin/role/{uuid}` | GET | 获取角色详情 |
| `/api/admin/user-role` | POST | 分配角色给用户 |
| `/api/admin/user-role/batch` | POST | 批量分配角色 |
| `/api/admin/user-role/{id}` | DELETE | 移除角色分配 |

### 8.2 知识库接口增强

#### 8.2.1 创建知识库

```json
POST /api/knowledge-base
{
    "title": "产品知识库",
    "remark": "产品相关文档",
    "isPublic": false,
    "isStrict": false,
    "tenantUuid": "tenant-001",
    "businessLineUuid": "bl-001",
    "visibility": "INTERNAL",
    "allowedRoleCodes": ["KB_EDITOR", "KB_READER"],
    "retrieveMaxResults": 5,
    "retrieveMinScore": 0.6,
    "ingestMaxOverlap": 100,
    "ingestModelId": 1,
    "ingestTokenEstimator": "openai",
    "queryLlmTemperature": 0.7,
    "querySystemMessage": "你是产品专家...",
    "splitStrategy": "HYBRID",
    "enableCache": true
}
```

#### 8.2.2 版本管理接口

| API路径 | HTTP方法 | 功能描述 |
|---------|---------|---------|
| `/api/knowledge-base/item/{uuid}/versions` | GET | 获取版本列表 |
| `/api/knowledge-base/item/{uuid}/version` | POST | 创建新版本 |
| `/api/knowledge-base/item/{uuid}/version/{version}` | GET | 获取指定版本 |
| `/api/knowledge-base/item/{uuid}/version/{version}/rollback` | POST | 回滚到指定版本 |

### 8.3 监控指标接口

| API路径 | HTTP方法 | 功能描述 |
|---------|---------|---------|
| `/api/monitor/metrics` | GET | 获取RAG相关指标 |
| `/api/monitor/health` | GET | 健康检查 |
| `/api/monitor/cache/stats` | GET | 缓存统计信息 |

---

## 九、安全设计

### 9.1 访问控制矩阵

| 操作 | SUPER_ADMIN | TENANT_ADMIN | BUSINESS_ADMIN | KB_OWNER | KB_EDITOR | KB_READER | KB_GUEST |
|------|-------------|--------------|----------------|----------|-----------|-----------|----------|
| 创建租户 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 管理租户 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 创建业务线 | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 管理业务线 | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| 创建知识库 | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| 编辑知识库 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| 删除知识库 | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| 上传文档 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| 索引文档 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| 查询知识库 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅* |
| 查看文档 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅* |
| 创建版本 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| 回滚版本 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |

> *: 仅对公开知识库或有访问权限的知识库

### 9.2 敏感操作审计

```java
@Service
public class AuditService {
    
    @Resource
    private AuditLogMapper auditLogMapper;
    
    public void log(AuditAction action, String targetType, String targetUuid, String detail) {
        AuditLog log = new AuditLog();
        log.setUserId(ThreadContext.getCurrentUserId());
        log.setAction(action.name());
        log.setTargetType(targetType);
        log.setTargetUuid(targetUuid);
        log.setDetail(detail);
        log.setIpAddress(ThreadContext.getClientIp());
        log.setUserAgent(ThreadContext.getUserAgent());
        auditLogMapper.insert(log);
    }
}

public enum AuditAction {
    KB_CREATE, KB_UPDATE, KB_DELETE,
    DOC_UPLOAD, DOC_DELETE, DOC_INDEX,
    DOC_VERSION_CREATE, DOC_VERSION_ROLLBACK,
    QA_ASK,
    ROLE_ASSIGN, ROLE_REMOVE,
    TENANT_CREATE, TENANT_UPDATE, TENANT_DELETE
}
```

### 9.3 数据保护

```java
@Service
public class DataProtectionService {
    
    private final Encryptor encryptor;
    private final Masker masker;
    
    public String encrypt(String plaintext) {
        return encryptor.encrypt(plaintext);
    }
    
    public String decrypt(String ciphertext) {
        return encryptor.decrypt(ciphertext);
    }
    
    public String mask(String data, String dataType) {
        return switch (dataType) {
            case "phone" -> masker.maskPhone(data);
            case "email" -> masker.maskEmail(data);
            case "idcard" -> masker.maskIdCard(data);
            default -> masker.maskGeneric(data);
        };
    }
}
```

---

## 十、部署与集成

### 10.1 依赖与环境

| 依赖 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 语言版本 |
| Spring Boot | 4.0.x | 框架版本 |
| LangChain4j | 0.34.x | RAG框架 |
| MyBatis Plus | 3.5.x | ORM框架 |
| Resilience4j | 2.0.x | 熔断降级 |
| Micrometer | 1.12.x | 监控指标 |
| PostgreSQL | 16+ | 主数据库（含PgVector） |
| Redis | 7+ | 缓存与限流 |
| Neo4j | 5+ | 可选：图谱存储 |
| Prometheus | 2.40+ | 监控系统 |
| Grafana | 10+ | 可视化 |

### 10.2 配置示例

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aideepin
    username: admin
    password: password
  
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000ms

langchain4j:
  embedding:
    model:
      type: ollama
      base-url: http://localhost:11434
      name: all-minilm
  chat-model:
    type: ollama
    base-url: http://localhost:11434
    name: llama3

rag:
  max-segment-size: 1000
  max-retrieve-results: 5
  min-score: 0.6
  enable-graph-rag: true
  enable-self-rag: false
  enable-cache: true
  
  retriever:
    timeout-ms: 30000
    retry-count: 3
    batch-size: 100
    parallelism: 4
  
  embedding:
    model-type: ollama
    base-url: http://localhost:11434
    model-name: all-minilm
    batch-size: 32
  
  cache:
    ttl-minutes: 30
    max-size: 10000
    cache-type: caffeine

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

resilience4j:
  circuitbreaker:
    instances:
      retrieval:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 10
```

---

## 十一、性能优化策略

### 11.1 性能指标目标

| 指标 | 优化目标 | 说明 |
|------|----------|------|
| 检索响应时间 | <1s | 从查询到返回结果 |
| 向量索引吞吐量 | 1000 docs/min | 文档向量化速度 |
| 缓存命中率 | >70% | 查询结果缓存命中 |
| 错误率 | <1% | 系统错误率 |
| 系统可用性 | 99.99% | 全年宕机时间<53分钟 |

### 11.2 优化策略

| 策略 | 说明 | 实施方式 |
|------|------|----------|
| 多级缓存 | L1本地缓存 + L2分布式缓存 | Caffeine + Redis |
| 批量处理 | 批量向量化、批量写入 | 异步批量任务 |
| 并行检索 | 向量检索和图谱检索并行 | CompletableFuture |
| 熔断降级 | 保护系统免受外部依赖故障 | Resilience4j |
| 索引优化 | 数据库索引、向量索引 | PostgreSQL索引、PgVector |
| 连接池优化 | 数据库连接池配置 | HikariCP调优 |

---

## 十二、水平扩展方案

### 12.1 分片检索设计

```java
public class ShardedRetriever implements ContentRetriever {
    
    private final List<ContentRetriever> shardRetrievers;
    private final Reranker reranker;
    
    @Override
    public List<Content> retrieve(String query) {
        List<CompletableFuture<List<Content>>> futures = shardRetrievers.stream()
                .map(retriever -> CompletableFuture.supplyAsync(() -> retriever.retrieve(query)))
                .toList();
        
        List<Content> allResults = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
        
        return reranker.rerank(query, allResults);
    }
}
```

### 12.2 部署架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           负载均衡层                                        │
│                        Nginx / Load Balancer                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                           应用层                                           │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐                  │
│  │  API Server   │  │  API Server   │  │  API Server   │                  │
│  │    Node 1     │  │    Node 2     │  │    Node N     │                  │
│  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘                  │
│          │                  │                  │                           │
├──────────┼──────────────────┼──────────────────┼──────────────────────────┤
│                           缓存层                                           │
│                      Redis Cluster                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                           存储层                                           │
│                PostgreSQL (主从复制) + PgVector                             │
│                     Neo4j Cluster (可选)                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 十三、升级路径与实施建议

### 13.1 实施阶段规划

| 阶段 | 周期 | 目标 | 关键交付物 |
|------|------|------|-----------|
| P0 | 1-2周 | 基础RAG稳定运行 | 向量检索、基础权限 |
| P1 | 2-3周 | 混合检索+重排序+缓存 | HybridRetriever、多级缓存 |
| P2 | 2-3周 | 查询增强+Graph RAG+熔断降级 | QueryEnhancer、容错机制 |
| P3 | 2-4周 | Self-RAG+CRAG+版本管理 | 自评估、增量索引、版本控制 |
| P4 | 2-3周 | 水平扩展+监控体系 | 分片检索、可观测性 |

### 13.2 评估指标

| 指标 | 说明 | 目标值 |
|------|------|--------|
| 检索准确率 | 正确检索到相关文档的比例 | >85% |
| 回答相关性 | 回答与问题的相关程度 | >90% |
| 召回率 | 召回相关文档的比例 | >95% |
| 响应时间 | 从提问到首字符响应 | <3s |
| Token效率 | 检索内容占总token比例 | 60-70% |
| 缓存命中率 | 缓存命中的查询比例 | >70% |
| 系统可用性 | 系统可用时间比例 | 99.99% |

---

## 十四、总结

本设计方案基于现有知识库架构，扩展实现了：

1. **Advanced RAG能力**：查询增强、混合检索、重排序、智能分块、Self-RAG等
2. **角色隔离体系**：支持租户、业务线、角色三级权限控制，支持RBAC+ABAC混合模式
3. **多租户支持**：租户级数据隔离，支持企业级部署
4. **高可用架构**：熔断降级、多级缓存、批量处理、异步索引
5. **可观测性**：完善的监控指标、审计日志、健康检查
6. **扩展性**：插件化架构、水平扩展支持、动态配置

设计遵循"从基础到高级逐步升级"的原则，每个阶段可独立评估效果，确保生产环境的稳定性和可观测性。

---

**文档版本**: v2.0 (增强版)  
**生成时间**: 2026-06-10  
**适用项目**: LangChain4j-AidDeepin
