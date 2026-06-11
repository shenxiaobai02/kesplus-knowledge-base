package com.kes.util;

import com.kes.exception.BaseException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void testNotNull() {
        assertDoesNotThrow(() -> ValidationUtil.notNull("test", "param cannot be null"));
        
        BaseException exception = assertThrows(BaseException.class, 
            () -> ValidationUtil.notNull(null, "param cannot be null"));
        assertEquals("param cannot be null", exception.getMessage());
    }

    @Test
    void testNotEmptyString() {
        assertDoesNotThrow(() -> ValidationUtil.notEmpty("test", "param cannot be null or empty"));
        
        BaseException nullException = assertThrows(BaseException.class, 
            () -> ValidationUtil.notEmpty((String) null, "param cannot be null or empty"));
        assertEquals("param cannot be null or empty", nullException.getMessage());
        
        BaseException emptyException = assertThrows(BaseException.class, 
            () -> ValidationUtil.notEmpty("", "param cannot be null or empty"));
        assertEquals("param cannot be null or empty", emptyException.getMessage());
        
        BaseException blankException = assertThrows(BaseException.class, 
            () -> ValidationUtil.notEmpty("   ", "param cannot be null or empty"));
        assertEquals("param cannot be null or empty", blankException.getMessage());
    }

    @Test
    void testNotEmptyCollection() {
        List<String> list = Arrays.asList("item1", "item2");
        assertDoesNotThrow(() -> ValidationUtil.notEmpty(list, "collection cannot be null or empty"));
        
        BaseException nullException = assertThrows(BaseException.class, 
            () -> ValidationUtil.notEmpty((List<?>) null, "collection cannot be null or empty"));
        assertEquals("collection cannot be null or empty", nullException.getMessage());
        
        BaseException emptyException = assertThrows(BaseException.class, 
            () -> ValidationUtil.notEmpty(Arrays.asList(), "collection cannot be null or empty"));
        assertEquals("collection cannot be null or empty", emptyException.getMessage());
    }

    @Test
    void testNotEmptyMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        assertDoesNotThrow(() -> ValidationUtil.notEmpty(map, "map cannot be null or empty"));
        
        BaseException nullException = assertThrows(BaseException.class, 
            () -> ValidationUtil.notEmpty((Map<?, ?>) null, "map cannot be null or empty"));
        assertEquals("map cannot be null or empty", nullException.getMessage());
        
        BaseException emptyException = assertThrows(BaseException.class, 
            () -> ValidationUtil.notEmpty(new HashMap<>(), "map cannot be null or empty"));
        assertEquals("map cannot be null or empty", emptyException.getMessage());
    }

    @Test
    void testIsTrue() {
        assertDoesNotThrow(() -> ValidationUtil.isTrue(true, "condition must be true"));
        
        BaseException exception = assertThrows(BaseException.class, 
            () -> ValidationUtil.isTrue(false, "condition must be true"));
        assertEquals("condition must be true", exception.getMessage());
    }

    @Test
    void testIsFalse() {
        assertDoesNotThrow(() -> ValidationUtil.isFalse(false, "condition must be false"));
        
        BaseException exception = assertThrows(BaseException.class, 
            () -> ValidationUtil.isFalse(true, "condition must be false"));
        assertEquals("condition must be false", exception.getMessage());
    }

    @Test
    void testIsEmail() {
        assertDoesNotThrow(() -> ValidationUtil.isEmail("test@example.com", "Invalid email"));
        
        BaseException invalidException = assertThrows(BaseException.class, 
            () -> ValidationUtil.isEmail("invalid-email", "Invalid email"));
        assertEquals("Invalid email", invalidException.getMessage());
        
        BaseException nullException = assertThrows(BaseException.class, 
            () -> ValidationUtil.isEmail(null, "Invalid email"));
        assertEquals("Invalid email", nullException.getMessage());
    }

    @Test
    void testIsPhone() {
        assertDoesNotThrow(() -> ValidationUtil.isPhone("13800138000", "Invalid phone"));
        
        BaseException invalidException = assertThrows(BaseException.class, 
            () -> ValidationUtil.isPhone("12345", "Invalid phone"));
        assertEquals("Invalid phone", invalidException.getMessage());
        
        BaseException nullException = assertThrows(BaseException.class, 
            () -> ValidationUtil.isPhone(null, "Invalid phone"));
        assertEquals("Invalid phone", nullException.getMessage());
    }

    @Test
    void testIsUuid() {
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";
        assertDoesNotThrow(() -> ValidationUtil.isUuid(validUuid, "Invalid UUID"));
        
        BaseException invalidException = assertThrows(BaseException.class, 
            () -> ValidationUtil.isUuid("invalid-uuid", "Invalid UUID"));
        assertEquals("Invalid UUID", invalidException.getMessage());
    }

    @Test
    void testCheckRangeInt() {
        assertDoesNotThrow(() -> ValidationUtil.checkRange(5, 1, 10, "value out of range"));
        
        BaseException minException = assertThrows(BaseException.class, 
            () -> ValidationUtil.checkRange(0, 1, 10, "value out of range"));
        assertEquals("value out of range", minException.getMessage());
        
        BaseException maxException = assertThrows(BaseException.class, 
            () -> ValidationUtil.checkRange(11, 1, 10, "value out of range"));
        assertEquals("value out of range", maxException.getMessage());
    }

    @Test
    void testCheckRangeLong() {
        assertDoesNotThrow(() -> ValidationUtil.checkRange(5L, 1L, 10L, "value out of range"));
        
        BaseException exception = assertThrows(BaseException.class, 
            () -> ValidationUtil.checkRange(0L, 1L, 10L, "value out of range"));
        assertEquals("value out of range", exception.getMessage());
    }

    @Test
    void testCheckRangeDouble() {
        assertDoesNotThrow(() -> ValidationUtil.checkRange(5.0, 1.0, 10.0, "value out of range"));
        
        BaseException exception = assertThrows(BaseException.class, 
            () -> ValidationUtil.checkRange(0.0, 1.0, 10.0, "value out of range"));
        assertEquals("value out of range", exception.getMessage());
    }
}
