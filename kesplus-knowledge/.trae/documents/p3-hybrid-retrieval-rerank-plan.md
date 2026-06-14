# 阶段P3：混合检索与重排序实现计划

## 一、阶段目标

实现向量检索与图谱检索的混合检索能力，通过重排序提升结果相关性。

## 二、当前状态分析

### 2.1 现有架构

| 模块 | 文件路径 | 功能 |
|-----|---------|------|
| 向量检索服务 | `EmbeddingRagService.java` | 文档向量化和向量检索 |
| 动态表管理 | `DynamicTableService.java` | 按维度动态创建向量表 |
| 向量实体 | `KnowledgeBaseEmbedding.java` | 向量数据实体 |
| 向量Mapper | `EmbeddingMapper.java` | 向量数据访问层 |
| RAG配置 | `RagConfig.java` | RAG参数配置（已包含enableGraphRag开关） |

### 2.2 现有配置

- `RagConfig.enableGraphRag` - 已预留GraphRAG开关配置
- `RagConfig.retriever.parallelism` - 已预留并行检索参数
- PostgreSQL + PgVector - 已配置向量存储

### 2.3 待实现功能

| 功能 | 说明 |
|-----|------|
| GraphRag | 图谱检索服务，支持Neo4j和PostgreSQL双方案 ✅ 已完成 |
| HybridRetriever | 混合检索器，并行执行向量+图谱检索 ⏳ 待实现 |
| Reranker | 重排序接口与实现 ⏳ 待实现 |
| 图谱索引 | 文档图谱化索引服务 ✅ 已完成 |
| Neo4j配置 | Neo4j连接配置 ✅ 已完成 |
| 结果合并策略 | 智能合并向量+图谱检索结果 ⏳ 待实现 |

---

## 三、实现方案

### 3.1 图谱存储方案（双方案实现，支持无缝切换） ✅ 已完成

**方案A：使用Neo4j作为图谱存储**

- ✅ 添加Neo4j Java Driver依赖
- ✅ 配置Neo4j连接参数
- ✅ 实现图谱节点和关系的存储
- 适合大规模图谱数据，原生图查询性能优异

**方案B：使用PostgreSQL存储图谱数据**

- ✅ 无需额外依赖
- ✅ 利用现有PostgreSQL基础设施
- ✅ 创建图谱关系表存储节点和边
- 适合中小规模图谱数据，部署简单

**实现策略**：两个方案都实现，通过配置参数 `rag.graph.storage-type`（neo4j/postgresql）无缝切换：

- ✅ 定义统一的 `GraphStorage` 接口
- ✅ 实现 `Neo4jGraphStorage` 和 `PostgresGraphStorage` 两个实现类
- ✅ 使用 `@ConditionalOnProperty` 根据配置动态选择实现
- ✅ 业务代码通过接口调用，不感知底层存储实现

### 3.2 核心类设计

#### 3.2.1 GraphStorage接口（抽象层） ✅ 已实现

```java
// 图谱存储接口 - 支持无缝切换Neo4j和PostgreSQL
public interface GraphStorage {
    
    // 创建节点
    void createNode(KnowledgeBaseGraphNode node);
    
    // 批量创建节点
    void batchCreateNodes(List<KnowledgeBaseGraphNode> nodes);
    
    // 创建关系
    void createEdge(KnowledgeBaseGraphEdge edge);
    
    // 批量创建关系
    void batchCreateEdges(List<KnowledgeBaseGraphEdge> edges);
    
    // 根据内容检索节点
    List<KnowledgeBaseGraphNode> searchNodes(String kbUuid, String query, int limit);
    
    // 获取相关节点（基于关系）
    List<KnowledgeBaseGraphNode> getRelatedNodes(String nodeUuid, int depth, int limit);
    
    // 删除知识库所有图谱数据
    void deleteByKbUuid(String kbUuid);
    
    // 统计节点数量
    int countNodesByKbUuid(String kbUuid);
}

// PostgreSQL实现（默认）
@Service
@ConditionalOnProperty(name = "rag.graph.storage-type", havingValue = "postgresql", matchIfMissing = true)
public class PostgresGraphStorage implements GraphStorage {
    @Autowired
    private GraphNodeMapper nodeMapper;
    @Autowired
    private GraphEdgeMapper edgeMapper;
    // 实现接口方法...
}

// Neo4j实现
@Service
@ConditionalOnProperty(name = "rag.graph.storage-type", havingValue = "neo4j")
public class Neo4jGraphStorage implements GraphStorage {
    @Autowired
    private Driver neo4jDriver;
    // 实现接口方法...
}
```

