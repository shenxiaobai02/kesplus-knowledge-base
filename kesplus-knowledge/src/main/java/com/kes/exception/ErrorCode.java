package com.kes.exception;

/**
 * 错误码枚举
 * <p>
 * 定义系统所有错误码，按模块分类管理。
 * 错误码格式：模块前缀 + 序号，便于快速定位问题来源。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
public enum ErrorCode {

    // Common error codes (0xxx)
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_ERROR(500, "Internal Server Error"),

    // Permission error codes (4xxx)
    PERMISSION_DENIED(4031, "Permission denied"),
    KNOWLEDGE_BASE_ACCESS_DENIED(4032, "Knowledge base access denied"),
    ROLE_ACCESS_DENIED(4033, "Role access denied"),
    TENANT_ACCESS_DENIED(4034, "Tenant access denied"),

    // Knowledge Base error codes (1xxx)
    KNOWLEDGE_BASE_NOT_FOUND(1001, "Knowledge Base not found"),
    KNOWLEDGE_BASE_ITEM_NOT_FOUND(1002, "Knowledge Base Item not found"),
    KNOWLEDGE_BASE_ALREADY_EXISTS(1003, "Knowledge Base already exists"),
    KNOWLEDGE_BASE_EMBEDDING_ERROR(1004, "Knowledge Base embedding error"),

    // Tenant error codes (2xxx)
    TENANT_NOT_FOUND(2001, "Tenant not found"),
    TENANT_CODE_EXISTS(2002, "Tenant code already exists"),
    TENANT_DISABLED(2003, "Tenant is disabled"),

    // Role error codes (3xxx)
    ROLE_NOT_FOUND(3001, "Role not found"),
    ROLE_CODE_EXISTS(3002, "Role code already exists"),
    ROLE_DISABLED(3003, "Role is disabled"),
    ROLE_ASSIGNMENT_FAILED(3004, "Role assignment failed");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
