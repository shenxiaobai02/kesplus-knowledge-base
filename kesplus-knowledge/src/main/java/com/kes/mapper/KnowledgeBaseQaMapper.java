package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.KnowledgeBaseQa;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeBaseQaMapper extends BaseMapper<KnowledgeBaseQa> {

    KnowledgeBaseQa selectByUuid(@Param("uuid") String uuid);

    List<KnowledgeBaseQa> selectByKbUuid(@Param("kbUuid") String kbUuid);

    int countByKbUuid(@Param("kbUuid") String kbUuid);
}