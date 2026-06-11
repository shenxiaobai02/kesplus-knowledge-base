CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_embedding_model (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    embedding_dimension INT NOT NULL,
    model_type VARCHAR(50),
    base_url VARCHAR(255),
    api_key VARCHAR(255),
    config_json TEXT,
    is_active BOOLEAN DEFAULT true,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    title VARCHAR(100) NOT NULL,
    remark TEXT,
    is_public BOOLEAN DEFAULT false,
    is_strict BOOLEAN DEFAULT false,
    star_count INT DEFAULT 0,
    item_count INT DEFAULT 0,
    embedding_count INT DEFAULT 0,
    owner_uuid VARCHAR(36),
    owner_id BIGINT,
    owner_name VARCHAR(100),
    ingest_max_overlap INT DEFAULT 100,
    ingest_model_name VARCHAR(100),
    ingest_model_id BIGINT,
    ingest_token_estimator VARCHAR(50) DEFAULT 'openai',
    retrieve_max_results INT DEFAULT 5,
    retrieve_min_score DOUBLE PRECISION DEFAULT 0.6,
    query_llm_temperature DOUBLE PRECISION DEFAULT 0.7,
    query_system_message TEXT,
    embedding_model_uuid VARCHAR(36),
    embedding_dimension INT DEFAULT 1024,
    tenant_uuid VARCHAR(36),
    business_line_uuid VARCHAR(36),
    visibility VARCHAR(20) DEFAULT 'PRIVATE',
    allowed_role_codes TEXT,
    config_json TEXT,
    is_deleted BOOLEAN DEFAULT false,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_knowledge_base_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    kb_id BIGINT,
    kb_uuid VARCHAR(36),
    source_file_id BIGINT,
    title VARCHAR(255),
    brief VARCHAR(200),
    remark TEXT,
    is_deleted BOOLEAN DEFAULT false,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP,
    CONSTRAINT fk_kb_item_kb_id FOREIGN KEY (kb_id) REFERENCES kesplus_knowledge_base.kes_knowledge_base(id)
);

CREATE INDEX IF NOT EXISTS idx_kb_uuid ON kesplus_knowledge_base.kes_knowledge_base(uuid);
CREATE INDEX IF NOT EXISTS idx_kb_owner_id ON kesplus_knowledge_base.kes_knowledge_base(owner_id);
CREATE INDEX IF NOT EXISTS idx_kb_is_deleted ON kesplus_knowledge_base.kes_knowledge_base(is_deleted);

CREATE INDEX IF NOT EXISTS idx_kb_item_kb_uuid ON kesplus_knowledge_base.kes_knowledge_base_item(kb_uuid);
CREATE INDEX IF NOT EXISTS idx_kb_item_is_deleted ON kesplus_knowledge_base.kes_knowledge_base_item(is_deleted);

CREATE INDEX IF NOT EXISTS idx_emb_model_name ON kesplus_knowledge_base.kes_embedding_model(model_name);
CREATE INDEX IF NOT EXISTS idx_emb_model_dimension ON kesplus_knowledge_base.kes_embedding_model(embedding_dimension);
CREATE INDEX IF NOT EXISTS idx_emb_model_is_active ON kesplus_knowledge_base.kes_embedding_model(is_active);

INSERT INTO kesplus_knowledge_base.kes_embedding_model (uuid, model_name, embedding_dimension, model_type, base_url, is_active) 
SELECT 'default-bge-m3', 'BAAI/bge-m3', 1024, 'huggingface', 'https://api.siliconflow.cn/v1', true
WHERE NOT EXISTS (SELECT 1 FROM kesplus_knowledge_base.kes_embedding_model WHERE model_name = 'BAAI/bge-m3');
