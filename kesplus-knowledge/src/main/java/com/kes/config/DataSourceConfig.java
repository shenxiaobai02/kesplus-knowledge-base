package com.kes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

@Configuration
public class DataSourceConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.timeout:3000ms}")
    private Duration redisTimeout;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Redis  standalone 配置
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        // Jedis 连接池配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);      // 最大连接数
        poolConfig.setMaxIdle(10);       // 最大空闲连接数
        poolConfig.setMinIdle(5);        // 最小空闲连接数
        poolConfig.setMaxWaitMillis(3000); // 最大等待时间

        // Jedis 客户端配置
        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .connectTimeout(redisTimeout)
                .readTimeout(redisTimeout)
                .usePooling()
                .poolConfig(poolConfig)
                .build();

        // 创建 Jedis 连接工厂
        return new JedisConnectionFactory(redisConfig, clientConfig);
    }
}
