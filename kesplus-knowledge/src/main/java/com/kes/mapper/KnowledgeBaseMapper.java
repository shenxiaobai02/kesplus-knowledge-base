package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    KnowledgeBase selectByUuid(@Param("uuid") String uuid);

    void updateStatByUuid(@Param("uuid") String uuid, @Param("embeddingCount") Integer embeddingCount);
}