package com.kes.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @Test
    void testFileInfoCreation() {
        FileStorageService.FileInfo info = new FileStorageService.FileInfo("test-uuid", "test.txt", "txt");
        
        assertEquals("test-uuid", info.getUuid());
        assertEquals("test.txt", info.getFilename());
        assertEquals("txt", info.getExtension());
    }
}