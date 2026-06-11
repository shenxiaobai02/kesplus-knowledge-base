package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.KnowledgeBaseItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeBaseItemMapper extends BaseMapper<KnowledgeBaseItem> {

    @Select("SELECT * FROM kes_knowledge_base_item WHERE uuid = #{uuid}")
    KnowledgeBaseItem selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM kes_knowledge_base_item WHERE kb_uuid = #{kbUuid} ORDER BY created_time DESC")
    List<KnowledgeBaseItem> selectByKbUuid(@Param("kbUuid") String kbUuid);

    @Select("SELECT * FROM kes_knowledge_base_item WHERE kb_id = #{kbId} ORDER BY created_time DESC")
    List<KnowledgeBaseItem> selectByKbId(@Param("kbId") Long kbId);

    @Select("SELECT COUNT(*) FROM kes_knowledge_base_item WHERE kb_uuid = #{kbUuid}")
    int countByKbUuid(@Param("kbUuid") String kbUuid);

    @Delete("DELETE FROM kes_knowledge_base_item WHERE kb_uuid = #{kbUuid}")
    int deleteByKbUuid(@Param("kbUuid") String kbUuid);
}