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
        item.setUuid(UuidUtil.generate());
        item.setKbId(1L);
        item.setKbUuid(UuidUtil.generate());
        item.setTitle("Test Item");
        item.setBrief("Test brief");
        item.setCreatedTime(LocalDateTime.now());
        item.setUpdatedTime(LocalDateTime.now());
    }

    @Test
    void testCreateItem() {
        when(itemMapper.insert(any(KnowledgeBaseItem.class))).thenReturn(1);
        when(itemMapper.selectById(any(Long.class))).thenReturn(item);

        KnowledgeBaseItem result = itemService.create(item);

        assertNotNull(result);
        assertEquals("Test Item", result.getTitle());
        verify(itemMapper, times(1)).insert(any(KnowledgeBaseItem.class));
    }

    @Test
    void testFindById() {
        when(itemMapper.selectById(eq(1L))).thenReturn(item);

        KnowledgeBaseItem result = itemService.findById(1L);

        assertNotNull(result);
        assertEquals("Test Item", result.getTitle());
    }

    @Test
    void testFindByUuid() {
        when(itemMapper.selectOne(any())).thenReturn(item);

        KnowledgeBaseItem result = itemService.findByUuid(item.getUuid());

        assertNotNull(result);
        assertEquals(item.getUuid(), result.getUuid());
    }

    @Test
    void testFindByKbUuid() {
        when(itemMapper.selectList(any())).thenReturn(Arrays.asList(item));

        List<KnowledgeBaseItem> result = itemService.findByKbUuid(item.getKbUuid());

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateItem() {
        when(itemMapper.selectOne(any())).thenReturn(item);
        when(itemMapper.updateById(any(KnowledgeBaseItem.class))).thenReturn(1);

        item.setTitle("Updated Item");
        KnowledgeBaseItem result = itemService.update(item.getUuid(), item);

        assertNotNull(result);
        assertEquals("Updated Item", result.getTitle());
        verify(itemMapper, times(1)).updateById(any(KnowledgeBaseItem.class));
    }

    @Test
    void testDeleteItem() {
        when(itemMapper.selectOne(any())).thenReturn(item);
        when(itemMapper.updateById(any(KnowledgeBaseItem.class))).thenReturn(1);

        itemService.delete(item.getUuid());

        verify(itemMapper, times(1)).updateById(any(KnowledgeBaseItem.class));
    }

    @Test
    void testDeleteByKbUuid() {
        when(itemMapper.delete(any())).thenReturn(1);

        itemService.deleteByKbUuid(item.getKbUuid());

        verify(itemMapper, times(1)).delete(any());
    }
}
