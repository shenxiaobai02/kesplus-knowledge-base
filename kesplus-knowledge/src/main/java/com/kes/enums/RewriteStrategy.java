package com.kes.enums;

/**
 * 查询改写策略枚举
 * <p>
 * 定义不同的查询改写策略，用于选择合适的改写方法。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
public enum RewriteStrategy {

    /**
     * 同义词扩展
     * 将口语词扩展为规范术语
     */
    SYNONYMY_EXPANSION("同义词扩展"),

    /**
     * 指代消解
     * 将代词替换为完整表达
     */
    PRONOUN_RESOLUTION("指代消解"),

    /**
     * 结构化转换
     * 将自然语言转为结构化查询
     */
    STRUCTURAL_TRANSFORM("结构化转换"),

    /**
     * 时间补全
     * 补充时间相关上下文
     */
    TEMPORAL_COMPLETION("时间补全"),

    /**
     * 实体补全
     * 补充实体相关属性信息
     */
    ENTITY_COMPLETION("实体补全"),

    /**
     * 上下文补全
     * 基于对话历史补充缺失上下文
     */
    CONTEXT_COMPLETION("上下文补全"),

    /**
     * 缩写展开
     * 将缩写展开为完整表述
     */
    ABBREVIATION_EXPANSION("缩写展开"),

    /**
     * 默认改写
     * 不做特殊改写
     */
    DEFAULT("默认改写");

    /**
     * 策略描述
     */
    private final String description;

    RewriteStrategy(String description) {
        this.description = description;
    }

    /**
     * 获取策略描述
     */
    public String getDescription() {
        return description;
    }
}
