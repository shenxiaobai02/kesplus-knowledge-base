package com.kes.rag;

import com.kes.dto.ConversationContext;
import com.kes.dto.QueryContext;
import com.kes.entity.RewriteResult;
import com.kes.enums.QueryIntent;
import com.kes.enums.RewriteStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 结构化查询改写器
 * <p>
 * 根据不同的意图类型，对查询进行结构化改写，生成更适合检索的查询语句。
 * 支持多种改写策略：
 * - 事实性查询：补全实体信息
 * - 流程性查询：规范化步骤描述
 * - 比较性查询：构建对比框架
 * - 分析性查询：补全因果关系
 * - 列表检索查询：提取可数关键词
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rag.query-enhancer.structural-transform-enabled", havingValue = "true", matchIfMissing = true)
public class StructuralQueryRewriter implements QueryRewriter {

    private static final Set<QueryIntent> SUPPORTED_INTENTS = EnumSet.of(
            QueryIntent.FACTS,
            QueryIntent.PROCEDURE,
            QueryIntent.COMPARISON,
            QueryIntent.ANALYSIS,
            QueryIntent.LIST_RETRIEVAL
    );

    private static final Set<String> COMPARISON_KEYWORDS = new HashSet<>(Arrays.asList(
            "和", "与", "vs", "VS", "比较", "区别", "差异", "对比", "哪个好"
    ));

    private static final Set<String> CAUSE_EFFECT_KEYWORDS = new HashSet<>(Arrays.asList(
            "为什么", "原因", "为什么", "怎么会", "怎么会", "怎么会"
    ));

    @Override
    public RewriteResult rewrite(String query, QueryIntent intent, QueryContext context) {
        if (intent == null || intent == QueryIntent.UNKNOWN) {
            return defaultRewrite(query);
        }

        if (!supportedIntents().contains(intent)) {
            return defaultRewrite(query);
        }

        try {
            return switch (intent) {
                case FACTS -> rewriteFactsQuery(query, context);
                case PROCEDURE -> rewriteProcedureQuery(query, context);
                case COMPARISON -> rewriteComparisonQuery(query, context);
                case ANALYSIS -> rewriteAnalysisQuery(query, context);
                case LIST_RETRIEVAL -> rewriteListQuery(query, context);
                default -> defaultRewrite(query);
            };
        } catch (Exception e) {
            log.warn("Query rewrite failed for intent {}: {}", intent, e.getMessage());
            return defaultRewrite(query);
        }
    }

    @Override
    public Set<QueryIntent> supportedIntents() {
        return SUPPORTED_INTENTS;
    }

    /**
     * 事实性查询改写：补全实体信息
     * <p>
     * 例如："Java是什么" -> "Java编程语言定义、特点、应用场景"
     * </p>
     */
    private RewriteResult rewriteFactsQuery(String query, QueryContext context) {
        String rewritten = query.trim();

        // 补全"是什么"类型查询
        if (query.contains("是什么") && !query.contains("定义为") && !query.contains("含义")) {
            rewritten = query + "，定义、含义、特点";
        }

        // 补全"是谁"类型查询
        if (query.contains("是谁") && !query.contains("身份") && !query.contains("角色")) {
            rewritten = query + "，身份、背景、角色";
        }

        // 补全"什么时候"类型查询
        if (query.contains("什么时候") || query.contains("何时")) {
            rewritten = query + "，时间、日期、时期";
        }

        // 补全"在哪里"类型查询
        if (query.contains("在哪里") || query.contains("哪儿")) {
            rewritten = query + "，位置、地点、场所";
        }

        // 补全"多少"类型查询
        if (query.contains("多少") && !query.contains("数量") && !query.contains("价格")) {
            rewritten = query + "，数量、价格、费用";
        }

        // 基于上下文补全实体信息
        if (context instanceof ConversationContext ctx) {
            String lastMessage = ctx.getLastUserMessage();
            if (lastMessage != null && query.contains("它")) {
                String resolved = ctx.resolvePronoun("它");
                if (!resolved.equals("它")) {
                    rewritten = query.replace("它", resolved);
                }
            }
        }

        return RewriteResult.success(query, rewritten, RewriteStrategy.ENTITY_COMPLETION, 0.85);
    }

    /**
     * 流程性查询改写：规范化步骤描述
     * <p>
     * 例如："如何安装Docker" -> "Docker安装步骤教程指南"
     * </p>
     */
    private RewriteResult rewriteProcedureQuery(String query, QueryContext context) {
        String rewritten = query.trim();

        // 规范化"如何"类型查询
        if (query.startsWith("如何") || query.startsWith("怎么") || query.startsWith("怎样")) {
            String subject = extractSubjectAfter(query, Arrays.asList("安装", "配置", "使用", "设置", "部署"));
            if (subject != null) {
                rewritten = subject + "安装步骤教程指南";
            }
        }

        // 添加步骤关键词
        if (!query.contains("步骤") && !query.contains("流程") && !query.contains("教程")) {
            if (query.contains("安装")) {
                rewritten = query + "，安装步骤流程";
            } else if (query.contains("配置")) {
                rewritten = query + "，配置方法步骤";
            } else if (query.contains("部署")) {
                rewritten = query + "，部署流程步骤";
            }
        }

        // 基于上下文补全
        if (context instanceof ConversationContext) {
            ConversationContext ctx = (ConversationContext) context;
            String lastMessage = ctx.getLastUserMessage();
            if (lastMessage != null && query.contains("它")) {
                String resolved = ctx.resolvePronoun("它");
                if (!resolved.equals("它")) {
                    rewritten = query.replace("它", resolved);
                }
            }
        }

        return RewriteResult.success(query, rewritten, RewriteStrategy.STRUCTURAL_TRANSFORM, 0.88);
    }

