package com.kes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * <p>
 * 负责RedisTemplate的Bean配置，管理Redis连接和序列化策略。
 * 遵循单一职责原则，专注于Redis相关配置。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * <p>
     * 使用StringRedisSerializer作为Key的序列化器，
     * 使用FastJson2RedisSerializer作为Value的序列化器，
     * 支持复杂对象的存储。
     * </p>
     *
     * @param connectionFactory Redis连接工厂
     * @return 配置好的RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        FastJson2RedisSerializer<Object> fastJsonSerializer = new FastJson2RedisSerializer<>(Object.class);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(fastJsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(fastJsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
