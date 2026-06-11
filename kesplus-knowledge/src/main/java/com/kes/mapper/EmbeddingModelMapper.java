package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.EmbeddingModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmbeddingModelMapper extends BaseMapper<EmbeddingModel> {

    @Select("SELECT * FROM kes_embedding_model WHERE uuid = #{uuid}")
    EmbeddingModel selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM kes_embedding_model WHERE model_name = #{modelName}")
    EmbeddingModel selectByModelName(@Param("modelName") String modelName);

    @Select("SELECT * FROM kes_embedding_model WHERE embedding_dimension = #{dimension}")
    List<EmbeddingModel> selectByDimension(@Param("dimension") Integer dimension);

    @Select("SELECT * FROM kes_embedding_model WHERE is_active = true ORDER BY created_time DESC")
    List<EmbeddingModel> selectActiveModels();

    @Select("SELECT COUNT(*) FROM kes_embedding_model WHERE model_name = #{modelName}")
    int countByModelName(@Param("modelName") String modelName);

    @Select("SELECT COUNT(*) FROM kes_embedding_model WHERE embedding_dimension = #{dimension}")
    int countByDimension(@Param("dimension") Integer dimension);
}