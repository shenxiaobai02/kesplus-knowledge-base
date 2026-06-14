package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT id, uuid, code, name, description, tenant_uuid, permissions, is_system, is_deleted, " +
            "created_time, updated_time " +
            "FROM kes_role WHERE uuid = #{uuid}")
    Role selectByUuid(@Param("uuid") String uuid);

    @Select("SELECT id, uuid, code, name, description, tenant_uuid, permissions, is_system, is_deleted, " +
            "created_time, updated_time " +
            "FROM kes_role WHERE code = #{code}")
    Role selectByCode(@Param("code") String code);

    @Select("SELECT id, uuid, code, name, description, tenant_uuid, permissions, is_system, is_deleted, " +
            "created_time, updated_time " +
            "FROM kes_role WHERE tenant_uuid = #{tenantUuid} ORDER BY created_time DESC")
    List<Role> selectByTenantUuid(@Param("tenantUuid") String tenantUuid);

    @Select("SELECT id, uuid, code, name, description, tenant_uuid, permissions, is_system, is_deleted, " +
            "created_time, updated_time " +
            "FROM kes_role WHERE tenant_uuid IS NULL ORDER BY created_time DESC")
    List<Role> selectSystemRoles();
}
