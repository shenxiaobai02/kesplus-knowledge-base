package com.kes.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能文档分割器
 * <p>
 * 根据文档类型自动选择最佳分割策略，支持多种文档格式。
 * 配置化参数包括分片大小、重叠大小等。
 * </p>
 */
@Slf4j
@Component
public class SmartDocumentSplitter {

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_CHUNK_OVERLAP = 100;

    // 分片大小（字符数）- 从配置文件注入
    @Value("${rag.document.chunk-size:1000}")
    private int chunkSize = DEFAULT_CHUNK_SIZE;

    // 分片重叠大小（字符数）- 从配置文件注入
    @Value("${rag.document.chunk-overlap:100}")
    private int chunkOverlap = DEFAULT_CHUNK_OVERLAP;
    
    /**
     * 根据文档类型自动选择最佳分割策略并分割
     */
    public List<Document> split(Document document) {
        SplitStrategy strategy = detectBestStrategy(document);
        log.debug("Auto-detected strategy: {} for document", strategy);
        return split(document, strategy, getEffectiveChunkSize(), getEffectiveChunkOverlap());
    }

    /**
     * 使用指定策略分割文档
     */
    public List<Document> split(Document document, SplitStrategy strategy) {
        return split(document, strategy, getEffectiveChunkSize(), getEffectiveChunkOverlap());
    }
    
    /**
     * 获取有效的分片大小
     */
    private int getEffectiveChunkSize() {
        return chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;
    }
    
    /**
     * 获取有效的分片重叠大小
     */
    private int getEffectiveChunkOverlap() {
        return chunkOverlap > 0 ? chunkOverlap : DEFAULT_CHUNK_OVERLAP;
    }

    /**
     * 使用指定策略和参数分割文档
     */
    public List<Document> split(Document document, SplitStrategy strategy, int chunkSize, int chunkOverlap) {
        DocumentSplitter splitter = createSplitter(strategy, chunkSize, chunkOverlap);
        List<TextSegment> segments = splitter.split(document);
        return segments.stream()
                .map(s -> Document.from(s.text(), s.metadata()))
                .collect(Collectors.toList());
    }

    /**
     * 根据文档类型和内容特征检测最佳分割策略
     * 识别逻辑：
     * 1. 优先基于文件扩展名判断（快速、准确）
     * 2. 未知类型时基于内容特征智能分析（代码/结构/表格/纯文本）
     */
    private SplitStrategy detectBestStrategy(Document document) {
        if (document.metadata() == null) {
            log.debug("No metadata found, defaulting to SEMANTIC strategy");
            return SplitStrategy.SEMANTIC;
        }

        // 从元数据中获取MIME类型（Tika解析器已自动注入）
        String mimeType = document.metadata().getString("mime_type");
        String fileName = document.metadata().getString("fileName");
        if (fileName == null) {
            fileName = "";
        }
        String extension = getFileExtension(fileName);
        
        // 优先基于MIME类型判断（更可靠，防止伪造后缀）
        if (mimeType != null && !mimeType.isEmpty()) {
            SplitStrategy strategy = detectByMimeType(mimeType, extension);
            if (strategy != null) {
                log.debug("Detected by MIME type: {}, using {} strategy", mimeType, strategy);
                return strategy;
            }
        }

        // 其次基于文件扩展名判断
        return switch (extension.toLowerCase()) {
            // 结构化文档 - 按段落分割保持结构完整性
            case "pdf" -> {
                log.debug("PDF file detected, using STRUCTURAL strategy");
                yield SplitStrategy.STRUCTURAL;
            }
            case "doc", "docx" -> {
                log.debug("Word document detected, using STRUCTURAL strategy");
                yield SplitStrategy.STRUCTURAL;
            }
            case "ppt", "pptx" -> {
                log.debug("PowerPoint file detected, using STRUCTURAL strategy");
                yield SplitStrategy.STRUCTURAL;
            }
            
            // 标记语言 - 混合分割平衡结构和语义
            case "md", "markdown" -> {
                log.debug("Markdown file detected, using HYBRID strategy");
                yield SplitStrategy.HYBRID;
            }
            case "html", "htm" -> {
                log.debug("HTML file detected, using HYBRID strategy");
                yield SplitStrategy.HYBRID;
            }
            case "xml" -> {
                log.debug("XML file detected, using HYBRID strategy");
                yield SplitStrategy.HYBRID;
            }
            
            // 纯文本和笔记 - 语义分割保持连贯性
            case "txt" -> {
                log.debug("Text file detected, using SEMANTIC strategy");
                yield SplitStrategy.SEMANTIC;
            }
            case "rtf" -> {
                log.debug("RTF file detected, using SEMANTIC strategy");
                yield SplitStrategy.SEMANTIC;
            }
            
            // 代码文件 - 按行分割尊重代码结构
            case "java", "py", "js", "ts", "go", "cpp", "c", "h", "cs" -> {
                log.debug("Source code file detected ({}), using HYBRID strategy", extension);
                yield SplitStrategy.HYBRID;
            }
            
            // 配置文件 - 按行分割保持配置项完整
            case "json", "yaml", "yml", "toml", "ini", "conf", "properties" -> {
                log.debug("Config file detected ({}), using HYBRID strategy", extension);
                yield SplitStrategy.HYBRID;
            }
            
            // 数据文件 - 语义分割便于检索
            case "csv", "tsv" -> {
                log.debug("Data file detected ({}), using SEMANTIC strategy", extension);
                yield SplitStrategy.SEMANTIC;
            }
            
            // 日志文件 - 按行分割便于分析
            case "log" -> {
                log.debug("Log file detected, using HYBRID strategy");
                yield SplitStrategy.HYBRID;
            }
            
            default -> {
                // 根据内容特征智能判断
                String content = document.text();
                SplitStrategy strategy = analyzeContentAndChoose(content);
                log.debug("Unknown file type, auto-detected strategy: {} based on content analysis", strategy);
                yield strategy;
            }
        };
    }