#### 3.2.2 图谱实体设计 ✅ 已实现

```java
// 图谱节点实体
@Data
@TableName("kes_knowledge_base_graph_node")
public class KnowledgeBaseGraphNode {
    private Long id;
    private String uuid;
    private String kbUuid;
    private String nodeType;       // DOCUMENT, SEGMENT, ENTITY, KEYWORD
    private String nodeId;         // 外部引用ID
    private String content;        // 节点内容
    private String metadataJson;   // 元数据
    private LocalDateTime createdTime;
}

// 图谱关系实体
@Data
@TableName("kes_knowledge_base_graph_edge")
public class KnowledgeBaseGraphEdge {
    private Long id;
    private String uuid;
    private String kbUuid;
    private String sourceNodeUuid;
    private String targetNodeUuid;
    private String relationType;   // CONTAINS, REFERENCES, SIMILAR, KEYWORD
    private Double weight;         // 关系权重
    private String metadataJson;
    private LocalDateTime createdTime;
}
```

#### 3.2.3 GraphRag服务 ✅ 已实现

```java
@Service
public class GraphRagService {
    @Autowired
    private GraphStorage graphStorage;  // 通过接口注入，自动选择实现
    
    // 图谱索引：将文档转换为图谱结构
    public void indexGraph(KnowledgeBase kb, List<Document> documents);
    
    // 图谱检索：基于节点关系进行检索
    public List<GraphRetrievalResult> retrieve(KnowledgeBase kb, String query, int maxResults);
    
    // 获取相关节点
    public List<KnowledgeBaseGraphNode> getRelatedNodes(String nodeUuid, int depth);
}
```

#### 3.2.4 混合检索器 ⏳ 待实现

```java
@Service
public class HybridRetriever {
    // 并行执行向量检索和图谱检索
    public List<HybridRetrievalResult> retrieve(KnowledgeBase kb, String query, 
                                                  EmbeddingModel embeddingModel);
    
    // 合并结果
    private List<HybridRetrievalResult> mergeResults(
        List<KnowledgeBaseEmbedding> vectorResults,
        List<GraphRetrievalResult> graphResults);
}
```

#### 3.2.5 重排序器 ⏳ 待实现

```java
public interface Reranker {
    List<RerankedResult> rerank(String query, List<HybridRetrievalResult> results);
}

@Service
public class ScoreBasedReranker implements Reranker {
    // 基于分数加权重排序
}

@Service  
public class LlmReranker implements Reranker {
    // 基于LLM语义相关性重排序（可选）
}
```

---

## 四、具体修改清单

### 4.1 新增文件

| 序号 | 文件路径 | 说明 | 状态 |
|-----|---------|------|------|
| 1 | `entity/KnowledgeBaseGraphNode.java` | 图谱节点实体 | ✅ 已完成 |
| 2 | `entity/KnowledgeBaseGraphEdge.java` | 图谱关系实体 | ✅ 已完成 |
| 3 | `entity/GraphRetrievalResult.java` | 图谱检索结果 | ✅ 已完成 |
| 4 | `entity/HybridRetrievalResult.java` | 混合检索结果 | ✅ 已完成 |
| 5 | `entity/RerankedResult.java` | 重排序结果 | ✅ 已完成 |
| 6 | `mapper/GraphNodeMapper.java` | 图谱节点Mapper（PostgreSQL方案） | ✅ 已完成 |
| 7 | `mapper/GraphEdgeMapper.java` | 图谱关系Mapper（PostgreSQL方案） | ✅ 已完成 |
| 8 | `graph/GraphStorage.java` | 图谱存储接口（抽象层） | ✅ 已完成 |
| 9 | `graph/PostgresGraphStorage.java` | PostgreSQL图谱存储实现 | ✅ 已完成 |
| 10 | `graph/Neo4jGraphStorage.java` | Neo4j图谱存储实现 | ✅ 已完成 |
| 11 | `config/Neo4jConfig.java` | Neo4j连接配置 | ✅ 已完成 |
| 12 | `service/GraphRagService.java` | 图谱检索服务 | ✅ 已完成 |
| 13 | `service/HybridRetriever.java` | 混合检索器 | ⏳ 待实现 |
| 14 | `service/RerankerService.java` | 重排序服务 | ⏳ 待实现 |
| 15 | `reranker/Reranker.java` | 重排序接口 | ⏳ 待实现 |
| 16 | `reranker/ScoreBasedReranker.java` | 分数加权重排序实现 | ⏳ 待实现 |
| 17 | `reranker/LlmReranker.java` | LLM重排序实现（可选） | ⏳ 待实现 |
| 18 | `test/service/GraphRagServiceTest.java` | 图谱服务测试 | ⏳ 待实现 |
| 19 | `test/service/HybridRetrieverTest.java` | 混合检索测试 | ⏳ 待实现 |
| 20 | `test/service/RerankerServiceTest.java` | 重排序测试 | ⏳ 待实现 |
| 21 | `test/graph/PostgresGraphStorageTest.java` | PostgreSQL图谱存储测试 | ⏳ 待实现 |
| 22 | `test/graph/Neo4jGraphStorageTest.java` | Neo4j图谱存储测试 | ⏳ 待实现 |

