package com.kes.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kes.entity.EmbeddingModel;
import com.kes.entity.KnowledgeBase;
import com.kes.mapper.KnowledgeBaseMapper;
import com.kes.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class KnowledgeBaseService extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> {

    @Autowired
    private EmbeddingModelService embeddingModelService;

    @Autowired
    private EmbeddingRagService embeddingRagService;

    @Transactional
    public KnowledgeBase create(KnowledgeBase kb, String embeddingModelUuid) {
        kb.setUuid(UuidUtil.create());
        kb.setIsDeleted(false);
        kb.setCreatedTime(LocalDateTime.now());
        kb.setUpdatedTime(LocalDateTime.now());

        if (embeddingModelUuid != null) {
            EmbeddingModel model = embeddingModelService.getByUuid(embeddingModelUuid);
            if (model != null) {
                kb.setEmbeddingModelUuid(model.getUuid());
                kb.setEmbeddingDimension(model.getEmbeddingDimension());

                embeddingRagService.ensureTableExists(model.getEmbeddingDimension());
            }
        }

        baseMapper.insert(kb);
        log.info("Created knowledge base: {}", kb.getTitle());
        return kb;
    }

    @Transactional
    public KnowledgeBase update(KnowledgeBase kb) {
        KnowledgeBase existing = getByUuid(kb.getUuid());
        if (existing == null) {
            throw new RuntimeException("Knowledge base not found");
        }

        Integer oldDimension = existing.getEmbeddingDimension();
        Integer newDimension = kb.getEmbeddingDimension();

        kb.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(kb);

        if (newDimension != null && !newDimension.equals(oldDimension)) {
            embeddingRagService.ensureTableExists(newDimension);
        }

        log.info("Updated knowledge base: {}", kb.getTitle());
        return kb;
    }

    @Transactional
    public void delete(String uuid) {
        KnowledgeBase kb = getByUuid(uuid);
        if (kb == null) {
            return;
        }

        kb.setIsDeleted(true);
        kb.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(kb);

        embeddingRagService.deleteByKbUuid(kb);
        log.info("Deleted knowledge base: {}", uuid);
    }

    public KnowledgeBase getByUuid(String uuid) {
        return baseMapper.selectByUuid(uuid);
    }

    public void updateEmbeddingCount(String kbUuid) {
        KnowledgeBase kb = getByUuid(kbUuid);
        if (kb == null) {
            return;
        }

        int count = embeddingRagService.countByKbUuid(kb);
        kb.setEmbeddingCount(count);
        kb.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(kb);
    }

    public java.util.List<KnowledgeBase> listByTenant(String tenantUuid) {
        return baseMapper.selectByTenantUuid(tenantUuid);
    }

    public java.util.List<KnowledgeBase> list() {
        return baseMapper.selectAllActive();
    }
}