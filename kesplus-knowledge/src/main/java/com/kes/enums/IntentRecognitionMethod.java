package com.kes.enums;

/**
 * 意图识别方法枚举
 * <p>
 * 定义意图识别的不同技术方法，用于记录识别过程的来源和可靠性。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
public enum IntentRecognitionMethod {

    /**
     * 基于规则的匹配
     * 优点：毫秒级响应，零成本
     * 适用：高频、结构化明确的查询
     */
    RULE_BASED,

    /**
     * 基于LLM的分类
     * 优点：利用世界知识，泛化能力最强
     * 适用：复杂、边界模糊的查询
     */
    LLM_CLASSIFY,

    /**
     * RAG增强的分类
     * 优点：泛化性强，无需频繁重训练
     * 适用：长尾查询、泛化要求高的场景
     */
    RAG_AUGMENTED
}
