package com.kes.service;

import com.kes.entity.KnowledgeBaseItem;
import com.kes.mapper.KnowledgeBaseItemMapper;
import com.kes.util.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseItemServiceTest {

    @Mock
    private KnowledgeBaseItemMapper itemMapper;

    @InjectMocks
    private KnowledgeBaseItemService itemService;

    private KnowledgeBaseItem item;

    @BeforeEach
    void setUp() {
        item = new KnowledgeBaseItem();
        item.setId(1L);
        item.setUuid(UuidUtil.create());
        item.setKbId(1L);
        item.setKbUuid(UuidUtil.create());
        item.setTitle("Test Item");
        item.setBrief("Test brief");
        item.setCreatedTime(LocalDateTime.now());
        item.setUpdatedTime(LocalDateTime.now());
    }

    @Test
    void testGetByUuid() {
        when(itemMapper.selectByUuid(eq(item.getUuid()))).thenReturn(item);

        KnowledgeBaseItem result = itemService.getByUuid(item.getUuid());

        assertNotNull(result);
        assertEquals(item.getUuid(), result.getUuid());
    }

    @Test
    void testListByKbUuid() {
        when(itemMapper.selectByKbUuid(eq(item.getKbUuid()))).thenReturn(Arrays.asList(item));

        List<KnowledgeBaseItem> result = itemService.listByKbUuid(item.getKbUuid());

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testCreate() {
        when(itemMapper.insert(any(KnowledgeBaseItem.class))).thenReturn(1);

        KnowledgeBaseItem result = itemService.create(item.getKbUuid(), "Test Item", "Test brief", "Test remark");

        assertNotNull(result);
        assertEquals("Test Item", result.getTitle());
        verify(itemMapper, times(1)).insert(any(KnowledgeBaseItem.class));
    }

    @Test
    void testUpdate() {
        when(itemMapper.selectByUuid(eq(item.getUuid()))).thenReturn(item);
        when(itemMapper.updateById(any(KnowledgeBaseItem.class))).thenReturn(1);

        item.setTitle("Updated Item");
        KnowledgeBaseItem result = itemService.update(item);

        assertNotNull(result);
        assertEquals("Updated Item", result.getTitle());
        verify(itemMapper, times(1)).updateById(any(KnowledgeBaseItem.class));
    }

    @Test
    void testDelete() {
        when(itemMapper.selectByUuid(eq(item.getUuid()))).thenReturn(item);
        when(itemMapper.updateById(any(KnowledgeBaseItem.class))).thenReturn(1);

        itemService.delete(item.getUuid());

        verify(itemMapper, times(1)).updateById(any(KnowledgeBaseItem.class));
    }

    @Test
    void testDeleteByKbUuid() {
        when(itemMapper.selectByKbUuid(eq(item.getKbUuid()))).thenReturn(Arrays.asList(item));
        when(itemMapper.updateById(any(KnowledgeBaseItem.class))).thenReturn(1);

        itemService.deleteByKbUuid(item.getKbUuid());

        verify(itemMapper, times(1)).updateById(any(KnowledgeBaseItem.class));
    }

    @Test
    void testCountByKbUuid() {
        when(itemMapper.countByKbUuid(eq(item.getKbUuid()))).thenReturn(5);

        int count = itemService.countByKbUuid(item.getKbUuid());

        assertEquals(5, count);
    }
}
