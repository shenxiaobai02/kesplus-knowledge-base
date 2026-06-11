package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select("SELECT role_uuid FROM kes_user_role WHERE user_id = #{userId}")
    List<String> selectRoleUuidsByUserId(@Param("userId") Long userId);

    @Select("SELECT ur.role_uuid FROM kes_user_role ur JOIN kes_role r ON ur.role_uuid = r.uuid WHERE ur.user_id = #{userId} AND r.tenant_uuid = #{tenantUuid}")
    List<String> selectRoleUuidsByUserIdAndTenant(@Param("userId") Long userId, @Param("tenantUuid") String tenantUuid);

    @Delete("DELETE FROM kes_user_role WHERE user_id = #{userId} AND role_uuid = #{roleUuid}")
    void deleteByUserIdAndRoleUuid(@Param("userId") Long userId, @Param("roleUuid") String roleUuid);

    @Delete("DELETE FROM kes_user_role WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM kes_user_role WHERE role_uuid = #{roleUuid}")
    void deleteByRoleUuid(@Param("roleUuid") String roleUuid);
}