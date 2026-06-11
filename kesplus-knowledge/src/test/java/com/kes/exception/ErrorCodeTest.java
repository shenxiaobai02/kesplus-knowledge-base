package com.kes.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void testCommonErrorCodes() {
        assertEquals(400, ErrorCode.BAD_REQUEST.getCode());
        assertEquals("Bad Request", ErrorCode.BAD_REQUEST.getMessage());

        assertEquals(401, ErrorCode.UNAUTHORIZED.getCode());
        assertEquals("Unauthorized", ErrorCode.UNAUTHORIZED.getMessage());

        assertEquals(403, ErrorCode.FORBIDDEN.getCode());
        assertEquals("Forbidden", ErrorCode.FORBIDDEN.getMessage());

        assertEquals(404, ErrorCode.NOT_FOUND.getCode());
        assertEquals("Not Found", ErrorCode.NOT_FOUND.getMessage());

        assertEquals(500, ErrorCode.INTERNAL_ERROR.getCode());
        assertEquals("Internal Server Error", ErrorCode.INTERNAL_ERROR.getMessage());
    }

    @Test
    void testKnowledgeBaseErrorCodes() {
        assertEquals(1001, ErrorCode.KNOWLEDGE_BASE_NOT_FOUND.getCode());
        assertEquals("Knowledge Base not found", ErrorCode.KNOWLEDGE_BASE_NOT_FOUND.getMessage());

        assertEquals(1002, ErrorCode.KNOWLEDGE_BASE_ITEM_NOT_FOUND.getCode());
        assertEquals("Knowledge Base Item not found", ErrorCode.KNOWLEDGE_BASE_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    void testTenantErrorCodes() {
        assertEquals(2001, ErrorCode.TENANT_NOT_FOUND.getCode());
        assertEquals("Tenant not found", ErrorCode.TENANT_NOT_FOUND.getMessage());

        assertEquals(2002, ErrorCode.TENANT_CODE_EXISTS.getCode());
        assertEquals("Tenant code already exists", ErrorCode.TENANT_CODE_EXISTS.getMessage());
    }

    @Test
    void testRoleErrorCodes() {
        assertEquals(3001, ErrorCode.ROLE_NOT_FOUND.getCode());
        assertEquals("Role not found", ErrorCode.ROLE_NOT_FOUND.getMessage());

        assertEquals(3002, ErrorCode.ROLE_CODE_EXISTS.getCode());
        assertEquals("Role code already exists", ErrorCode.ROLE_CODE_EXISTS.getMessage());
    }

    @Test
    void testErrorCodeEquality() {
        ErrorCode code1 = ErrorCode.BAD_REQUEST;
        ErrorCode code2 = ErrorCode.BAD_REQUEST;
        ErrorCode code3 = ErrorCode.NOT_FOUND;

        assertEquals(code1, code2);
        assertNotEquals(code1, code3);
        assertEquals(code1.hashCode(), code2.hashCode());
    }
}
