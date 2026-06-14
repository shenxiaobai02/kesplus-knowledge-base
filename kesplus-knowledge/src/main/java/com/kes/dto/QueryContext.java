package com.kes.dto;

import com.kes.enums.QueryIntent;
import com.kes.util.ThreadContext;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询上下文
 * <p>
 * 存储查询增强和检索过程中需要的上下文信息，包括原始查询、历史问题、用户信息等。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Data
@SuperBuilder
public class QueryContext {

    /**
     * 知识库UUID
     */
    private String kbUuid;

    /**
     * 原始查询
     */
    private String originalQuery;

    /**
     * 历史问题列表
     */
    private List<String> historyQuestions;

    /**
     * 当前用户上下文
     */
    private ThreadContext.UserContext user;

    /**
     * 识别到的查询意图
     */
    private QueryIntent intent;

    public QueryContext() {
        this.historyQuestions = new ArrayList<>();
        this.intent = QueryIntent.UNKNOWN;
    }

    /**
     * 创建查询上下文
     */
    public static QueryContext of(String kbUuid, String originalQuery) {
        QueryContext context = new QueryContext();
        context.setKbUuid(kbUuid);
        context.setOriginalQuery(originalQuery);
        context.setUser(ThreadContext.getUserContext());
        return context;
    }

    /**
     * 添加历史问题
     */
    public void addHistoryQuestion(String question) {
        if (this.historyQuestions == null) {
            this.historyQuestions = new ArrayList<>();
        }
        this.historyQuestions.add(question);
    }
}
