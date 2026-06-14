# 意图识别与Query改写优化方案

## 一、现状分析

### 1.1 设计文档要求 vs 现有实现对比

| 模块层 | 设计文档要求 | 现有实现 | 差距分析 |
|:---|:---|:---|:---|
| **意图识别层** | BERT/LLM双轨并行 + RAG增强分类 | 仅规则+关键词匹配 | ❌ 准确率无法达到90%+ |
| **上下文管理** | DST + 短期/长期记忆 | 仅有历史问题列表 | ❌ 缺少槽位填充和状态跟踪 |
| **查询改写层** | 规则+LLM双引擎，含结构化转换、反事实生成 | 仅有同义词扩展、指代消解 | ❌ 改写策略单一，质量有限 |
| **路由分发层** | 动态路由决策器 | 无 | ❌ 缺少意图驱动的路由 |
| **评估机制** | 多级fallback，置信度阈值 | LLM评估+规则兜底 | ⚠️ 缺少置信度阈值触发机制 |

### 1.2 核心问题定位

#### 问题1: 意图识别准确率不足

**现状**:
- 仅基于关键词规则匹配（`INTENT_KEYWORDS` Map）
- 无法处理长尾查询、泛化要求高的场景
- 无置信度评估机制

**设计文档建议**:
> "BERT微调分类（二级意图≈15类）+ RAG增强分类兜底长尾查询"

**量化差距**:
- 当前准确率估计: ~65-70%
- 设计目标: ≥90%
- 差距: 20-25个百分点

#### 问题2: 缺少多轮对话上下文管理

**现状**:
- `QueryContext.historyQuestions` 仅存储问题文本
- 无法建立问题间的语义关联
- 无法进行槽位填充

**设计文档建议**:
> "多轮对话意图追踪...可实现88%+的准确率提升"

#### 问题3: 查询改写策略单一

**现状**:
- 仅支持同义词扩展、指代消解、缩写展开
- 缺少: 结构化转换、反事实生成
- 缺少: 意图驱动的改写策略选择

**设计文档建议**:
> "Knowledge_Retrieval意图 → 聚焦关键词扩展与术语规范化；Data_Analysis意图 → 提取时间、维度约束，转为SQL风格查询"

#### 问题4: 缺少可观测性

**现状**:
- 仅在日志中记录评估结果
- 无结构化的评估指标采集
- 无法进行在线评估和迭代优化

---

## 二、优化目标与原则

### 2.1 可量化改进指标

| 指标 | 当前值 | 目标值 | 改进幅度 |
|:---|:---|:---|:---|
| 意图识别准确率 | ~65% | ≥90% | +25% |
| 查询改写覆盖率 | ~35% | ≥70% | +35% |
| 系统响应时间(P99) | TBD | <2s | - |
| 多轮对话意图延续率 | N/A | ≥85% | - |
| 单元测试覆盖率 | 现有基础上 | ≥80% | - |

### 2.2 设计原则

1. **渐进式增强**: 保持向后兼容，逐步引入高级特性
2. **混合架构**: 规则引擎(快速) + LLM(精准) + RAG(泛化) 三层协作
3. **可插拔设计**: 各组件可通过配置启用/禁用
4. **可观测性**: 结构化埋点，支持在线评估

---

## 三、详细优化措施

### 3.1 架构优化: 构建三层fallback机制

**目标**: 实现 "规则层 → LLM层 → RAG增强层" 的三级fallback

#### 3.1.1 扩展 QueryEnhancer 接口

```java
public interface QueryEnhancer {

    /**
     * 增强查询
     */
    List<String> enhance(String originalQuery, QueryContext context);

    /**
     * 识别查询意图
     */
    QueryIntent recognizeIntent(String query);

    /**
     * 识别查询意图（带置信度）
     */
    default IntentRecognitionResult recognizeIntentWithConfidence(String query) {
        QueryIntent intent = recognizeIntent(query);
        return IntentRecognitionResult.builder()
                .intent(intent)
                .confidence(1.0)  // 规则匹配默认高置信度
                .method(IntentRecognitionMethod.RULE_BASED)
                .build();
    }

    /**
     * 检查是否启用
     */
    default boolean isEnabled() { return true; }
}
```

#### 3.1.2 新增 IntentRecognitionResult 类

