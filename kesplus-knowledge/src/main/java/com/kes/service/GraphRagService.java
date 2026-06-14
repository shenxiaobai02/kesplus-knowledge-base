package com.kes.service;

import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseGraphNode;
import com.kes.entity.KnowledgeBaseGraphEdge;
import com.kes.entity.GraphRetrievalResult;
import com.kes.graph.GraphStorage;
import com.kes.util.JsonUtil;
import com.kes.util.UuidUtil;
import dev.langchain4j.data.document.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图谱检索服务
 * 实现文档图谱化索引和图谱检索功能
 */
@Slf4j
@Service
public class GraphRagService {

    @Autowired
    private GraphStorage graphStorage;

    // ==================== 配置注入 ====================
    
    /**
     * CONTAINS关系权重 - 文档包含段落关系，重要性最高
     */
    @Value("${rag.graph.relation.contains-weight:1.0}")
    private double containsRelationWeight;

    /**
     * KEYWORD关系基础权重 - 段落提及关键词关系
     */
    @Value("${rag.graph.relation.keyword-weight:0.5}")
    private double keywordRelationWeight;

    /**
     * SIMILAR_TO关系基础权重 - 段落相似关系
     */
    @Value("${rag.graph.relation.similar-weight:0.3}")
    private double similarRelationWeight;

    /**
     * 相似关系相似度阈值 - 低于此阈值不创建相似关系
     */
    @Value("${rag.graph.similarity-threshold:0.2}")
    private double similarityThreshold;

    /**
     * 关键词提取数量限制
     */
    @Value("${rag.graph.keyword-limit:8}")
    private int keywordLimit;

    /**
     * 相关节点分数衰减基础系数
     */
    @Value("${rag.graph.decay-factor:0.8}")
    private double defaultDecayFactor;

    /**
     * 图谱索引：将文档转换为图谱结构
     *
     * @param kb        知识库
     * @param documents 文档列表
     */
    @Transactional
    public void indexGraph(KnowledgeBase kb, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("No documents to index for kb: {}", kb.getUuid());
            return;
        }

        List<KnowledgeBaseGraphNode> nodes = new ArrayList<>();
        List<KnowledgeBaseGraphEdge> edges = new ArrayList<>();
        Map<String, KnowledgeBaseGraphNode> segmentNodes = new HashMap<>();

        // 创建文档节点
        KnowledgeBaseGraphNode docNode = createDocumentNode(kb);
        nodes.add(docNode);

        // 为每个文档段落创建节点
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            KnowledgeBaseGraphNode segmentNode = createSegmentNode(kb, doc, i);
            nodes.add(segmentNode);
            segmentNodes.put(segmentNode.getUuid(), segmentNode);

            // 创建文档包含段落的关系
            KnowledgeBaseGraphEdge containsEdge = createContainsEdge(kb, docNode.getUuid(), segmentNode.getUuid());
            edges.add(containsEdge);

