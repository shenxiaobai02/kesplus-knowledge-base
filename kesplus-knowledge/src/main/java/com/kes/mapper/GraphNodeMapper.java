package com.kes.mapper;

import com.kes.entity.KnowledgeBaseGraphNode;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 图谱节点Mapper
 * 用于PostgreSQL方案的图谱存储
 */
@Mapper
public interface GraphNodeMapper {

    @Insert("INSERT INTO kes_knowledge_base_graph_node (uuid, kb_uuid, node_type, node_id, content, metadata_json) " +
            "VALUES (#{node.uuid}, #{node.kbUuid}, #{node.nodeType}, #{node.nodeId}, #{node.content}, #{node.metadataJson})")
    void insert(@Param("node") KnowledgeBaseGraphNode node);

    @Insert("<script>" +
            "INSERT INTO kes_knowledge_base_graph_node (uuid, kb_uuid, node_type, node_id, content, metadata_json) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.uuid}, #{item.kbUuid}, #{item.nodeType}, #{item.nodeId}, #{item.content}, #{item.metadataJson})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<KnowledgeBaseGraphNode> list);

    @Select("SELECT id, uuid, kb_uuid, node_type, node_id, content, metadata_json, created_time " +
            "FROM kes_knowledge_base_graph_node WHERE uuid = #{uuid}")
    KnowledgeBaseGraphNode selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT id, uuid, kb_uuid, node_type, node_id, content, metadata_json, created_time " +
            "FROM kes_knowledge_base_graph_node WHERE kb_uuid = #{kbUuid} AND node_id = #{nodeId}")
    KnowledgeBaseGraphNode selectByNodeId(@Param("kbUuid") String kbUuid, @Param("nodeId") String nodeId);

    @Select("SELECT id, uuid, kb_uuid, node_type, node_id, content, metadata_json, created_time " +
            "FROM kes_knowledge_base_graph_node WHERE kb_uuid = #{kbUuid}")
    List<KnowledgeBaseGraphNode> selectByKbUuid(@Param("kbUuid") String kbUuid);

    @Select("SELECT id, uuid, kb_uuid, node_type, node_id, content, metadata_json, created_time " +
            "FROM kes_knowledge_base_graph_node WHERE kb_uuid = #{kbUuid} AND content LIKE CONCAT('%', #{query}, '%') LIMIT #{limit}")
    List<KnowledgeBaseGraphNode> searchByContent(@Param("kbUuid") String kbUuid, @Param("query") String query, @Param("limit") int limit);

    @Delete("DELETE FROM kes_knowledge_base_graph_node WHERE kb_uuid = #{kbUuid}")
    int deleteByKbUuid(@Param("kbUuid") String kbUuid);

    @Select("SELECT COUNT(*) FROM kes_knowledge_base_graph_node WHERE kb_uuid = #{kbUuid}")
    int countByKbUuid(@Param("kbUuid") String kbUuid);

    @Select("SELECT n.id, n.uuid, n.kb_uuid, n.node_type, n.node_id, n.content, n.metadata_json, n.created_time " +
            "FROM kes_knowledge_base_graph_node n " +
            "INNER JOIN kes_knowledge_base_graph_edge e ON n.uuid = e.target_node_uuid " +
            "WHERE e.source_node_uuid = #{nodeUuid} " +
            "LIMIT #{limit}")
    List<KnowledgeBaseGraphNode> selectRelatedNodes(@Param("nodeUuid") String nodeUuid, @Param("limit") int limit);
}