    /**
     * 比较性查询改写：构建对比框架
     * <p>
     * 例如："MySQL vs PostgreSQL" -> "MySQL和PostgreSQL区别对比数据库"
     * </p>
     */
    private RewriteResult rewriteComparisonQuery(String query, QueryContext context) {
        String rewritten = query.trim();

        // 规范化vs类型查询
        if (query.contains("vs") || query.contains("VS")) {
            String[] parts = query.split("(vs|VS)");
            if (parts.length >= 2) {
                rewritten = parts[0].trim() + "和" + parts[1].trim() + "区别对比";
            }
        }

        // 规范化"和...区别"类型查询
        if (query.contains("和") && (query.contains("区别") || query.contains("差异"))) {
            int andIndex = query.indexOf("和");
            int diffIndex = query.indexOf("区别");
            if (diffIndex == -1) {
                diffIndex = query.indexOf("差异");
            }
            if (andIndex > 0 && diffIndex > andIndex) {
                String entity = query.substring(andIndex + 1, diffIndex).trim();
                String subject = query.substring(0, andIndex).trim();
                rewritten = subject + "和" + entity + "区别对比";
            }
        }

        // 添加对比关键词
        if (!query.contains("对比") && !query.contains("比较")) {
            rewritten = rewritten + "，功能、性能、特点对比";
        }

        return RewriteResult.success(query, rewritten, RewriteStrategy.STRUCTURAL_TRANSFORM, 0.82);
    }

    /**
     * 分析性查询改写：补全因果关系
     * <p>
     * 例如："为什么要用微服务" -> "微服务架构优点、原因、优势分析"
     * </p>
     */
    private RewriteResult rewriteAnalysisQuery(String query, QueryContext context) {
        String rewritten = query.trim();

        // 补全"为什么"类型查询
        if (query.startsWith("为什么") || query.startsWith("为何")) {
            String subject = extractSubjectAfter(query, Arrays.asList("用", "使用", "选择", "采用"));
            if (subject != null) {
                rewritten = subject + "优点、原因、优势分析";
            } else {
                rewritten = query + "，原因、理由、解释";
            }
        }

        // 添加分析维度
        if (!query.contains("原因") && !query.contains("影响") && !query.contains("结果")) {
            if (query.contains("为什么")) {
                rewritten = rewritten + "，原因分析";
            }
        }

        return RewriteResult.success(query, rewritten, RewriteStrategy.CONTEXT_COMPLETION, 0.80);
    }

    /**
     * 列表检索查询改写：提取可数关键词
     * <p>
     * 例如："Docker有哪些版本" -> "Docker版本列表"
     * </p>
     */
    private RewriteResult rewriteListQuery(String query, QueryContext context) {
        String rewritten = query.trim();

        // 规范化"有哪些"类型查询
        if (query.contains("有哪些") || query.contains("有什么") || query.contains("列举")) {
            String subject = extractSubjectBefore(query, Arrays.asList("有哪些", "有什么"));
            if (subject != null) {
                rewritten = subject + "列表";
            }
        }

        // 规范化"需要哪些"类型查询
        if (query.contains("需要哪些")) {
            String subject = extractSubjectBefore(query, Arrays.asList("需要哪些"));
            if (subject != null) {
                rewritten = subject + "需求列表";
            }
        }

        // 添加列表关键词
        if (!query.contains("列表") && !query.contains("清单")) {
            if (query.contains("版本") || query.contains("类型") || query.contains("功能")) {
                rewritten = rewritten + "列表";
            }
        }

        return RewriteResult.success(query, rewritten, RewriteStrategy.STRUCTURAL_TRANSFORM, 0.83);
    }

    /**
     * 默认改写（不做特殊处理）
     */
    private RewriteResult defaultRewrite(String query) {
        return RewriteResult.success(query, query, RewriteStrategy.DEFAULT, 1.0);
    }

    /**
     * 从查询中提取主语（跟在关键词后面的内容）
     */
    private String extractSubjectAfter(String query, java.util.List<String> keywords) {
        for (String keyword : keywords) {
            int index = query.indexOf(keyword);
            if (index > 0) {
                return query.substring(0, index).trim();
            }
        }
        return null;
    }

    /**
     * 从查询中提取主语（在关键词前面的内容）
     */
    private String extractSubjectBefore(String query, java.util.List<String> keywords) {
        for (String keyword : keywords) {
            int index = query.indexOf(keyword);
            if (index > 0) {
                return query.substring(0, index).trim();
            }
        }
        return null;
    }
}