    /**
     * 基于MIME类型检测分割策略（更可靠，防止伪造后缀）
     */
    private SplitStrategy detectByMimeType(String mimeType, String extension) {
        if (mimeType == null) {
            return null;
        }
        
        return switch (mimeType.toLowerCase()) {
            // PDF文档
            case "application/pdf" -> SplitStrategy.STRUCTURAL;
            
            // Word文档
            case "application/msword", 
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document" 
                    -> SplitStrategy.STRUCTURAL;
            
            // PowerPoint
            case "application/vnd.ms-powerpoint",
                 "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    -> SplitStrategy.STRUCTURAL;
            
            // 标记语言
            case "text/html", "text/xml", "application/xml" -> SplitStrategy.HYBRID;
            
            // 纯文本
            case "text/plain" -> {
                // 纯文本需要进一步根据扩展名或内容判断
                if (isCodeExtension(extension)) {
                    yield SplitStrategy.HYBRID; // 代码文件
                } else if (isConfigExtension(extension)) {
                    yield SplitStrategy.HYBRID; // 配置文件
                } else {
                    yield SplitStrategy.SEMANTIC; // 普通文本
                }
            }
            
            // CSV数据
            case "text/csv" -> SplitStrategy.SEMANTIC;
            
            // JSON
            case "application/json" -> SplitStrategy.HYBRID;
            
            default -> null; // 无法识别，降级到扩展名或内容分析
        };
    }

    /**
     * 分析文档内容并选择最佳策略
     */
    private SplitStrategy analyzeContentAndChoose(String content) {
        if (content == null || content.isEmpty()) {
            return SplitStrategy.SEMANTIC;
        }
        
        // 检测是否是代码
        if (isCodeContent(content)) {
            log.debug("Detected code-like content, using HYBRID strategy");
            return SplitStrategy.HYBRID;
        }
        
        // 检测是否是结构化文档
        if (hasClearStructure(content)) {
            log.debug("Detected structured content, using STRUCTURAL strategy");
            return SplitStrategy.STRUCTURAL;
        }
        
        // 检测是否是表格数据
        if (isTabularData(content)) {
            log.debug("Detected tabular data, using SEMANTIC strategy");
            return SplitStrategy.SEMANTIC;
        }
        
        // 默认使用语义分割
        return SplitStrategy.SEMANTIC;
    }

    /**
     * 检测内容是否像代码
     */
    private boolean isCodeContent(String content) {
        // 检测代码特征：大括号、分号、关键字等
        int braceCount = countOccurrences(content, "{") + countOccurrences(content, "}");
        int semicolonCount = countOccurrences(content, ";");
        int codeKeywords = countCodeKeywords(content);
        
        // 如果有明显的代码特征
        return braceCount > 5 && semicolonCount > 3 && codeKeywords > 3;
    }

    /**
     * 计算代码关键字数量
     */
    private int countCodeKeywords(String content) {
        String[] keywords = {"public", "private", "protected", "class", "interface", "function", 
                            "import", "package", "void", "return", "if", "else", "for", "while"};
        int count = 0;
        for (String keyword : keywords) {
            count += countOccurrences(content, "\n" + keyword + " ");
            count += countOccurrences(content, "\n" + keyword + "\t");
        }
        return count;
    }

