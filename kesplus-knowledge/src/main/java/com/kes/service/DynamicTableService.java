package com.kes.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Service
public class DynamicTableService {

    private static final String TABLE_PREFIX = "kes_knowledge_base_embedding_";

    @Autowired
    private DataSource dataSource;

    public String getTableName(int dimension) {
        return TABLE_PREFIX + dimension;
    }

    public void createTableIfNotExists(int dimension) throws SQLException {
        String tableName = getTableName(dimension);
        if (tableExists(tableName)) {
            log.debug("Table {} already exists", tableName);
            return;
        }

        String tableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id BIGSERIAL PRIMARY KEY,
                uuid VARCHAR(36) UNIQUE NOT NULL,
                kb_uuid VARCHAR(36) NOT NULL,
                kb_item_uuid VARCHAR(36),
                embedding vector(%d) NOT NULL,
                text TEXT NOT NULL,
                metadata_json TEXT,
                created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """, tableName, dimension);

        String indexSql = String.format("""
            CREATE INDEX IF NOT EXISTS idx_%s_kb_uuid ON %s(kb_uuid);
            CREATE INDEX IF NOT EXISTS idx_%s_vector ON %s USING hnsw (embedding vector_cosine_ops);
            """, tableName.replace("kes_", ""), tableName,
                tableName.replace("kes_", ""), tableName);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(tableSql);
            stmt.execute(indexSql);
            log.info("Created embedding table: {}", tableName);
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        String sql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    public void dropTable(int dimension) throws SQLException {
        String tableName = getTableName(dimension);
        if (!tableExists(tableName)) {
            return;
        }

        String sql = String.format("DROP TABLE IF EXISTS %s", tableName);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Dropped embedding table: {}", tableName);
        }
    }

    public int countRecords(int dimension, String kbUuid) throws SQLException {
        String tableName = getTableName(dimension);
        if (!tableExists(tableName)) {
            return 0;
        }

        String sql = String.format("SELECT COUNT(*) FROM %s WHERE kb_uuid = ?", tableName);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, kbUuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void deleteByKbUuid(int dimension, String kbUuid) throws SQLException {
        String tableName = getTableName(dimension);
        if (!tableExists(tableName)) {
            return;
        }

        String sql = String.format("DELETE FROM %s WHERE kb_uuid = ?", tableName);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, kbUuid);
            int deleted = pstmt.executeUpdate();
            log.info("Deleted {} records from {} for kb_uuid: {}", deleted, tableName, kbUuid);
        }
    }
}