package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.BusinessLine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessLineMapper extends BaseMapper<BusinessLine> {

    BusinessLine selectByUuid(@Param("uuid") String uuid);

    List<BusinessLine> selectByTenantUuid(@Param("tenantUuid") String tenantUuid);

    BusinessLine selectByTenantAndCode(@Param("tenantUuid") String tenantUuid, @Param("code") String code);
}