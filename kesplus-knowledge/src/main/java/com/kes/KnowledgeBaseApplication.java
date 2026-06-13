package com.kes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 知识库应用启动类
 * <p>
 * 作为应用程序入口点，负责启动Spring Boot应用。
 * 遵循单一职责原则，仅包含启动逻辑和必要的框架配置注解。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@SpringBootApplication
@MapperScan("com.kes.mapper")
@EnableConfigurationProperties
@EnableAsync
public class KnowledgeBaseApplication {

    /**
     * 应用程序入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseApplication.class, args);
    }
}
