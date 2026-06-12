package com.kes.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            org.springframework.core.io.Resource[] resources = resolver.getResources("classpath:mapper/**/*.xml");
            if (resources != null && resources.length > 0) {
                factoryBean.setMapperLocations(resources);
            }
        } catch (Exception e) {
            // mapper目录下没有XML文件是正常的，使用注解方式
        }
        
        return factoryBean.getObject();
    }
}