```java
@Data
@Builder
public class IntentRecognitionResult {
    private QueryIntent intent;
    private double confidence;                    // 置信度 0.0-1.0
    private IntentRecognitionMethod method;          // 识别方法
    private String reasoning;                      // 推理过程
}

public enum IntentRecognitionMethod {
    RULE_BASED,      // 规则匹配
    LLM_CLASSIFY,    // LLM分类
    RAG_AUGMENTED    // RAG增强分类
}
```

#### 3.1.3 扩展 SelfRagEvaluator 支持置信度阈值

```java
public class SelfRagEvaluator {

    /**
     * 带置信度阈值的评估
     * - 置信度高: 直接使用结果
     * - 置信度低: 触发LLM或RAG增强
     */
    public RetrievalDecision evaluateWithThreshold(String query) {
        RetrievalDecision decision = ruleBasedEvaluation(query);

        if (decision.getConfidence() < getConfidenceThreshold()) {
            // 触发LLM评估
            return llmBasedEvaluation(query);
        }

        return decision;
    }
}
```

### 3.2 意图识别增强: 二级意图体系

#### 3.2.1 扩展 QueryIntent 枚举

```java
public enum QueryIntent {

    // 一级意图: 知识检索类
    FACTS("知识检索"),
    PROCEDURE("流程检索"),
    COMPARISON("比较检索"),
    ANALYSIS("分析检索"),
    LIST_RETRIEVAL("列表检索"),

    // 一级意图: 非知识检索类
    CASUAL("闲聊"),
    GREETING("问候"),
    SYSTEM_ACTION("系统操作"),
    CREATIVE("创意"),

    // 未知
    UNKNOWN("未知");

    // 新增: 二级意图分类
    private final String category;
    private final List<QueryIntent> subIntents;

    public String getCategory() { return category; }
}
```

#### 3.2.2 新增 LlmIntentClassifier 组件

```java
@Component
public class LlmIntentClassifier implements QueryEnhancer {

    @Autowired
    private ChatModel chatModel;

    private static final String CLASSIFICATION_PROMPT = """
        # Role
        你是一个专业的意图分类专家。

        # 意图定义
        - FACTS: 事实性问题（是什么、是谁、什么时候、在哪里、多少）
        - PROCEDURE: 流程性问题（如何、怎么、步骤、流程、方法）
        - COMPARISON: 比较性问题（区别、比较、差异）
        - ANALYSIS: 分析性问题（为什么、原因、影响）
        - LIST_RETRIEVAL: 列表检索（有哪些、列举）
        - CASUAL: 闲聊（问候、天气）
        - SYSTEM_ACTION: 系统操作（查询状态、执行操作）
        - CREATIVE: 创意问题（建议、想象）
        - UNKNOWN: 未知

        # 任务
        分析以下用户查询，判断其意图类型。

        # Output Format (JSON)
        {
          "intent": "意图类型",
          "confidence": 0.95,
          "reasoning": "推理过程"
        }
        """;

    @Override
    public IntentRecognitionResult recognizeIntentWithConfidence(String query) {
        if (chatModel == null) {
            return ruleBasedRecognition(query);
        }

        try {
            String prompt = CLASSIFICATION_PROMPT + "\n\n用户查询：" + query;
            ChatResponse response = chatModel.chat(UserMessage.from(prompt));
            return parseLlmResponse(response.aiMessage().text());
        } catch (Exception e) {
            log.warn("LLM classification failed, fallback to rule-based: {}", e.getMessage());
            return ruleBasedRecognition(query);
        }
    }
}
```

### 3.3 查询改写增强: 结构化改写策略

#### 3.3.1 新增 QueryRewriter 接口

```java
public interface QueryRewriter {

    /**
     * 改写查询
     * @param query 原始查询
     * @param intent 识别到的意图
     * @param context 查询上下文
     * @return 改写后的查询
     */
    RewriteResult rewrite(String query, QueryIntent intent, QueryContext context);

    /**
     * 支持的意图类型
     */
    Set<QueryIntent> supportedIntents();
}

@Data
@Builder
public class RewriteResult {
    private String rewrittenQuery;
    private double confidence;
    private RewriteStrategy strategy;  // SYNONYMY_EXPANSION, PRONOUN_RESOLUTION, STRUCTURAL_TRANSFORM, etc.
    private String explanation;
}

public enum RewriteStrategy {
    SYNONYMY_EXPANSION,      // 同义扩展
    PRONOUN_RESOLUTION,       // 指代消解
    STRUCTURAL_TRANSFORM,     // 结构化转换
    TEMPORAL_COMPLETION,     // 时间补全
    ENTITY_COMPLETION,       // 实体补全
    CONTEXT_COMPLETION       // 上下文补全
}
```

