package com.kes.controller;

import com.kes.common.ResponseWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthControllerTest {

    @Test
    void testHealthAllUp() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("health_check")).thenReturn("ok");

        HealthController controller = new HealthController(jdbcTemplate, redisTemplate);
        ResponseWrapper<Map<String, Object>> response = controller.health();

        assertNotNull(response);
        assertEquals("SUCCESS", response.getCode());
        assertEquals("UP", response.getData().get("status"));
        assertEquals("UP", ((Map<?, ?>) response.getData().get("components")).get("database"));
        assertEquals("UP", ((Map<?, ?>) response.getData().get("components")).get("redis"));
    }

    @Test
    void testHealthDatabaseDown() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        
        doThrow(new RuntimeException("Connection failed")).when(jdbcTemplate).execute("SELECT 1");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("health_check")).thenReturn("ok");

        HealthController controller = new HealthController(jdbcTemplate, redisTemplate);
        ResponseWrapper<Map<String, Object>> response = controller.health();

        assertNotNull(response);
        assertEquals("SUCCESS", response.getCode());
        assertEquals("DOWN", ((Map<?, ?>) response.getData().get("components")).get("database"));
        assertEquals("UP", ((Map<?, ?>) response.getData().get("components")).get("redis"));
    }

    @Test
    void testHealthRedisDown() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        
        doThrow(new RuntimeException("Redis connection failed")).when(redisTemplate).opsForValue();

        HealthController controller = new HealthController(jdbcTemplate, redisTemplate);
        ResponseWrapper<Map<String, Object>> response = controller.health();

        assertNotNull(response);
        assertEquals("SUCCESS", response.getCode());
        assertEquals("UP", ((Map<?, ?>) response.getData().get("components")).get("database"));
        assertEquals("DOWN", ((Map<?, ?>) response.getData().get("components")).get("redis"));
    }

    @Test
    void testReady() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        HealthController controller = new HealthController(jdbcTemplate, redisTemplate);
        ResponseWrapper<String> response = controller.ready();

        assertNotNull(response);
        assertEquals("SUCCESS", response.getCode());
        assertEquals("Ready", response.getMessage());
    }

    @Test
    void testLive() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        HealthController controller = new HealthController(jdbcTemplate, redisTemplate);
        ResponseWrapper<String> response = controller.live();

        assertNotNull(response);
        assertEquals("SUCCESS", response.getCode());
        assertEquals("Live", response.getMessage());
    }
}
