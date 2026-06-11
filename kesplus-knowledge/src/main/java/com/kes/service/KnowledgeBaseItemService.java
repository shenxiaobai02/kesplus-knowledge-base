package com.kes.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseItem;
import com.kes.mapper.KnowledgeBaseItemMapper;
import com.kes.mapper.KnowledgeBaseMapper;
import com.kes.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class KnowledgeBaseItemService extends ServiceImpl<KnowledgeBaseItemMapper, KnowledgeBaseItem> {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private EmbeddingRagService embeddingRagService;

    @Autowired
    private DynamicTableService dynamicTableService;

    public KnowledgeBaseItem getByUuid(String uuid) {
        return baseMapper.selectByUuid(uuid);
    }

    public List<KnowledgeBaseItem> listByKbUuid(String kbUuid) {
        return baseMapper.selectByKbUuid(kbUuid);
    }

    @Transactional
    public KnowledgeBaseItem create(String kbUuid, String title, String brief, String remark) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByUuid(kbUuid);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }

        KnowledgeBaseItem item = new KnowledgeBaseItem();
        item.setUuid(UuidUtil.create());
        item.setKbId(kb.getId());
        item.setKbUuid(kbUuid);
        item.setTitle(title);
        item.setBrief(brief);
        item.setRemark(remark);
        item.setIsDeleted(false);
        item.setCreatedTime(LocalDateTime.now());
        item.setUpdatedTime(LocalDateTime.now());

        baseMapper.insert(item);
        log.info("Created knowledge base item: {}", title);
        return item;
    }

    @Transactional
    public KnowledgeBaseItem update(KnowledgeBaseItem item) {
        KnowledgeBaseItem existing = getByUuid(item.getUuid());
        if (existing == null) {
            throw new RuntimeException("知识库条目不存在");
        }

        if (item.getTitle() != null) {
            existing.setTitle(item.getTitle());
        }
        if (item.getBrief() != null) {
            existing.setBrief(item.getBrief());
        }
        if (item.getRemark() != null) {
            existing.setRemark(item.getRemark());
        }
        existing.setUpdatedTime(LocalDateTime.now());

        baseMapper.updateById(existing);
        log.info("Updated knowledge base item: {}", existing.getUuid());
        return existing;
    }

    @Transactional
    public void delete(String uuid) {
        KnowledgeBaseItem item = getByUuid(uuid);
        if (item == null) {
            return;
        }

        item.setIsDeleted(true);
        item.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(item);

        KnowledgeBase kb = knowledgeBaseMapper.selectByUuid(item.getKbUuid());
        if (kb != null) {
            embeddingRagService.deleteByKbUuid(kb);
        }

        log.info("Deleted knowledge base item: {}", uuid);
    }

    @Transactional
    public void deleteByKbUuid(String kbUuid) {
        List<KnowledgeBaseItem> items = baseMapper.selectByKbUuid(kbUuid);
        for (KnowledgeBaseItem item : items) {
            item.setIsDeleted(true);
            item.setUpdatedTime(LocalDateTime.now());
            baseMapper.updateById(item);
        }
        log.info("Deleted {} items for kb: {}", items.size(), kbUuid);
    }

    public int countByKbUuid(String kbUuid) {
        return baseMapper.countByKbUuid(kbUuid);
    }
}