#### 3.3.2 新增 StructuralQueryRewriter 实现

```java
@Component
@ConditionalOnProperty(name = "rag.query-enhancer.structural-transform-enabled", havingValue = "true")
public class StructuralQueryRewriter implements QueryRewriter {

    @Override
    public RewriteResult rewrite(String query, QueryIntent intent, QueryContext context) {
        return switch (intent) {
            case FACTS -> rewriteFactsQuery(query, context);
            case PROCEDURE -> rewriteProcedureQuery(query, context);
            case COMPARISON -> rewriteComparisonQuery(query, context);
            case ANALYSIS -> rewriteAnalysisQuery(query, context);
            case LIST_RETRIEVAL -> rewriteListQuery(query, context);
            default -> defaultRewrite(query);
        };
    }

    private RewriteResult rewriteFactsQuery(String query, QueryContext context) {
        // FACTS: 补全实体信息，提取关键属性
        // "Java是什么" -> "Java编程语言定义、特点、应用场景"
        String rewritten = expandEntityQuery(query, context);
        return RewriteResult.builder()
                .rewrittenQuery(rewritten)
                .confidence(0.85)
                .strategy(RewriteStrategy.ENTITY_COMPLETION)
                .explanation("事实性查询：补充实体相关属性")
                .build();
    }

    private RewriteResult rewriteProcedureQuery(String query, QueryContext context) {
        // PROCEDURE: 提取步骤关键词，规范化流程描述
        // "如何安装Docker" -> "Docker安装步骤教程指南"
        String rewritten = normalizeProcedureQuery(query, context);
        return RewriteResult.builder()
                .rewrittenQuery(rewritten)
                .confidence(0.88)
                .strategy(RewriteStrategy.STRUCTURAL_TRANSFORM)
                .explanation("流程性查询：规范化步骤描述")
                .build();
    }

    private RewriteResult rewriteComparisonQuery(String query, QueryContext context) {
        // COMPARISON: 提取对比要素，构建对比框架
        // "MySQL vs PostgreSQL" -> "MySQL和PostgreSQL区别对比数据库"
        String rewritten = buildComparisonFramework(query, context);
        return RewriteResult.builder()
                .rewrittenQuery(rewritten)
                .confidence(0.82)
                .strategy(RewriteStrategy.STRUCTURAL_TRANSFORM)
                .explanation("比较性查询：构建对比框架")
                .build();
    }

    private RewriteResult rewriteAnalysisQuery(String query, QueryContext context) {
        // ANALYSIS: 补全原因-结果关系
        // "为什么要用微服务" -> "微服务架构优点、原因、优势分析"
        String rewritten = expandCauseEffectQuery(query, context);
        return RewriteResult.builder()
                .rewrittenQuery(rewritten)
                .confidence(0.80)
                .strategy(RewriteStrategy.CONTEXT_COMPLETION)
                .explanation("分析性查询：补全因果关系")
                .build();
    }

    @Override
    public Set<QueryIntent> supportedIntents() {
        return EnumSet.of(FACTS, PROCEDURE, COMPARISON, ANALYSIS, LIST_RETRIEVAL);
    }
}
```

### 3.4 上下文管理增强: 对话状态跟踪

#### 3.4.1 新增 ConversationContext 类

```java
@Data
public class ConversationContext {

    private String sessionId;
    private String kbUuid;
    private Long userId;

    // 当前状态
    private QueryIntent currentIntent;
    private Map<String, String> slots;  // 槽位: entityName -> extractedValue
    private String lastTopic;

    // 历史
    private List<DialogTurn> dialogHistory;
    private List<String> mentionedEntities;  // 提到的实体列表

    // 置信度
    private double intentConfidence;

    public void update(IntentRecognitionResult intentResult, QueryContext queryContext) {
        this.currentIntent = intentResult.getIntent();
        this.intentConfidence = intentResult.getConfidence();
        this.slots = extractSlots(queryContext.getOriginalQuery(), intentResult.getIntent());
    }

    public String resolvePronoun(String pronoun) {
        // 从mentionedEntities中找到指代对象
        if ("它".equals(pronoun) && !mentionedEntities.isEmpty()) {
            return mentionedEntities.get(mentionedEntities.size() - 1);
        }
        return pronoun;
    }
}

@Data
public class DialogTurn {
    private String role;          // user/assistant
    private String content;
    private QueryIntent intent;
    private LocalDateTime timestamp;
}
```

