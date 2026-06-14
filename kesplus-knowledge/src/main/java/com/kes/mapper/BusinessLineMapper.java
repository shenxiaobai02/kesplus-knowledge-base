package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.BusinessLine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业务线Mapper
 */
@Mapper
public interface BusinessLineMapper extends BaseMapper<BusinessLine> {

    @Select("SELECT uuid, tenant_uuid, code, name, description, created_time, updated_time " +
            "FROM kes_business_line WHERE uuid = #{uuid}")
    BusinessLine selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT id, uuid, tenant_uuid, code, name, description, created_time, updated_time " +
            "FROM kes_business_line WHERE tenant_uuid = #{tenantUuid} ORDER BY created_time DESC")
    List<BusinessLine> selectByTenantUuid(@Param("tenantUuid") String tenantUuid);

    @Select("SELECT uuid, tenant_uuid, code, name, description, created_time, updated_time " +
            "FROM kes_business_line WHERE tenant_uuid = #{tenantUuid} AND code = #{code}")
    BusinessLine selectByTenantAndCode(@Param("tenantUuid") String tenantUuid, @Param("code") String code);
}
