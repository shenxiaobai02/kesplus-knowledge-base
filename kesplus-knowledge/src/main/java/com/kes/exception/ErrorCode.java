package com.kes.exception;

public enum ErrorCode {

    // Common error codes
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_ERROR(500, "Internal Server Error"),

    // Knowledge Base error codes (1xxx)
    KNOWLEDGE_BASE_NOT_FOUND(1001, "Knowledge Base not found"),
    KNOWLEDGE_BASE_ITEM_NOT_FOUND(1002, "Knowledge Base Item not found"),

    // Tenant error codes (2xxx)
    TENANT_NOT_FOUND(2001, "Tenant not found"),
    TENANT_CODE_EXISTS(2002, "Tenant code already exists"),

    // Role error codes (3xxx)
    ROLE_NOT_FOUND(3001, "Role not found"),
    ROLE_CODE_EXISTS(3002, "Role code already exists");

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
