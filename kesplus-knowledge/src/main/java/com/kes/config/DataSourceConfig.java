package com.kes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;

@Configuration
public class DataSourceConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.timeout:3000ms}")
    private Duration redisTimeout;

    // 连接池配置
    @Value("${spring.data.redis.pool.max-total:20}")
    private int maxTotal;

    @Value("${spring.data.redis.pool.max-idle:10}")
    private int maxIdle;

    @Value("${spring.data.redis.pool.min-idle:5}")
    private int minIdle;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Redis  standalone 配置
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        // Jedis 连接池配置
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(Duration.ofMillis(redisTimeout.toMillis()));

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
