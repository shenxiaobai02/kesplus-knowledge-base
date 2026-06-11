package com.kes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UuidUtilTest {

    @Test
    void testCreate() {
        String uuid = UuidUtil.create();

        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.contains("-"));
    }

    @Test
    void testCreateShort() {
        String shortUuid = UuidUtil.createShort();

        assertNotNull(shortUuid);
        assertTrue(shortUuid.length() > 0);
        assertFalse(shortUuid.contains("-"));
    }

    @Test
    void testCreateMultiple() {
        String uuid1 = UuidUtil.create();
        String uuid2 = UuidUtil.create();

        assertNotNull(uuid1);
        assertNotNull(uuid2);
        assertNotEquals(uuid1, uuid2);
    }

    @Test
    void testIsValid() {
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";
        String invalidUuid = "invalid-uuid";

        assertTrue(UuidUtil.isValid(validUuid));
        assertFalse(UuidUtil.isValid(invalidUuid));
        assertFalse(UuidUtil.isValid(null));
        assertFalse(UuidUtil.isValid(""));
    }
}
