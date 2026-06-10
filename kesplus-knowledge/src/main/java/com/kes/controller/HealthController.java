package com.kes.controller;

import com.kes.common.ResponseWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public ResponseWrapper<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        
        Map<String, Object> components = new HashMap<>();
        
        try {
            jdbcTemplate.execute("SELECT 1");
            components.put("database", "UP");
        } catch (Exception e) {
            components.put("database", "DOWN");
            components.put("database_error", e.getMessage());
        }
        
        try {
            redisTemplate.opsForValue().set("health_check", "ok", java.time.Duration.ofSeconds(10));
            String value = redisTemplate.opsForValue().get("health_check");
            components.put("redis", "ok".equals(value) ? "UP" : "DOWN");
        } catch (Exception e) {
            components.put("redis", "DOWN");
            components.put("redis_error", e.getMessage());
        }
        
        status.put("components", components);
        
        return ResponseWrapper.success(status);
    }

    @GetMapping("/ready")
    public ResponseWrapper<String> ready() {
        return ResponseWrapper.success("Ready");
    }

    @GetMapping("/live")
    public ResponseWrapper<String> live() {
        return ResponseWrapper.success("Live");
    }
}
