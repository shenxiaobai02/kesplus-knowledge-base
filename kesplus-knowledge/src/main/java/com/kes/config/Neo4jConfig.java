package com.kes.config;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Neo4j配置类
 * 仅在storage-type为neo4j时生效
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rag.graph.storage-type", havingValue = "neo4j")
public class Neo4jConfig {

    @Value("${rag.graph.neo4j.uri:bolt://localhost:7687}")
    private String uri;

    @Value("${rag.graph.neo4j.username:neo4j}")
    private String username;

    @Value("${rag.graph.neo4j.password:password}")
    private String password;

    @Value("${rag.graph.neo4j.max-connection-pool-size:50}")
    private int maxConnectionPoolSize;

    @Value("${rag.graph.neo4j.connection-acquisition-timeout:60000}")
    private long connectionAcquisitionTimeout;

    /**
     * 创建Neo4j Driver Bean
     */
    @Bean
    public Driver neo4jDriver() {
        log.info("Initializing Neo4j Driver with uri: {}", uri);
        
        org.neo4j.driver.Config config = org.neo4j.driver.Config.builder()
                .withMaxConnectionPoolSize(maxConnectionPoolSize)
                .withConnectionAcquisitionTimeout(connectionAcquisitionTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
        
        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password), config);
        
        // 验证连接
        try {
            driver.verifyConnectivity();
            log.info("Neo4j Driver connected successfully");
        } catch (Exception e) {
            log.warn("Neo4j connection verification failed, but driver will be created anyway: {}", e.getMessage());
        }
        
        return driver;
    }
}