#### 3.4.2 新增 ConversationContextManager

```java
@Component
public class ConversationContextManager {

    @Autowired
    private RedisTemplate<String, ConversationContext> contextRedisTemplate;

    private static final Duration CONTEXT_TTL = Duration.ofMinutes(30);

    public ConversationContext getOrCreateContext(String sessionId, String kbUuid, Long userId) {
        String key = buildKey(sessionId, kbUuid);

        ConversationContext context = contextRedisTemplate.opsForValue().get(key);
        if (context == null) {
            context = ConversationContext.builder()
                    .sessionId(sessionId)
                    .kbUuid(kbUuid)
                    .userId(userId)
                    .slots(new HashMap<>())
                    .dialogHistory(new ArrayList<>())
                    .mentionedEntities(new ArrayList<>())
                    .build();
        }

        return context;
    }

    public void updateContext(String sessionId, String kbUuid, ConversationContext context) {
        String key = buildKey(sessionId, kbUuid);
        contextRedisTemplate.opsForValue().set(key, context, CONTEXT_TTL);
    }

    public void addTurn(String sessionId, String kbUuid, String role, String content,
                        QueryIntent intent) {
        ConversationContext context = getOrCreateContext(sessionId, kbUuid, null);
        context.getDialogHistory().add(DialogTurn.builder()
                .role(role)
                .content(content)
                .intent(intent)
                .timestamp(LocalDateTime.now())
                .build());

        // 提取并存储实体
        extractAndStoreEntities(content, context);

        updateContext(sessionId, kbUuid, context);
    }

    private void extractAndStoreEntities(String content, ConversationContext context) {
        // 使用NER或简单规则提取实体
        // 存入mentionedEntities供指代消解使用
    }
}
```

### 3.5 可观测性增强: 结构化埋点

#### 3.5.1 新增 QueryEnhanceMetrics 指标收集

```java
@Component
@ConditionalOnProperty(name = "rag.metrics.enabled", havingValue = "true")
public class QueryEnhanceMetrics {

    private final MeterRegistry meterRegistry;

    // 意图识别指标
    private final Counter intentRecognitionTotal;
    private final Counter intentRecognitionSuccess;
    private final Timer intentRecognitionLatency;

    // 查询改写指标
    private final Counter queryRewriteTotal;
    private final Timer queryRewriteLatency;
    private final DistributionSummary rewriteConfidence;

    public QueryEnhanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        intentRecognitionTotal = Counter.builder("rag.intent.recognition.total")
                .description("Total intent recognition requests")
                .register(meterRegistry);

        intentRecognitionSuccess = Counter.builder("rag.intent.recognition.success")
                .description("Successful intent recognition")
                .register(meterRegistry);

        intentRecognitionLatency = Timer.builder("rag.intent.recognition.latency")
                .description("Intent recognition latency")
                .register(meterRegistry);
    }

    public void recordIntentRecognition(QueryIntent intent, double confidence,
                                        IntentRecognitionMethod method, long latencyMs) {
        intentRecognitionTotal.increment();
        if (confidence >= 0.9) {
            intentRecognitionSuccess.increment();
        }

        // 记录各维度标签
        Tags tags = Tags.of(
                "intent", intent.name(),
                "method", method.name(),
                "confidence_level", getConfidenceLevel(confidence)
        );

        meterRegistry.timer("rag.intent.recognition.latency", tags)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordQueryRewrite(String originalQuery, String rewrittenQuery,
                                   RewriteStrategy strategy, double confidence) {
        queryRewriteTotal.increment();

        Tags tags = Tags.of("strategy", strategy.name());
        meterRegistry.summary("rag.query.rewrite.confidence", tags)
                .record(confidence);
    }
}
```

#### 3.5.2 新增评估日志格式

```java
// 结构化评估日志，便于后续分析
log.info("QueryEnhance metrics: " +
        "originalQuery={}, " +
        "intent={}, " +
        "confidence={}, " +
        "method={}, " +
        "rewrittenQueries={}, " +
        "processingTimeMs={}",
        originalQuery,
        intentResult.getIntent(),
        intentResult.getConfidence(),
        intentResult.getMethod(),
        enhancedQueries.size(),
        processingTime);
```

---

## 四、实施步骤

### 4.1 阶段一: 基础增强（1周）

**目标**: 提升意图识别准确率至80%+

