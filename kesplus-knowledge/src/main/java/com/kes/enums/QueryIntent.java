package com.kes.enums;

/**
 * 查询意图枚举
 * <p>
 * 定义用户查询的意图类型，用于查询增强和答案生成策略选择。
 * 采用二级意图分类体系：
 * - 一级意图：知识检索类、非知识检索类
 * - 二级意图：具体的问题类型
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
public enum QueryIntent {

    // ============ 知识检索类意图 ============

    /**
     * 事实性问题 - 询问具体的事实、数据
     * 示例：Java是什么、是谁、什么时候、在哪里、多少
     */
    FACTS("知识检索"),

    /**
     * 流程性问题 - 询问操作步骤、方法
     * 示例：如何安装、怎么配置、步骤、流程、方法
     */
    PROCEDURE("知识检索"),

    /**
     * 比较性问题 - 询问差异、对比
     * 示例：MySQL和PostgreSQL区别、比较、不同
     */
    COMPARISON("知识检索"),

    /**
     * 分析性问题 - 询问原因、影响
     * 示例：为什么要用、原因、影响、结果
     */
    ANALYSIS("知识检索"),

    /**
     * 列表检索问题 - 询问有哪些、列举
     * 示例：有哪些方法、需要哪些步骤、列举
     */
    LIST_RETRIEVAL("知识检索"),

    // ============ 非知识检索类意图 ============

    /**
     * 闲聊问题 - 问候、日常对话
     * 示例：你好、天气怎么样
     */
    CASUAL("非知识检索"),

    /**
     * 问候问题 - 简单的问候
     * 示例：你好、您好、hi
     */
    GREETING("非知识检索"),

    /**
     * 系统操作问题 - 查询状态、执行操作
     * 示例：查询状态、执行操作
     */
    SYSTEM_ACTION("非知识检索"),

    /**
     * 创意性问题 - 建议、想象
     * 示例：建议、想象、假如
     */
    CREATIVE("非知识检索"),

    // ============ 未知意图 ============

    /**
     * 未知意图 - 无法明确分类的查询
     */
    UNKNOWN("未知");

    /**
     * 意图所属的一级分类
     */
    private final String category;

    QueryIntent(String category) {
        this.category = category;
    }

    /**
     * 获取意图所属的一级分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 判断是否为知识检索类意图
     */
    public boolean isKnowledgeRetrieval() {
        return "知识检索".equals(category);
    }

    /**
     * 判断是否需要检索知识库
     */
    public boolean shouldRetrieve() {
        return this != GREETING
                && this != CASUAL
                && this != CREATIVE
                && this != UNKNOWN;
    }

    /**
     * 判断是否为主观创意性问题
     */
    public boolean isSubjective() {
        return this == CREATIVE || this == CASUAL;
    }
}
