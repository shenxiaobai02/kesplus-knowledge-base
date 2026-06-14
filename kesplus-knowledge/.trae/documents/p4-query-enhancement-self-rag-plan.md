# 阶段P4实施计划：查询增强与Self-RAG

## 一、概述

本计划旨在实现查询预处理和自评估机制，提升RAG系统的智能性。根据设计文档和现有代码结构，将创建完整的查询增强模块并集成到现有QA流程中。

## 二、现状分析

### 2.1 现有代码结构

**关键文件**：
- [KnowledgeBaseQaService.java](file:///e:\Desktop\Java\aideepin\kesplus-knowledge-base\kesplus-knowledge\src\main\java\com\kes\service\KnowledgeBaseQaService.java) - QA服务，接收用户查询并返回答案
- [EmbeddingRagService.java](file:///e:\Desktop\Java\aideepin\kesplus-knowledge-base\kesplus-knowledge\src\main\java\com\kes\service\EmbeddingRagService.java) - 向量检索服务
- [HybridRetriever.java](file:///e:\Desktop\Java\aideepin\kesplus-knowledge-base\kesplus-knowledge\src\main\java\com\kes\service\HybridRetriever.java) - 混合检索器
- [RagConfig.java](file:///e:\Desktop\Java\aideepin\kesplus-knowledge-base\kesplus-knowledge\src\main\java\com\kes\config\RagConfig.java) - RAG配置类
- [LangChain4jConfig.java](file:///e:\Desktop\Java\aideepin\kesplus-knowledge-base\kesplus-knowledge\src\main\java\com\kes\config\LangChain4jConfig.java) - LangChain4j配置

**现有模式**：
- 使用 `@Slf4j`, `@Service` 注解服务类
- 配置类使用 `@ConfigurationProperties(prefix = "xxx")`
- 实体类使用 `@Data`, `@TableName`, `@TableField` 注解
- 错误处理使用 `ErrorCode` 枚举和 `BaseException`
- 使用 `ThreadContext` 获取当前用户信息

### 2.2 现有QA流程

```
用户问题 → KnowledgeBaseQaService.qa()
    → 权限校验
    → 获取EmbeddingModel
    → 混合检索/向量检索
    → 重排序
    → 构建上下文
    → LLM生成答案
    → 返回响应
```

## 三、实施方案

### 3.1 新增实体类和数据结构

#### 3.1.1 QueryIntent 枚举

**文件**: `src/main/java/com/kes/enums/QueryIntent.java`

```java
public enum QueryIntent {
    FACTS,           // 事实性问题（who, what, when, where）
    PROCEDURE,       // 流程性问题（how to, steps）
    COMPARISON,      // 比较性问题（difference, compare）
    ANALYSIS,        // 分析性问题（why, cause, effect）
    CREATIVE,        // 创意性问题（suggest, imagine）
    UNKNOWN          // 未知意图
}
```

#### 3.1.2 RetrievalDecision 类

**文件**: `src/main/java/com/kes/entity/RetrievalDecision.java`

```java
@Data
public class RetrievalDecision {
    private boolean needRetrieval;      // 是否需要检索
    private String reason;               // 判断理由
    private List<String> keywords;      // 检索关键词
    private QueryIntent intent;          // 识别到的意图
    private List<String> enhancedQueries; // 增强后的查询列表
}
```

#### 3.1.3 QueryContext 类

**文件**: `src/main/java/com/kes/dto/QueryContext.java`

```java
@Data
public class QueryContext {
    private String kbUuid;              // 知识库UUID
    private String originalQuery;        // 原始查询
    private List<String> historyQuestions; // 历史问题
    private ThreadContext.UserContext user; // 当前用户
    private QueryIntent intent;          // 识别到的意图
}
```

### 3.2 QueryEnhancer 接口和实现

#### 3.2.1 QueryEnhancer 接口

**文件**: `src/main/java/com/kes/rag/QueryEnhancer.java`

```java
public interface QueryEnhancer {
    /**
     * 增强查询：将原始查询改写、扩展为多个查询
     */
    List<String> enhance(String originalQuery, QueryContext context);

    /**
     * 识别查询意图
     */
    QueryIntent recognizeIntent(String query);
}
```

#### 3.2.2 DefaultQueryEnhancer 实现

**文件**: `src/main/java/com/kes/rag/DefaultQueryEnhancer.java`

**核心逻辑**：
1. **意图识别**：基于规则+关键词匹配识别查询意图
2. **查询改写**：将模糊问题转化为精确查询
3. **查询扩展**：添加同义词、相关概念

**意图识别规则**：
- FACTS: 含"是谁/是什么/什么时候/在哪里"等关键词
- PROCEDURE: 含"如何/步骤/流程/方法"等关键词
- COMPARISON: 含"区别/比较/不同"等关键词
- ANALYSIS: 含"为什么/原因/影响"等关键词
- CREATIVE: 含"建议/想象/假如"等关键词

### 3.3 SelfRagEvaluator 实现

**文件**: `src/main/java/com/kes/rag/SelfRagEvaluator.java`

**核心逻辑**：
1. 使用LLM判断是否需要检索
2. 生成检索关键词
3. 返回RetrievalDecision

**评估Prompt模板**：
```
请判断以下问题是否需要检索知识库来回答。

问题：{query}

判断标准：
- 需要检索：问题涉及具体事实、数据、流程等需要从知识库获取的信息
- 不需要检索：问题可以基于常识回答，或涉及主观判断、创意创作

回答格式（JSON）：
{
  "needRetrieval": true/false,
  "reason": "判断理由",
  "keywords": ["关键词1", "关键词2"],
  "intent": "FACTS/PROCEDURE/COMPARISON/ANALYSIS/CREATIVE/UNKNOWN"
}
```

### 3.4 配置扩展

#### 3.4.1 RagConfig 扩展

**修改文件**: `src/main/java/com/kes/config/RagConfig.java`

新增配置项：
```java
@Data
public static class QueryEnhancerConfig {
    private Boolean enabled = true;                    // 是否启用查询增强
    private Boolean enableQueryRewrite = true;         // 是否启用查询改写
    private Boolean enableQueryExpansion = true;        // 是否启用查询扩展
    private Boolean enableIntentRecognition = true;     // 是否启用意图识别
    private Integer maxEnhancedQueries = 3;              // 最大增强查询数量
    private Integer selfRagThreshold = 70;               // Self-RAG评分阈值
}
```

### 3.5 集成到QA流程

#### 3.5.1 KnowledgeBaseQaService 改造

**修改方法**：`qa()` 和 `streamQa()` 方法

**新增逻辑**：
1. 在检索前调用 `SelfRagEvaluator.evaluate()`
2. 如果 `needRetrieval=false`，直接基于常识回答
3. 如果 `needRetrieval=true`，调用 `QueryEnhancer.enhance()` 获取增强查询
4. 使用增强后的查询进行检索

**修改后的流程**：
```
用户问题 → SelfRagEvaluator.evaluate()
    ├── needRetrieval=false → 直接生成答案（无需检索）
    │
    └── needRetrieval=true → QueryEnhancer.enhance()
        → 混合检索/向量检索
        → 重排序
        → 构建上下文
        → LLM生成答案
```

### 3.6 新增文件清单

| 序号 | 文件路径 | 描述 |
|------|----------|------|
| 1 | `src/main/java/com/kes/enums/QueryIntent.java` | 查询意图枚举 |
| 2 | `src/main/java/com/kes/entity/RetrievalDecision.java` | 检索决策实体 |
| 3 | `src/main/java/com/kes/dto/QueryContext.java` | 查询上下文DTO |
| 4 | `src/main/java/com/kes/rag/QueryEnhancer.java` | 查询增强器接口 |
| 5 | `src/main/java/com/kes/rag/DefaultQueryEnhancer.java` | 默认查询增强器实现 |
| 6 | `src/main/java/com/kes/rag/SelfRagEvaluator.java` | Self-RAG自评估器 |
| 7 | `src/test/java/com/kes/rag/QueryEnhancerTest.java` | 查询增强器测试 |
| 8 | `src/test/java/com/kes/rag/SelfRagEvaluatorTest.java` | 自评估器测试 |

### 3.7 修改文件清单

| 序号 | 文件路径 | 修改内容 |
|------|----------|----------|
| 1 | `src/main/java/com/kes/config/RagConfig.java` | 新增QueryEnhancerConfig内部类 |
| 2 | `src/main/java/com/kes/service/KnowledgeBaseQaService.java` | 集成查询增强和Self-RAG |
| 3 | `src/main/resources/application.yml` | 新增查询增强配置 |
| 4 | `src/main/java/com/kes/exception/ErrorCode.java` | 新增相关错误码 |

## 四、验收标准

### 4.1 功能验收

- [ ] QueryEnhancer 接口所有方法正常工作
- [ ] DefaultQueryEnhancer 能正确识别查询意图
- [ ] DefaultQueryEnhancer 能生成增强查询
- [ ] SelfRagEvaluator 能正确判断是否需要检索
- [ ] 集成到KnowledgeBaseQaService后不影响现有功能
- [ ] 配置开关可正常启用/禁用各功能

### 4.2 质量验收

- [ ] 单元测试覆盖率≥80%
- [ ] 意图识别准确率≥90%（基于测试用例）
- [ ] 代码符合现有编码规范

### 4.3 配置说明

**application.yml 新增配置**：
```yaml
rag:
  query-enhancer:
    enabled: true
    enable-query-rewrite: true
    enable-query-expansion: true
    enable-intent-recognition: true
    max-enhanced-queries: 3
    self-rag-threshold: 70
```

## 五、技术实现细节

### 5.1 意图识别算法

```java
private static final Map<QueryIntent, List<String>> INTENT_KEYWORDS = Map.of(
    QueryIntent.FACTS, Arrays.asList("是谁", "是什么", "什么时候", "在哪里", "多少", "什么是"),
    QueryIntent.PROCEDURE, Arrays.asList("如何", "怎么", "怎样", "步骤", "流程", "方法", "操作"),
    QueryIntent.COMPARISON, Arrays.asList("区别", "比较", "不同", "差异", "相比", "对比"),
    QueryIntent.ANALYSIS, Arrays.asList("为什么", "原因", "影响", "结果", "分析", "为什么"),
    QueryIntent.CREATIVE, Arrays.asList("建议", "想象", "假如", "假设", "创意", "推荐")
);
```

### 5.2 查询改写规则

1. **指代消解**：将"它"、"这个"等替换为具体实体
2. **关键词补全**：补充缺失的搜索关键词
3. **同义词扩展**：添加关键词的同义词

### 5.3 Self-RAG评估策略

1. 如果问题明显是闲聊/问候，不需检索
2. 如果问题涉及具体知识领域，需要检索
3. 如果问题可以基于常识回答，不需检索
4. 其他情况使用LLM进行评估

## 六、风险与注意事项

### 6.1 风险

1. **LLM评估准确性**：Self-RAG评估依赖LLM能力，可能存在误判
2. **性能影响**：增加LLM调用会增加响应时间

### 6.2 缓解措施

1. 添加超时机制，避免LLM调用阻塞
2. 实现fallback策略，评估失败时默认检索
3. 添加评估结果缓存，减少重复评估

---

**计划版本**: v1.0
**创建时间**: 2026-06-14
**适用阶段**: P4 - 查询增强与Self-RAG
