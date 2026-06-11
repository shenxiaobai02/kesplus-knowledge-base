package com.kes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    void testHealthAllUp() throws Exception {
        ValueOperations<String, String> valueOps = (ValueOperations<String, String>) when(redisTemplate.opsForValue()).getMock();
        when(valueOps.get("health_check")).thenReturn("ok");

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.components.database").value("UP"))
                .andExpect(jsonPath("$.data.components.redis").value("UP"));
    }

    @Test
    void testHealthDatabaseDown() throws Exception {
        doThrow(new RuntimeException("Connection failed")).when(jdbcTemplate).execute("SELECT 1");
        ValueOperations<String, String> valueOps = (ValueOperations<String, String>) when(redisTemplate.opsForValue()).getMock();
        when(valueOps.get("health_check")).thenReturn("ok");

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.components.database").value("DOWN"))
                .andExpect(jsonPath("$.data.components.redis").value("UP"));
    }

    @Test
    void testHealthRedisDown() throws Exception {
        doThrow(new RuntimeException("Redis connection failed")).when(redisTemplate).opsForValue();

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.components.database").value("UP"))
                .andExpect(jsonPath("$.data.components.redis").value("DOWN"));
    }

    @Test
    void testReady() throws Exception {
        mockMvc.perform(get("/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Ready"));
    }

    @Test
    void testLive() throws Exception {
        mockMvc.perform(get("/health/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Live"));
    }
}
