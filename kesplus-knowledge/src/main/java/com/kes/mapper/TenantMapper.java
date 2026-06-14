package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 租户Mapper
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

    @Select("SELECT id, uuid, code, name, description, status, config_json, is_deleted, " +
            "created_time, updated_time " +
            "FROM kes_tenant WHERE uuid = #{uuid}")
    Tenant selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT id, uuid, code, name, description, status, config_json, is_deleted, " +
            "created_time, updated_time " +
            "FROM kes_tenant WHERE code = #{code}")
    Tenant selectByCode(@Param("code") String code);

    @Select("SELECT id, uuid, code, name, description, status, config_json, is_deleted, " +
            "created_time, updated_time " +
            "FROM kes_tenant WHERE is_deleted = FALSE ORDER BY created_time DESC")
    List<Tenant> selectAllActive();
}