            // 提取关键词并创建关键词节点
            List<String> keywords = extractKeywords(doc.text());
            for (String keyword : keywords) {
                KnowledgeBaseGraphNode keywordNode = createKeywordNode(kb, keyword);
                nodes.add(keywordNode);

                // 创建段落包含关键词的关系
                KnowledgeBaseGraphEdge keywordEdge = createKeywordEdge(kb, segmentNode.getUuid(), keywordNode.getUuid(), keyword);
                edges.add(keywordEdge);
            }
        }

        // 创建段落之间的相似关系（基于内容相似度）
        createSimilarEdges(kb, segmentNodes, edges);

        // 批量存储节点和关系
        graphStorage.batchCreateNodes(nodes);
        graphStorage.batchCreateEdges(edges);

        log.info("Indexed {} nodes and {} edges for kb: {}", nodes.size(), edges.size(), kb.getUuid());
    }

    /**
     * 图谱检索：基于节点关系进行检索
     *
     * @param kb        知识库
     * @param query     查询内容
     * @param maxResults 最大返回数量
     * @return 检索结果列表
     */
    public List<GraphRetrievalResult> retrieve(KnowledgeBase kb, String query, int maxResults) {
        // 先搜索匹配的节点
        List<KnowledgeBaseGraphNode> matchedNodes = graphStorage.searchNodes(kb.getUuid(), query, maxResults);
        
        List<GraphRetrievalResult> results = new ArrayList<>();
        
        for (KnowledgeBaseGraphNode node : matchedNodes) {
            // 计算分数（基于匹配程度）
            double score = calculateMatchScore(query, node.getContent());
            
            GraphRetrievalResult result = GraphRetrievalResult.fromNode(node, score, 0);
            results.add(result);
            
            // 获取相关节点（扩展检索范围）
            if (results.size() < maxResults) {
                expandRelatedNodes(node, results, maxResults - results.size());
            }
        }

        // 按分数排序并限制数量
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results.size() > maxResults ? results.subList(0, maxResults) : results;
    }

    /**
     * 扩展相关节点
     */
    private void expandRelatedNodes(KnowledgeBaseGraphNode node, List<GraphRetrievalResult> results, int limit) {
        List<KnowledgeBaseGraphNode> relatedNodes = graphStorage.getRelatedNodes(node.getUuid(), 2, limit);
        
        for (KnowledgeBaseGraphNode relatedNode : relatedNodes) {
            if (!containsNode(results, relatedNode.getUuid())) {
                // 根据节点类型计算衰减系数
                double decayFactor = getDecayFactor(node.getNodeType(), relatedNode.getNodeType());
                double baseScore = calculateMatchScore(node.getContent(), relatedNode.getContent());
                double relatedScore = baseScore * decayFactor;
                
                GraphRetrievalResult relatedResult = GraphRetrievalResult.fromNode(relatedNode, relatedScore, 1);
                results.add(relatedResult);
            }
        }
    }

    /**
     * 根据节点类型计算衰减系数
     */
    private double getDecayFactor(String sourceType, String targetType) {
        // 衰减矩阵：从sourceType到targetType的衰减系数
        Map<String, Map<String, Double>> decayMatrix = Map.of(
            "DOCUMENT", Map.of(
                "SEGMENT", 0.9,
                "KEYWORD", 0.7
            ),
            "SEGMENT", Map.of(
                "SEGMENT", 0.85,
                "KEYWORD", 0.75,
                "DOCUMENT", 0.6
            ),
            "KEYWORD", Map.of(
                "SEGMENT", 0.7,
                "KEYWORD", 0.55
            )
        );
        
        return decayMatrix.getOrDefault(sourceType, Map.of())
                          .getOrDefault(targetType, defaultDecayFactor);
    }

    /**
     * 获取相关节点
     *
     * @param nodeUuid 节点UUID
     * @param depth    关系深度
     * @return 相关节点列表
     */
    public List<KnowledgeBaseGraphNode> getRelatedNodes(String nodeUuid, int depth) {
        return graphStorage.getRelatedNodes(nodeUuid, depth, 10);
    }

    /**
     * 删除知识库图谱数据
     *
     * @param kb 知识库
     */
    @Transactional
    public void deleteByKbUuid(KnowledgeBase kb) {
        graphStorage.deleteByKbUuid(kb.getUuid());
        log.info("Deleted graph data for kb: {}", kb.getUuid());
    }

    /**
     * 统计图谱节点数量
     *
     * @param kb 知识库
     * @return 节点数量
     */
    public int countNodesByKbUuid(KnowledgeBase kb) {
        return graphStorage.countNodesByKbUuid(kb.getUuid());
    }

    // ==================== 私有方法 ====================

    /**
     * 创建文档节点
     */
    private KnowledgeBaseGraphNode createDocumentNode(KnowledgeBase kb) {
        KnowledgeBaseGraphNode node = new KnowledgeBaseGraphNode();
        node.setUuid(UuidUtil.create());
        node.setKbUuid(kb.getUuid());
        node.setNodeType("DOCUMENT");
        node.setNodeId(kb.getUuid());
        node.setContent(kb.getTitle());
        node.setCreatedTime(LocalDateTime.now());
        return node;
    }

    /**
     * 创建段落节点
     */
    private KnowledgeBaseGraphNode createSegmentNode(KnowledgeBase kb, Document doc, int index) {
        KnowledgeBaseGraphNode node = new KnowledgeBaseGraphNode();
        node.setUuid(UuidUtil.create());
        node.setKbUuid(kb.getUuid());
        node.setNodeType("SEGMENT");
        node.setNodeId("segment-" + index);
        node.setContent(doc.text());
        
        // 存储元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("index", index);
        if (doc.metadata() != null) {
            metadata.put("source", doc.metadata().toMap());
        }
        node.setMetadataJson(JsonUtil.toJson(metadata));
        node.setCreatedTime(LocalDateTime.now());
        return node;
    }

    /**
     * 创建关键词节点
     */
    private KnowledgeBaseGraphNode createKeywordNode(KnowledgeBase kb, String keyword) {
        KnowledgeBaseGraphNode node = new KnowledgeBaseGraphNode();
        node.setUuid(UuidUtil.create());
        node.setKbUuid(kb.getUuid());
        node.setNodeType("KEYWORD");
        node.setNodeId("keyword-" + keyword.hashCode());
        node.setContent(keyword);
        node.setCreatedTime(LocalDateTime.now());
        return node;
    }

    /**
     * 创建包含关系
     */
    private KnowledgeBaseGraphEdge createContainsEdge(KnowledgeBase kb, String sourceUuid, String targetUuid) {
        KnowledgeBaseGraphEdge edge = new KnowledgeBaseGraphEdge();
        edge.setUuid(UuidUtil.create());
        edge.setKbUuid(kb.getUuid());
        edge.setSourceNodeUuid(sourceUuid);
        edge.setTargetNodeUuid(targetUuid);
        edge.setRelationType("CONTAINS");
        edge.setWeight(containsRelationWeight);  // 使用配置注入的权重
        edge.setCreatedTime(LocalDateTime.now());
        return edge;
    }

    /**
     * 创建关键词关系
     */
    private KnowledgeBaseGraphEdge createKeywordEdge(KnowledgeBase kb, String sourceUuid, String targetUuid, String keyword) {
        KnowledgeBaseGraphEdge edge = new KnowledgeBaseGraphEdge();
        edge.setUuid(UuidUtil.create());
        edge.setKbUuid(kb.getUuid());
        edge.setSourceNodeUuid(sourceUuid);
        edge.setTargetNodeUuid(targetUuid);
        edge.setRelationType("KEYWORD");
        // 基于关键词重要性动态计算权重
        edge.setWeight(calculateKeywordWeight(keyword));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("keyword", keyword);
        edge.setMetadataJson(JsonUtil.toJson(metadata));
        edge.setCreatedTime(LocalDateTime.now());
        return edge;
    }

    /**
     * 计算关键词权重（基于关键词长度和内容特征）
     */
    private double calculateKeywordWeight(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return keywordRelationWeight;
        }
        
        double weight = keywordRelationWeight;
        int length = keyword.length();
        
        // 长度加权：中等长度关键词更具区分性
        if (length >= 3 && length <= 8) {
            weight += 0.1;
        } else if (length > 12) {
            weight -= 0.15;
        }
        
        // 数字关键词权重降低
        if (keyword.matches(".*\\d+.*")) {
            weight -= 0.15;
        }
        
        // 归一化到合理范围
        return Math.min(Math.max(weight, 0.2), 0.8);
    }

    /**
     * 创建相似关系（基于内容相似度）
     */
    private void createSimilarEdges(KnowledgeBase kb, Map<String, KnowledgeBaseGraphNode> segmentNodes, List<KnowledgeBaseGraphEdge> edges) {
        List<KnowledgeBaseGraphNode> nodeList = new ArrayList<>(segmentNodes.values());
        
        // 基于内容相似度创建相似关系
        for (int i = 0; i < nodeList.size(); i++) {
            for (int j = i + 1; j < nodeList.size(); j++) {
                KnowledgeBaseGraphNode node1 = nodeList.get(i);
                KnowledgeBaseGraphNode node2 = nodeList.get(j);
                
                // 计算内容相似度（Jaccard相似度）
                double similarity = calculateJaccardSimilarity(node1.getContent(), node2.getContent());
                
                // 阈值过滤
                if (similarity >= similarityThreshold) {
                    KnowledgeBaseGraphEdge edge = new KnowledgeBaseGraphEdge();
                    edge.setUuid(UuidUtil.create());
                    edge.setKbUuid(kb.getUuid());
                    edge.setSourceNodeUuid(node1.getUuid());
                    edge.setTargetNodeUuid(node2.getUuid());
                    edge.setRelationType("SIMILAR_TO");
                    // 基于相似度动态设置权重
                    edge.setWeight(similarity * similarRelationWeight * 2);
                    edge.setCreatedTime(LocalDateTime.now());
                    edges.add(edge);
                }
            }
        }
    }

    /**
     * 计算Jaccard相似度
     */
    private double calculateJaccardSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0;
        }
        
        Set<String> words1 = tokenizeToSet(text1);
        Set<String> words2 = tokenizeToSet(text2);
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0;
        }
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    /**
     * 分词并转换为集合（用于相似度计算）
     */
    private Set<String> tokenizeToSet(String text) {
        Set<String> tokens = new HashSet<>();
        String[] words = text.toLowerCase().split("[\\s,，。！？；;：:\"\"''\\[\\]()（）]+");
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 15) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    /**
     * 提取关键词（基于TF-IDF加权）
     */
    private List<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 使用TF-IDF加权提取关键词
        Map<String, Double> tfIdfScores = calculateTfIdf(text);
        
        return tfIdfScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(keywordLimit)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 计算TF-IDF分数
     */
    private Map<String, Double> calculateTfIdf(String text) {
        Map<String, Double> tfIdfScores = new HashMap<>();
        String[] words = text.toLowerCase().split("[\\s,，。！？；;：:\"\"''\\[\\]()（）]+");
        
        // 词频统计
        Map<String, Integer> termFreq = new HashMap<>();
        int totalTerms = 0;
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 15 && !isStopWord(word)) {
                termFreq.put(word, termFreq.getOrDefault(word, 0) + 1);
                totalTerms++;
            }
        }
        
        // 计算TF-IDF
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String term = entry.getKey();
            double tf = totalTerms > 0 ? (double) entry.getValue() / totalTerms : 0;
            double idf = getInverseDocumentFrequency(term);
            tfIdfScores.put(term, tf * idf);
        }
        
        return tfIdfScores;
    }

    /**
     * 判断是否为停用词
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
            "的", "是", "在", "有", "和", "了", "我", "你", "他", "她", "它",
            "这", "那", "能", "会", "可以", "不", "很", "都", "就", "要",
            "把", "被", "给", "让", "跟", "向", "对", "对于", "关于"
        );
        return stopWords.contains(word);
    }

    /**
     * 获取逆文档频率（经验值）
     */
    private double getInverseDocumentFrequency(String term) {
        // 常见词IDF较低，专业词IDF较高
        Map<String, Double> idfMap = Map.of(
            "的", 0.1, "是", 0.1, "在", 0.15, "有", 0.15, "和", 0.1,
            "了", 0.1, "我", 0.2, "你", 0.2, "他", 0.2, "她", 0.2
        );
        return idfMap.getOrDefault(term, 1.0);
    }

    /**
     * 计算匹配分数（基于词重叠度和顺序匹配）
     */
    private double calculateMatchScore(String query, String content) {
        if (query == null || content == null) {
            return 0;
        }
        
        String lowerQuery = query.toLowerCase();
        String lowerContent = content.toLowerCase();
        
        // 词重叠度
        double wordOverlap = calculateWordOverlap(lowerQuery, lowerContent);
        
        // 顺序匹配度
        double orderMatch = calculateOrderMatch(lowerQuery, lowerContent);
        
        // 加权融合
        return wordOverlap * 0.7 + orderMatch * 0.3;
    }

    /**
     * 计算词重叠度
     */
    private double calculateWordOverlap(String query, String content) {
        Set<String> queryWords = tokenizeToSet(query);
        Set<String> contentWords = tokenizeToSet(content);
        
        if (queryWords.isEmpty()) {
            return 0;
        }
        
        int matchCount = 0;
        for (String word : queryWords) {
            if (contentWords.contains(word)) {
                matchCount++;
            }
        }
        
        return (double) matchCount / queryWords.size();
    }

    /**
     * 计算顺序匹配度
     */
    private double calculateOrderMatch(String query, String content) {
        List<String> queryTokens = new ArrayList<>(tokenizeToSet(query));
        
        if (queryTokens.size() < 2) {
            return 1.0;
        }
        
        int matchedPairs = 0;
        int totalPairs = queryTokens.size() - 1;
        
        for (int i = 0; i < totalPairs; i++) {
            String pair = queryTokens.get(i) + " " + queryTokens.get(i + 1);
            if (content.contains(pair)) {
                matchedPairs++;
            }
        }
        
        return totalPairs > 0 ? (double) matchedPairs / totalPairs : 1.0;
    }

    /**
     * 检查结果列表是否包含指定节点
     */
    private boolean containsNode(List<GraphRetrievalResult> results, String nodeUuid) {
        return results.stream().anyMatch(r -> r.getNodeUuid().equals(nodeUuid));
    }
}