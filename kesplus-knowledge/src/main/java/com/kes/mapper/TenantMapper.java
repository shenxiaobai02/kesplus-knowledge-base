package com.kes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kes.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

    Tenant selectByUuid(@Param("uuid") String uuid);

    Tenant selectByCode(@Param("code") String code);

    List<Tenant> selectAllActive();
}