### 4.2 修改文件

| 序号 | 文件路径 | 修改内容 | 状态 |
|-----|---------|---------|------|
| 1 | `config/RagConfig.java` | 添加图谱存储类型配置、Neo4j配置参数、重排序配置 | ⏳ 待实现 |
| 2 | `service/EmbeddingRagService.java` | 添加图谱索引调用 | ⏳ 待实现 |
| 3 | `service/KnowledgeBaseQaService.java` | 集成HybridRetriever | ⏳ 待实现 |
| 4 | `resources/application.yml` | 添加图谱存储类型、Neo4j和重排序配置 | ⏳ 待实现 |
| 5 | `resources/schema.sql` | 添加图谱表DDL（PostgreSQL方案） | ✅ 已完成 |
| 6 | `pom.xml` | 添加Neo4j Java Driver依赖（必选） | ✅ 已完成 |

### 4.3 数据库变更（PostgreSQL方案） ✅ 已完成

```sql
-- 图谱节点表
CREATE TABLE kes_knowledge_base_graph_node (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    kb_uuid VARCHAR(36) NOT NULL,
    node_type VARCHAR(20) NOT NULL,
    node_id VARCHAR(36),
    content TEXT NOT NULL,
    metadata_json TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 图谱关系表
CREATE TABLE kes_knowledge_base_graph_edge (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    kb_uuid VARCHAR(36) NOT NULL,
    source_node_uuid VARCHAR(36) NOT NULL,
    target_node_uuid VARCHAR(36) NOT NULL,
    relation_type VARCHAR(20) NOT NULL,
    weight DOUBLE PRECISION DEFAULT 1.0,
    metadata_json TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_graph_node_kb_uuid ON kes_knowledge_base_graph_node(kb_uuid);
CREATE INDEX idx_graph_node_type ON kes_knowledge_base_graph_node(node_type);
CREATE INDEX idx_graph_edge_kb_uuid ON kes_knowledge_base_graph_edge(kb_uuid);
CREATE INDEX idx_graph_edge_source ON kes_knowledge_base_graph_edge(source_node_uuid);
CREATE INDEX idx_graph_edge_target ON kes_knowledge_base_graph_edge(target_node_uuid);
```

---

## 五、实现步骤

### 步骤1：依赖配置 ✅ 已完成

- ✅ 添加Neo4j Java Driver依赖到 `pom.xml`
- ✅ 配置Neo4j连接参数

### 步骤2：数据库表创建（DDL - PostgreSQL方案） ✅ 已完成

- ✅ 创建图谱节点表 `kes_knowledge_base_graph_node`
- ✅ 创建图谱关系表 `kes_knowledge_base_graph_edge`
- ✅ 创建相关索引

### 步骤3：实体类创建 ✅ 已完成

- ✅ 创建 `KnowledgeBaseGraphNode` 实体
- ✅ 创建 `KnowledgeBaseGraphEdge` 实体
- ✅ 创建结果实体类（`GraphRetrievalResult`, `HybridRetrievalResult`, `RerankedResult`）

### 步骤4：GraphStorage接口实现（双方案） ✅ 已完成

- ✅ 定义 `GraphStorage` 接口
- ✅ 实现 `PostgresGraphStorage`（使用Mapper）
  - 使用 `@ConditionalOnProperty(name = "rag.graph.storage-type", havingValue = "postgresql", matchIfMissing = true)`
  - 默认方案，无需额外配置
- ✅ 实现 `Neo4jGraphStorage`（使用Neo4j Driver）
  - 使用 `@ConditionalOnProperty(name = "rag.graph.storage-type", havingValue = "neo4j")`
  - 需配置Neo4j连接参数

