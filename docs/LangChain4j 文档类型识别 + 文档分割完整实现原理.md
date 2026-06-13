# LangChain4j 文档类型识别 + 文档分割完整实现原理

整体流水线分为两大独立模块：**文档类型识别&加载解析层（Loader+Parser）**、**文档切分层（DocumentSplitter）**，两层解耦：识别输出标准`Document`对象，分割只依赖`Document`，不感知原始文件格式。

## 一、文档类型识别逻辑完整实现

### 1. 核心分层架构

```
原始文件流/File → DocumentLoader（加载器） → DocumentParser（解析器，负责格式识别） → 标准化Document(文本+metadata)
```

识别逻辑全部封装在`DocumentParser`，Loader只负责读取文件字节流，不做格式判断。

### 2. 识别模式：

#### 全自动格式识别（生产常用）

**ApacheTikaDocumentParser** 是LangChain4j唯一内置自动识别组件，依托Apache Tika实现全格式探测。

##### 识别底层原理（Tika）

1. **第一步：后缀名初筛**：读取文件名后缀（`.pdf`/`.docx`/`.html`）快速预判MIME类型；
2. **第二步：文件魔数校验（二进制签名）**：读取文件头部字节，PDF以`%PDF-`、DOCX是ZIP包结构、JPG/JPEG固定头标记，绕过篡改后缀的恶意文件；
3. **第三步：内容特征二次校验**：HTML识别`<html>`标签、JSON识别`{`/`[`；
4. Tika内置`AutoDetectParser`自动匹配对应解析器，不需要代码分支判断格式。

##### LangChain4j封装流程

```java
DocumentParser parser = new ApacheTikaDocumentParser();
// 传入文件流，自动识别格式并解析
Document doc = parser.parse(Files.newInputStream(filePath));
```

识别结果会存入`metadata`，key为`mime_type`，可直接读取识别出的文档类型。

### 3. FileSystemDocumentLoader一站式封装（最常用入口）

`FileSystemDocumentLoader.loadDocuments()`内部逻辑：

1. 遍历文件夹所有文件；
2. 默认绑定`ApacheTikaDocumentParser`自动识别每一个文件；
3. 批量输出`List<Document>`，一行代码完成多格式批量加载+识别+解析；
4. 元数据自动注入：`file_name`、`absolute_path`、`last_modified`等。

### 4. 识别异常处理机制

- 加密PDF、损坏Office文件：Parser抛出`DocumentParseException`；
- 不支持的二进制格式（exe、zip）：Tika识别后返回空文本或抛出解析异常；
- 可自定义`Parser`扩展新增自定义文件类型识别规则。

## 二、文档分割（Chunk）完整实现逻辑

### 1. 顶层接口契约

```java
public interface DocumentSplitter {
    List<TextSegment> split(Document document);
    List<TextSegment> splitAll(List<Document> documents);
}
```

输入统一`Document`，输出`TextSegment`（分片+继承原文档全部metadata），和原始文件格式完全解耦。

### 2. 所有内置分割器实现原理分类

#### 类别1：结构化边界分割（按语义天然分隔符）

##### （1）段落分割 DocumentByParagraphSplitter

- 分隔规则：连续\*\*2个及以上换行符`\n\n`\*\*切分段落；
- 执行逻辑：
  1. 全文切分为多个段落片段；
  2. 循环拼接段落，直到达到`maxSegmentSize`（字符/token上限）；
  3. 超长单个段落自动调用子分割器二次切分；
  4. 支持`overlap`上下文重叠，相邻分片尾部冗余一段文本。

##### （2）行分割 DocumentByLineSplitter

- 分隔符：单行`\n`；
- 适用日志、CSV、代码文件；单行超长自动降级分句器拆分。

##### （3）句子分割 DocumentBySentenceSplitter

- 底层依赖Apache OpenNLP英文分句模型；
- 依据句号`./?/!`等标点拆分完整句子；
- 短板：原生不支持中文分句，需替换第三方分词器。

#### 类别2：固定长度硬分割（无语义，兜底方案）

1. **DocumentByCharacterSplitter**：纯字符计数截断，无视语义边界；
2. **DocumentByWordSplitter**：按空格分词，单词数量聚合到上限；
   仅超长无结构化文本兜底使用，检索精度差。

#### 类别3：自定义规则分割

- **DocumentByRegexSplitter**：传入正则表达式切分，支持自定义分隔符（###、##标题标记）；
- 超长单片段支持嵌套子分割器递归拆分。

#### 类别4：递归分割器（生产首选，`DocumentSplitters.recursive()`）

LangChain4j官方推荐默认分片器，**多层级逐级降级切分**，完整实现逻辑：

1. 预设分层分隔优先级（从粗到细）：
   `段落(\n\n) → 单行(\n) → 句子 → 单词 → 字符`
2. 第一轮：按最高优先级段落拆分，批量拼接段落凑满`maxTokenSize`；
3. 若单个段落本身超长：自动降级到下一级分隔符（行）再次拆分；
4. 逐级降级，直到分片长度满足token上限；
5. 叠加`overlapTokens`：后一个分片开头复用前一个分片末尾N个token，解决边界语义断裂问题；
6. 支持传入模型专属`Tokenizer`（OpenAI/HuggingFace分词器），**按真实token计数**而非字符计数，完美适配LLM上下文窗口限制。

### 3. 分片通用核心执行步骤（所有Splitter统一流程）

1. 从`Document`取出完整`text`正文，复制metadata；
2. 按分隔规则切分为最小原子单元（段落/句子/行）；
3. 正向聚合原子单元，累加token/字符数，不超过上限就持续合并；
4. 达到上限后生成一个`TextSegment`，记录重叠偏移；
5. 剩余原子单元继续循环聚合；
6. 超长单个原子单元启用嵌套子分割器二次切分；
7. 所有分片继承原文档metadata，新增分片偏移元数据。

### 4. 重叠（Overlap）实现细节

- 每个分片尾部保留前一分片末尾`N`个token；
- 原理：检索时查询词落在分片边界，不会因为切分丢失上下文；
- 经验值：overlap = chunkSize \* 10%\~20%；
- 递归分割器原生内置重叠逻辑，无需手动拼接字符串。

## 三、完整端到端数据流串联

```
PDF/DOCX/TXT文件
  ↓（文件魔数+后缀识别）
ApacheTika自动识别文档类型 + 解析文本
  ↓
标准化Document(text+metadata)
  ↓（递归语义分割+token计数+重叠）
DocumentSplitter → List<TextSegment>
  ↓
Embedding向量化 → 存入向量库
```

## 四、关键设计亮点总结

1. **两层完全解耦**：Parser只负责格式识别解析，Splitter只做文本切分，新增文件格式只扩展Parser，不改动分割逻辑；
2. **识别双模式兼容**：手动指定Parser适合固定格式场景，Tika自动探测适配多文件批量导入；
3. **分层递归切分**：优先保留完整语义单元，迫不得已才硬截断，大幅提升RAG召回准确率；
4. **Token感知分片**：绑定模型Tokenizer，不再用字符粗略估算分片大小，适配不同LLM上下文限制；
5. **元数据全程透传**：原始文件名、MIME类型、分片偏移完整保留，溯源可定位原文位置。

## 补充常见坑点

1. Tika自动识别依赖完整依赖包，缺少对应格式解析依赖会识别失败；
2. 原生分句器OpenNLP仅英文可用，中文必须替换HanLP、jieba分词器；
3. 字符分片和token分片差异极大，生产环境禁止使用字符计数做分片上限。

