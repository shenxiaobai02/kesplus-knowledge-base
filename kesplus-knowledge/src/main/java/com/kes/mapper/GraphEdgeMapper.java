package com.kes.mapper;

import com.kes.entity.KnowledgeBaseGraphEdge;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 图谱关系Mapper
 * 用于PostgreSQL方案的图谱存储
 */
@Mapper
public interface GraphEdgeMapper {

    @Insert("INSERT INTO kes_knowledge_base_graph_edge (uuid, kb_uuid, source_node_uuid, target_node_uuid, relation_type, weight, metadata_json) " +
            "VALUES (#{edge.uuid}, #{edge.kbUuid}, #{edge.sourceNodeUuid}, #{edge.targetNodeUuid}, #{edge.relationType}, #{edge.weight}, #{edge.metadataJson})")
    void insert(@Param("edge") KnowledgeBaseGraphEdge edge);

    @Insert("<script>" +
            "INSERT INTO kes_knowledge_base_graph_edge (uuid, kb_uuid, source_node_uuid, target_node_uuid, relation_type, weight, metadata_json) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.uuid}, #{item.kbUuid}, #{item.sourceNodeUuid}, #{item.targetNodeUuid}, #{item.relationType}, #{item.weight}, #{item.metadataJson})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<KnowledgeBaseGraphEdge> list);

    @Select("SELECT id, uuid, kb_uuid, source_node_uuid, target_node_uuid, relation_type, weight, metadata_json, created_time " +
            "FROM kes_knowledge_base_graph_edge WHERE source_node_uuid = #{nodeUuid}")
    List<KnowledgeBaseGraphEdge> selectBySourceNodeUuid(@Param("nodeUuid") String nodeUuid);

    @Select("SELECT id, uuid, kb_uuid, source_node_uuid, target_node_uuid, relation_type, weight, metadata_json, created_time " +
            "FROM kes_knowledge_base_graph_edge WHERE target_node_uuid = #{nodeUuid}")
    List<KnowledgeBaseGraphEdge> selectByTargetNodeUuid(@Param("nodeUuid") String nodeUuid);

    @Select("SELECT id, uuid, kb_uuid, source_node_uuid, target_node_uuid, relation_type, weight, metadata_json, created_time " +
            "FROM kes_knowledge_base_graph_edge WHERE source_node_uuid = #{nodeUuid} OR target_node_uuid = #{nodeUuid}")
    List<KnowledgeBaseGraphEdge> selectByNodeUuid(@Param("nodeUuid") String nodeUuid);

    @Select("SELECT id, uuid, kb_uuid, source_node_uuid, target_node_uuid, relation_type, weight, metadata_json, created_time " +
            "FROM kes_knowledge_base_graph_edge WHERE kb_uuid = #{kbUuid}")
    List<KnowledgeBaseGraphEdge> selectByKbUuid(@Param("kbUuid") String kbUuid);

    @Delete("DELETE FROM kes_knowledge_base_graph_edge WHERE kb_uuid = #{kbUuid}")
    int deleteByKbUuid(@Param("kbUuid") String kbUuid);

    @Delete("DELETE FROM kes_knowledge_base_graph_edge WHERE source_node_uuid = #{nodeUuid} OR target_node_uuid = #{nodeUuid}")
    int deleteByNodeUuid(@Param("nodeUuid") String nodeUuid);

    @Select("SELECT COUNT(*) FROM kes_knowledge_base_graph_edge WHERE kb_uuid = #{kbUuid}")
    int countByKbUuid(@Param("kbUuid") String kbUuid);
}
