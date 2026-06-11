package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.KnowledgeBaseItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeBaseItemMapper extends BaseMapper<KnowledgeBaseItem> {

    KnowledgeBaseItem selectByUuid(@Param("uuid") String uuid);

    List<KnowledgeBaseItem> selectByKbUuid(@Param("kbUuid") String kbUuid);

    List<KnowledgeBaseItem> selectByKbId(@Param("kbId") Long kbId);

    int countByKbUuid(@Param("kbUuid") String kbUuid);

    int deleteByKbUuid(@Param("kbUuid") String kbUuid);
}