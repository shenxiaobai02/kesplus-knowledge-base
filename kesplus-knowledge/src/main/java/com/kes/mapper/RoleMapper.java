package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    Role selectByUuid(@Param("uuid") String uuid);

    Role selectByCode(@Param("code") String code);

    List<Role> selectByTenantUuid(@Param("tenantUuid") String tenantUuid);

    List<Role> selectSystemRoles();
}