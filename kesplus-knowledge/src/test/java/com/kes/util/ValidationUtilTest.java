package com.kes.util;

import com.kes.dto.request.KnowledgeBaseCreateRequest;
import com.kes.dto.request.TenantCreateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void testValidateNotNull() {
        assertDoesNotThrow(() -> ValidationUtil.validateNotNull("test", "param"));
        
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validateNotNull(null, "param"));
        assertEquals("param cannot be null", exception.getMessage());
    }

    @Test
    void testValidateNotEmpty() {
        assertDoesNotThrow(() -> ValidationUtil.validateNotEmpty("test", "param"));
        
        Exception nullException = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validateNotEmpty(null, "param"));
        assertEquals("param cannot be null or empty", nullException.getMessage());
        
        Exception emptyException = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validateNotEmpty("", "param"));
        assertEquals("param cannot be null or empty", emptyException.getMessage());
        
        Exception blankException = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validateNotEmpty("   ", "param"));
        assertEquals("param cannot be null or empty", blankException.getMessage());
    }

    @Test
    void testValidateMinLength() {
        assertDoesNotThrow(() -> ValidationUtil.validateMinLength("test", 3, "param"));
        
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validateMinLength("ab", 3, "param"));
        assertEquals("param must be at least 3 characters", exception.getMessage());
    }

    @Test
    void testValidateMaxLength() {
        assertDoesNotThrow(() -> ValidationUtil.validateMaxLength("test", 10, "param"));
        
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validateMaxLength("too long string", 10, "param"));
        assertEquals("param must be at most 10 characters", exception.getMessage());
    }

    @Test
    void testValidateEmail() {
        assertDoesNotThrow(() -> ValidationUtil.validateEmail("test@example.com"));
        
        Exception invalidException = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validateEmail("invalid-email"));
        assertEquals("Invalid email format", invalidException.getMessage());
        
        Exception nullException = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validateEmail(null));
        assertEquals("Email cannot be null", nullException.getMessage());
    }

    @Test
    void testValidateKnowledgeBaseCreateRequest() {
        KnowledgeBaseCreateRequest validRequest = new KnowledgeBaseCreateRequest();
        validRequest.setTitle("Valid Title");
        validRequest.setRemark("Test remark");
        assertDoesNotThrow(() -> ValidationUtil.validate(validRequest));

        KnowledgeBaseCreateRequest invalidRequest = new KnowledgeBaseCreateRequest();
        invalidRequest.setTitle("");
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validate(invalidRequest));
        assertTrue(exception.getMessage().contains("title"));
    }

    @Test
    void testValidateTenantCreateRequest() {
        TenantCreateRequest validRequest = new TenantCreateRequest();
        validRequest.setName("Valid Tenant");
        validRequest.setCode("VALID");
        assertDoesNotThrow(() -> ValidationUtil.validate(validRequest));

        TenantCreateRequest invalidRequest = new TenantCreateRequest();
        invalidRequest.setName("");
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validate(invalidRequest));
        assertTrue(exception.getMessage().contains("name"));
    }

    @Test
    void testValidatePositiveNumber() {
        assertDoesNotThrow(() -> ValidationUtil.validatePositive(10, "param"));
        assertDoesNotThrow(() -> ValidationUtil.validatePositive(1, "param"));
        
        Exception zeroException = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validatePositive(0, "param"));
        assertEquals("param must be positive", zeroException.getMessage());
        
        Exception negativeException = assertThrows(IllegalArgumentException.class, 
            () -> ValidationUtil.validatePositive(-1, "param"));
        assertEquals("param must be positive", negativeException.getMessage());
    }
}
