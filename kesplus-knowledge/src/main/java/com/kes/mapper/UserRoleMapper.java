package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    List<String> selectRoleUuidsByUserId(@Param("userId") Long userId);

    List<String> selectRoleUuidsByUserIdAndTenant(@Param("userId") Long userId, @Param("tenantUuid") String tenantUuid);

    void deleteByUserIdAndRoleUuid(@Param("userId") Long userId, @Param("roleUuid") String roleUuid);

    void deleteByUserId(@Param("userId") Long userId);

    void deleteByRoleUuid(@Param("roleUuid") String roleUuid);
}