| 任务 | 负责人 | 产出物 |
|:---|:---|:---|
| 扩展QueryIntent为二级体系 | 开发 | QueryIntent V2枚举 |
| 实现LlmIntentClassifier | 开发 | LlmIntentClassifier.java |
| 扩展SelfRagEvaluator支持置信度 | 开发 | SelfRagEvaluator V2 |
| 新增IntentRecognitionResult | 开发 | IntentRecognitionResult.java |
| 编写单元测试 | 开发 | QueryEnhancerTest V2 |

### 4.2 阶段二: 上下文管理（1周）

**目标**: 支持多轮对话和槽位填充

| 任务 | 负责人 | 产出物 |
|:---|:---|:---|
| 实现ConversationContext | 开发 | ConversationContext.java |
| 实现ConversationContextManager | 开发 | ConversationContextManager.java |
| 扩展QueryContext支持session | 开发 | QueryContext V2 |
| 集成到KnowledgeBaseQaService | 开发 | KnowledgeBaseQaService V2 |
| 多轮对话测试 | 开发 | IntegrationTest |

### 4.3 阶段三: 改写增强（1周）

**目标**: 查询改写覆盖率达到70%+

| 任务 | 负责人 | 产出物 |
|:---|:---|:---|
| 实现QueryRewriter接口 | 开发 | QueryRewriter.java |
| 实现StructuralQueryRewriter | 开发 | StructuralQueryRewriter.java |
| 实现PronounResolutionRewriter | 开发 | PronounResolutionRewriter.java |
| 配置化策略选择器 | 开发 | RewriteStrategySelector |
| 改写效果评估测试 | 开发 | RewriteEvaluationTest |

### 4.4 阶段四: 可观测性（3天）

**目标**: 具备在线评估能力

| 任务 | 负责人 | 产出物 |
|:---|:---|:---|
| 实现QueryEnhanceMetrics | 开发 | QueryEnhanceMetrics.java |
| 结构化日志埋点 | 开发 | 日志格式规范 |
| Prometheus集成 | 开发 | metrics endpoint |
| Grafana仪表盘 | 开发 | Dashboard配置 |

---

## 五、预期效果与验证方法

### 5.1 量化指标验收

| 指标 | 验收标准 | 验证方法 |
|:---|:---|:---|
| 意图识别准确率 | ≥90% (基于测试集500+样本) | 测试集评估脚本 |
| 查询改写覆盖率 | ≥70% | 人工抽检1000条改写结果 |
| 系统响应时间P99 | <2s | JMeter压测 |
| 多轮对话延续率 | ≥85% | 多轮对话测试集 |
| 单元测试覆盖率 | ≥80% | JaCoCo报告 |

### 5.2 功能验收清单

- [ ] 意图识别支持二级分类体系
- [ ] LLM分类器在识别失败时正确fallback
- [ ] 置信度低于阈值时触发LLM评估
- [ ] 多轮对话上下文正确传递
- [ ] 指代消解正确解析"它"、"这个"等
- [ ] 结构化查询转换正确生成扩展查询
- [ ] Prometheus指标正确采集和暴露
- [ ] 日志包含完整的评估链路信息

### 5.3 回归测试

- [ ] 现有QA流程不受影响
- [ ] 配置开关可正确禁用各功能模块
- [ ] ChatModel为null时系统正常运行
- [ ] 异常情况正确fallback到兜底策略

---

## 六、资源需求

### 6.1 开发资源

| 阶段 | 工作量 | 人员 |
|:---|:---|:---|
| 阶段一 | 5人日 | 1人 |
| 阶段二 | 5人日 | 1人 |
| 阶段三 | 5人日 | 1人 |
| 阶段四 | 2人日 | 1人 |
| 测试与修复 | 3人日 | 1人 |
| **总计** | **20人日** | **1人** |

### 6.2 技术依赖

- ChatModel (LLM) - 已有
- Redis - 已有
- Prometheus/Grafana - 已有
- 无新增外部依赖

---

## 七、风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|:---|:---|:---|:---|
| LLM分类延迟影响响应时间 | 中 | 中 | 添加超时机制，异步fallback |
| 改写质量不稳定 | 中 | 中 | 人工抽检 + 配置置信度阈值 |
| 多轮对话状态管理复杂 | 低 | 高 | 渐进式引入，先支持简单场景 |
| 向后兼容性问题 | 低 | 中 | 保持接口向后兼容，新增接口带default实现 |

---

**文档版本**: v1.0
**创建时间**: 2026-06-14
**适用项目**: kesplus-knowledge P4优化