    /**
     * 检测内容是否是表格数据
     */
    private boolean isTabularData(String content) {
        // 检测CSV或TSV格式
        String[] lines = content.split("\n");
        if (lines.length < 2) {
            return false;
        }
        
        // 检查前几行是否有逗号或制表符分隔的多列数据
        int commaCount = 0;
        int tabCount = 0;
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            if (lines[i].contains(",")) commaCount++;
            if (lines[i].contains("\t")) tabCount++;
        }
        
        return commaCount >= 3 || tabCount >= 3;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * 判断是否为代码文件扩展名
     */
    private boolean isCodeExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        return List.of("java", "py", "js", "ts", "go", "cpp", "c", "h", "cs", "rb", "php")
                .contains(extension.toLowerCase());
    }

    /**
     * 判断是否为配置文件扩展名
     */
    private boolean isConfigExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        return List.of("json", "yaml", "yml", "toml", "ini", "conf", "properties", "xml")
                .contains(extension.toLowerCase());
    }

    /**
     * 检测文档是否有清晰的结构（标题、章节等）
     */
    private boolean hasClearStructure(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        // 检测Markdown风格的标题
        int markdownHeaders = countOccurrences(content, "\n#");
        
        // 检测大写标题行（英文文档）
        int uppercaseLines = countUppercaseLines(content);
        
        // 检测数字编号的章节（如 "1.", "Chapter 1", "第一节"等）
        int numberedSections = countNumberedSections(content);
        
        // 如果有任何一种结构化特征明显，则认为是结构化文档
        return markdownHeaders >= 3 || uppercaseLines >= 3 || numberedSections >= 2;
    }

    /**
     * 计算字符串中模式出现的次数
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * 计算大写标题行的数量
     */
    private int countUppercaseLines(String text) {
        String[] lines = text.split("\n");
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            // 短的全大写行可能是标题
            if (!trimmed.isEmpty() && trimmed.equals(trimmed.toUpperCase()) && 
                trimmed.length() > 5 && trimmed.length() < 100 && 
                Character.isLetter(trimmed.charAt(0))) {
                count++;
            }
        }
        return count;
    }

    /**
     * 计算编号章节的数量
     */
    private int countNumberedSections(String text) {
        // 匹配 "1.", "1.1", "Chapter 1", "第一节" 等模式
        String[] patterns = {"\\d+\\.", "Chapter\\s+\\d+", "第[一二三四五六七八九十百]+章"};
        int total = 0;
        for (String pattern : patterns) {
            total += countOccurrences(text, pattern);
        }
        return total;
    }

    /**
     * 根据不同的策略创建文档分割器
     * - SEMANTIC: 递归分割（LangChain4j官方推荐），保持语义连贯性，适合通用场景
     * - STRUCTURAL: 按段落分割，尊重文档结构，适合结构化文档
     * - HYBRID: 按行分割，平衡语义和结构，适合混合内容
     * 
     * 设计亮点：
     * 1. 递归分割器采用分层降级策略：段落→行→句子→单词→字符
     * 2. 支持Token计数而非字符计数，完美适配LLM上下文窗口
     * 3. 内置重叠逻辑，解决边界语义断裂问题
     */
    private DocumentSplitter createSplitter(SplitStrategy strategy, int chunkSize, int chunkOverlap) {
        return switch (strategy) {
            case SEMANTIC -> {
                log.debug("Creating SEMANTIC splitter with recursive strategy, chunkSize={}, overlap={}", 
                        chunkSize, chunkOverlap);
                // 语义分割：使用递归分割器（生产首选）
                // 多层级逐级降级切分：段落(\n\n) → 单行(\n) → 句子 → 单词 → 字符
                yield DocumentSplitters.recursive(chunkSize, chunkOverlap);
            }
                
            case STRUCTURAL -> {
                log.debug("Creating STRUCTURAL splitter with paragraph strategy, chunkSize={}, overlap={}", 
                        chunkSize, chunkOverlap);
                // 结构分割：按段落分割，尊重文档的自然结构
                // 适用场景：PDF、Word等有明确章节结构的文档
                yield new DocumentByParagraphSplitter(chunkSize, chunkOverlap);
            }
                
            case HYBRID -> {
                log.debug("Creating HYBRID splitter with line strategy, chunkSize={}, overlap={}", 
                        chunkSize, chunkOverlap);
                // 混合分割：按行分割，平衡语义和结构的优点
                // 适用场景：代码文件、配置文件、日志文件
                yield new DocumentByLineSplitter(chunkSize, chunkOverlap);
            }
        };
    }

    /**
     * 基于Token估算的分割方法
     */
    public List<Document> splitWithTokenEstimation(Document document, int maxTokens) {
        List<TextSegment> segments = DocumentSplitters.recursive(maxTokens, maxTokens / 10).split(document);
        return segments.stream()
                .map(s -> Document.from(s.text(), s.metadata()))
                .collect(Collectors.toList());
    }
}