**双方案无缝切换已实现**：通过配置参数 `rag.graph.storage-type`（neo4j/postgresql）自动切换，无需修改代码。

### 步骤5：Mapper层实现（PostgreSQL方案） ✅ 已完成

- ✅ 实现 `GraphNodeMapper`
- ✅ 实现 `GraphEdgeMapper`

### 步骤6：Neo4j配置实现 ✅ 已完成

- ✅ 实现 `Neo4jConfig` 配置类
- ✅ 配置Neo4j Driver Bean（仅在storage-type为neo4j时生效）

### 步骤7：GraphRag服务实现 ✅ 已完成

- ✅ 实现图谱索引逻辑（使用GraphStorage接口）
- ✅ 实现图谱检索逻辑
- ✅ 实现节点关系查询
- ✅ 注入GraphStorage接口，自动选择实现

### 步骤8：HybridRetriever实现 ⏳ 待实现

**实现内容**：
- 创建 `HybridRetriever.java` 服务类
- 实现并行检索（使用CompletableFuture）
  - 并行执行向量检索（EmbeddingRagService）
  - 并行执行图谱检索（GraphRagService）
  - 配置超时控制（rag.retriever.timeoutMs）
- 实现结果合并策略
  - 基于分数加权合并（vectorScore + graphScore）
  - 去重处理（相同内容只保留高分）
  - 来源标记（区分向量检索和图谱检索）
- 集成向量检索和图谱检索

**关键代码结构**：
```java
@Service
public class HybridRetriever {
    @Autowired
    private EmbeddingRagService embeddingRagService;
    
    @Autowired
    private GraphRagService graphRagService;
    
    @Autowired
    private RagConfig ragConfig;
    
    public List<HybridRetrievalResult> retrieve(KnowledgeBase kb, String query, EmbeddingModel embeddingModel) {
        // 并行检索
        CompletableFuture<List<KnowledgeBaseEmbedding>> vectorFuture = 
            CompletableFuture.supplyAsync(() -> embeddingRagService.retrieve(kb, query, embeddingModel));
        
        CompletableFuture<List<GraphRetrievalResult>> graphFuture = 
            CompletableFuture.supplyAsync(() -> graphRagService.retrieve(kb, query, ragConfig.getMaxRetrieveResults()));
        
        // 合并结果
        return mergeResults(vectorFuture.get(), graphFuture.get());
    }
    
    private List<HybridRetrievalResult> mergeResults(
        List<KnowledgeBaseEmbedding> vectorResults,
        List<GraphRetrievalResult> graphResults) {
        // 基于分数加权合并，去重处理
    }
}
```

### 步骤9：Reranker实现 ⏳ 待实现

**实现内容**：
- 创建 `reranker/Reranker.java` 接口
- 创建 `reranker/ScoreBasedReranker.java` 实现
  - 基于分数加权重排序
  - vectorScore * weight + graphScore * weight
- 创建 `reranker/LlmReranker.java` 实现（可选）
  - 基于LLM语义相关性重排序
  - 使用ChatModel评估相关性
- 创建 `service/RerankerService.java` 服务类
  - 集成Reranker接口
  - 根据配置选择重排序策略

**关键代码结构**：
```java
public interface Reranker {
    List<RerankedResult> rerank(String query, List<HybridRetrievalResult> results);
}

@Service
@ConditionalOnProperty(name = "rag.reranker.type", havingValue = "score", matchIfMissing = true)
public class ScoreBasedReranker implements Reranker {
    @Override
    public List<RerankedResult> rerank(String query, List<HybridRetrievalResult> results) {
        // 基于分数加权重排序
    }
}

@Service
@ConditionalOnProperty(name = "rag.reranker.type", havingValue = "llm")
public class LlmReranker implements Reranker {
    @Autowired
    private ChatModel chatModel;
    
    @Override
    public List<RerankedResult> rerank(String query, List<HybridRetrievalResult> results) {
        // 使用LLM评估语义相关性
    }
}
```

### 步骤10：配置更新 ⏳ 待实现

**实现内容**：
- 更新 `RagConfig.java`
  - 添加 `GraphConfig` 内部类
    - storageType: String (neo4j/postgresql)
    - neo4jUri: String
    - neo4jUsername: String
    - neo4jPassword: String
  - 添加 `RerankerConfig` 内部类
    - type: String (score/llm)
    - vectorWeight: Double
    - graphWeight: Double
- 更新 `application.yml`
  - 添加图谱存储配置
  - 添加Neo4j连接配置
  - 添加重排序配置

