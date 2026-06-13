package com.kes.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * <p>
 * 标记需要记录审计日志的方法，由AuditLogAspect切面自动拦截并记录操作日志。
 * 支持记录操作类型、资源类型、操作结果等信息。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * &#64;AuditLog(operationType = "CREATE", resourceType = "KNOWLEDGE_BASE")
 * public KnowledgeBase create(KnowledgeBase kb) { ... }
 * </pre>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 操作类型
     * <p>
     * 常见类型：CREATE、UPDATE、DELETE、READ、UPLOAD、QUERY等
     * </p>
     *
     * @return 操作类型字符串
     */
    String operationType();

    /**
     * 资源类型
     * <p>
     * 常见类型：KNOWLEDGE_BASE、DOCUMENT、ROLE、TENANT等
     * </p>
     *
     * @return 资源类型字符串
     */
    String resourceType();

    /**
     * 操作描述
     * <p>
     * 可选的详细描述信息，用于补充说明操作内容
     * </p>
     *
     * @return 操作描述字符串，默认为空
     */
    String description() default "";
}