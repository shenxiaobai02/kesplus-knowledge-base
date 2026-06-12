package com.kes.service;

import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseItem;
import com.kes.mapper.KnowledgeBaseMapper;
import com.kes.util.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class KnowledgeBaseItemServiceTest {

    @Autowired
    private KnowledgeBaseItemService itemService;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    private KnowledgeBase kb;
    private String kbUuid;

    @BeforeEach
    void setUp() {
        kb = new KnowledgeBase();
        kb.setTitle("Test KB");
        kb.setRemark("Test remark");
        kb.setIsPublic(false);
        kb.setIsStrict(false);
        kb.setEmbeddingDimension(1024);
        kb.setIsDeleted(false);
        kb.setUuid(UuidUtil.create());
        
        knowledgeBaseMapper.insert(kb);
        kbUuid = kb.getUuid();
    }

    @Test
    void testGetByUuid() {
        KnowledgeBaseItem item = itemService.create(kbUuid, "Test Item", "Test brief", "Test remark");

        KnowledgeBaseItem result = itemService.getByUuid(item.getUuid());

        assertNotNull(result);
        assertEquals(item.getUuid(), result.getUuid());
        assertEquals("Test Item", result.getTitle());
    }

    @Test
    void testListByKbUuid() {
        itemService.create(kbUuid, "Test Item 1", "Brief 1", "Remark 1");
        itemService.create(kbUuid, "Test Item 2", "Brief 2", "Remark 2");

        List<KnowledgeBaseItem> result = itemService.listByKbUuid(kbUuid);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testCreate() {
        KnowledgeBaseItem result = itemService.create(kbUuid, "Test Item", "Test brief", "Test remark");

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getUuid());
        assertEquals("Test Item", result.getTitle());
        assertEquals("Test brief", result.getBrief());
        assertFalse(result.getIsDeleted());
    }

    @Test
    void testCreateWithNonExistentKb() {
        assertThrows(RuntimeException.class, () -> 
            itemService.create(UuidUtil.create(), "Test Item", "Test brief", "Test remark"));
    }

    @Test
    void testUpdate() {
        KnowledgeBaseItem item = itemService.create(kbUuid, "Test Item", "Test brief", "Test remark");

        KnowledgeBaseItem updateItem = new KnowledgeBaseItem();
        updateItem.setUuid(item.getUuid());
        updateItem.setTitle("Updated Item");
        updateItem.setBrief("Updated brief");

        KnowledgeBaseItem result = itemService.update(updateItem);

        assertNotNull(result);
        assertEquals("Updated Item", result.getTitle());
        assertEquals("Updated brief", result.getBrief());
    }

    @Test
    void testUpdateNotFound() {
        KnowledgeBaseItem item = new KnowledgeBaseItem();
        item.setUuid(UuidUtil.create());
        item.setTitle("Updated Item");

        assertThrows(RuntimeException.class, () -> itemService.update(item));
    }

    @Test
    void testDelete() {
        KnowledgeBaseItem item = itemService.create(kbUuid, "Test Item", "Test brief", "Test remark");
        String uuid = item.getUuid();

        itemService.delete(uuid);

        KnowledgeBaseItem result = itemService.getByUuid(uuid);
        assertTrue(result.getIsDeleted());
    }

    @Test
    void testDeleteNotFound() {
        assertDoesNotThrow(() -> itemService.delete(UuidUtil.create()));
    }

    @Test
    void testDeleteByKbUuid() {
        itemService.create(kbUuid, "Test Item 1", "Brief 1", "Remark 1");
        itemService.create(kbUuid, "Test Item 2", "Brief 2", "Remark 2");

        itemService.deleteByKbUuid(kbUuid);

        List<KnowledgeBaseItem> items = itemService.listByKbUuid(kbUuid);
        for (KnowledgeBaseItem item : items) {
            assertTrue(item.getIsDeleted());
        }
    }

    @Test
    void testCountByKbUuid() {
        itemService.create(kbUuid, "Test Item 1", "Brief 1", "Remark 1");
        itemService.create(kbUuid, "Test Item 2", "Brief 2", "Remark 2");
        itemService.create(kbUuid, "Test Item 3", "Brief 3", "Remark 3");

        int count = itemService.countByKbUuid(kbUuid);

        assertEquals(3, count);
    }

    @Test
    void testCountByKbUuidEmpty() {
        int count = itemService.countByKbUuid(kbUuid);

        assertEquals(0, count);
    }
}