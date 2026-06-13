package com.kes.config;

import com.kes.filter.UserContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * <p>
 * 注册Servlet过滤器，配置Web相关组件
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 注册用户上下文注入过滤器
     * <p>
     * 该过滤器负责从请求头获取用户信息并注入到ThreadContext中，
     * 供后续权限校验和审计日志使用
     * </p>
     *
     * @param userContextFilter 用户上下文过滤器
     * @return 过滤器注册Bean
     */
    @Bean
    public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration(
            UserContextFilter userContextFilter) {
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(userContextFilter);
        registration.addUrlPatterns("/api/*");
        registration.setName("userContextFilter");
        registration.setOrder(1);
        return registration;
    }
}