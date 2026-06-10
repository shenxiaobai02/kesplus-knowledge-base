package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.EmbeddingModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmbeddingModelMapper extends BaseMapper<EmbeddingModel> {

    EmbeddingModel selectByUuid(@Param("uuid") String uuid);

    EmbeddingModel selectByModelName(@Param("modelName") String modelName);

    List<EmbeddingModel> selectByDimension(@Param("dimension") Integer dimension);

    List<EmbeddingModel> selectActiveModels();

    int countByModelName(@Param("modelName") String modelName);

    int countByDimension(@Param("dimension") Integer dimension);
}