**关键配置结构**：
```yaml
rag:
  graph:
    storage-type: postgresql  # neo4j/postgresql
    neo4j:
      uri: bolt://localhost:7687
      username: neo4j
      password: password
  reranker:
    type: score  # score/llm
    vector-weight: 0.6
    graph-weight: 0.4
```

### 步骤11：集成到现有服务 ⏳ 待实现

**实现内容**：
- 更新 `KnowledgeBaseQaService.java`
  - 注入 `HybridRetriever`
  - 在qa方法中使用混合检索（当enableGraphRag为true时）
  - 使用Reranker重排序结果
- 更新 `EmbeddingRagService.java`
  - 注入 `GraphRagService`
  - 在batchIngest方法中添加图谱索引调用（当enableGraphRag为true时）

**关键代码修改**：
```java
// KnowledgeBaseQaService.java
@Autowired
private HybridRetriever hybridRetriever;

@Autowired
private RerankerService rerankerService;

public QaResponse qa(String kbUuid, String question, ...) {
    if (ragConfig.getEnableGraphRag()) {
        // 使用混合检索
        List<HybridRetrievalResult> hybridResults = hybridRetriever.retrieve(kb, question, embeddingModel);
        // 重排序
        List<RerankedResult> rerankedResults = rerankerService.rerank(question, hybridResults);
        // 构建上下文
        String context = buildContextFromReranked(rerankedResults);
    } else {
        // 使用向量检索
        List<KnowledgeBaseEmbedding> retrieved = embeddingRagService.retrieve(kb, question, embeddingModel);
        String context = buildContext(retrieved);
    }
}

// EmbeddingRagService.java
@Autowired
private GraphRagService graphRagService;

public void batchIngest(KnowledgeBase kb, List<Document> documents, EmbeddingModel embeddingModel) {
    // 向量索引
    embeddingMapper.batchInsert(tableName, embeddings);
    
    // 图谱索引（可选）
    if (ragConfig.getEnableGraphRag()) {
        graphRagService.indexGraph(kb, documents);
    }
}
```

### 步骤12：测试编写 ⏳ 待实现

**实现内容**：
- 编写 `PostgresGraphStorageTest.java`
  - 测试节点创建和查询
  - 测试关系创建和查询
  - 测试相关节点获取
- 编写 `Neo4jGraphStorageTest.java`
  - 测试节点创建和查询
  - 测试关系创建和查询
  - 测试相关节点获取
- 编写 `GraphRagServiceTest.java`
  - 测试图谱索引
  - 测试图谱检索
- 编写 `HybridRetrieverTest.java`
  - 测试并行检索
  - 测试结果合并
- 编写 `RerankerServiceTest.java`
  - 测试分数加权重排序
  - 测试LLM重排序（可选）

---

## 六、技术要求

### 6.1 并行检索优化

- 使用 `CompletableFuture` 实现向量检索和图谱检索并行执行
- 配置并行度参数 `rag.retriever.parallelism`
- 添加超时控制

### 6.2 结果合并策略

- 基于分数加权合并
- 去重处理（相同内容只保留高分）
- 来源标记（区分向量检索和图谱检索）

### 6.3 重排序策略

- 分数加权：向量分数 + 图谱关系权重
- LLM重排序：使用LLM评估语义相关性（可选）
- 可配置重排序策略

---

## 七、验收标准

| 标准 | 指标 |
|-----|------|
| 混合检索响应时间 | <2s |
| 检索准确率提升 | ≥10% |
| 单元测试覆盖率 | ≥80% |
| 并行检索成功率 | ≥99% |
| 结果合并正确性 | 100% |
| 存储方案切换 | 配置切换无需重启 |

---

## 八、风险与应对

| 风险 | 应对措施 |
|-----|---------|
| Neo4j部署复杂 | 提供PostgreSQL备选方案，默认使用PostgreSQL |
| 图谱索引性能 | 使用批量索引，异步处理 |
| 并行检索超时 | 配置合理超时时间，添加降级策略 |
| 结果合并冲突 | 实现去重逻辑，优先保留高分结果 |
| 存储方案切换 | 使用接口抽象，Spring条件注入自动切换 |

---

## 九、依赖关系

**前置依赖**：P1阶段（基础RAG功能）已完成

**后续依赖**：为P4阶段（查询增强与Self-RAG）提供检索基础

---

## 十、回滚SQL

```sql
-- 回滚图谱表
DROP TABLE IF EXISTS kes_knowledge_base_graph_edge;
DROP TABLE IF EXISTS kes_knowledge_base_graph_node;
```