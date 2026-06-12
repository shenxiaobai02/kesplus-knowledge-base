CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_tenant (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    config_json TEXT,
    is_deleted BOOLEAN DEFAULT false,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_role (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    permissions TEXT,
    tenant_uuid VARCHAR(36),
    is_system BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_business_line (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    tenant_uuid VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    is_deleted BOOLEAN DEFAULT false,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_uuid VARCHAR(36) NOT NULL,
    tenant_uuid VARCHAR(36),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_audit_log (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    user_id BIGINT,
    user_name VARCHAR(100),
    tenant_uuid VARCHAR(36),
    operation_type VARCHAR(50),
    resource_type VARCHAR(50),
    resource_uuid VARCHAR(36),
    resource_name VARCHAR(200),
    operation_detail TEXT,
    success BOOLEAN,
    error_message TEXT,
    client_ip VARCHAR(50),
    user_agent VARCHAR(500),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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
    content TEXT,
    content_hash VARCHAR(64),
    file_path VARCHAR(500),
    file_size BIGINT,
    file_type VARCHAR(50),
    word_count INT,
    is_deleted BOOLEAN DEFAULT false,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP,
    CONSTRAINT fk_kb_item_kb_id FOREIGN KEY (kb_id) REFERENCES kesplus_knowledge_base.kes_knowledge_base(id)
);

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_knowledge_base_qa (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    kb_uuid VARCHAR(36) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    prompt_tokens INT DEFAULT 0,
    answer_tokens INT DEFAULT 0,
    score DOUBLE PRECISION,
    response_time_ms INT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT false,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kesplus_knowledge_base.kes_knowledge_base_embedding_1024 (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    kb_uuid VARCHAR(36) NOT NULL,
    kb_item_uuid VARCHAR(36),
    embedding vector(1024) NOT NULL,
    text TEXT NOT NULL,
    metadata_json TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenant_uuid ON kesplus_knowledge_base.kes_tenant(uuid);
CREATE INDEX IF NOT EXISTS idx_tenant_code ON kesplus_knowledge_base.kes_tenant(code);
CREATE INDEX IF NOT EXISTS idx_tenant_is_deleted ON kesplus_knowledge_base.kes_tenant(is_deleted);

CREATE INDEX IF NOT EXISTS idx_role_uuid ON kesplus_knowledge_base.kes_role(uuid);
CREATE INDEX IF NOT EXISTS idx_role_code ON kesplus_knowledge_base.kes_role(code);
CREATE INDEX IF NOT EXISTS idx_role_tenant_uuid ON kesplus_knowledge_base.kes_role(tenant_uuid);
CREATE INDEX IF NOT EXISTS idx_role_is_deleted ON kesplus_knowledge_base.kes_role(is_deleted);

CREATE INDEX IF NOT EXISTS idx_business_line_uuid ON kesplus_knowledge_base.kes_business_line(uuid);
CREATE INDEX IF NOT EXISTS idx_business_line_tenant_uuid ON kesplus_knowledge_base.kes_business_line(tenant_uuid);
CREATE INDEX IF NOT EXISTS idx_business_line_is_deleted ON kesplus_knowledge_base.kes_business_line(is_deleted);

CREATE INDEX IF NOT EXISTS idx_user_role_user_id ON kesplus_knowledge_base.kes_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role_uuid ON kesplus_knowledge_base.kes_user_role(role_uuid);
CREATE INDEX IF NOT EXISTS idx_user_role_tenant_uuid ON kesplus_knowledge_base.kes_user_role(tenant_uuid);

CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON kesplus_knowledge_base.kes_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_uuid ON kesplus_knowledge_base.kes_audit_log(tenant_uuid);
CREATE INDEX IF NOT EXISTS idx_audit_log_operation_type ON kesplus_knowledge_base.kes_audit_log(operation_type);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource_type ON kesplus_knowledge_base.kes_audit_log(resource_type);

CREATE INDEX IF NOT EXISTS idx_kb_uuid ON kesplus_knowledge_base.kes_knowledge_base(uuid);
CREATE INDEX IF NOT EXISTS idx_kb_owner_id ON kesplus_knowledge_base.kes_knowledge_base(owner_id);
CREATE INDEX IF NOT EXISTS idx_kb_tenant_uuid ON kesplus_knowledge_base.kes_knowledge_base(tenant_uuid);
CREATE INDEX IF NOT EXISTS idx_kb_is_deleted ON kesplus_knowledge_base.kes_knowledge_base(is_deleted);

CREATE INDEX IF NOT EXISTS idx_kb_item_kb_uuid ON kesplus_knowledge_base.kes_knowledge_base_item(kb_uuid);
CREATE INDEX IF NOT EXISTS idx_kb_item_is_deleted ON kesplus_knowledge_base.kes_knowledge_base_item(is_deleted);

CREATE INDEX IF NOT EXISTS idx_kb_qa_kb_uuid ON kesplus_knowledge_base.kes_knowledge_base_qa(kb_uuid);

CREATE INDEX IF NOT EXISTS idx_embedding_kb_uuid ON kesplus_knowledge_base.kes_knowledge_base_embedding_1024(kb_uuid);
CREATE INDEX IF NOT EXISTS idx_embedding_item_uuid ON kesplus_knowledge_base.kes_knowledge_base_embedding_1024(kb_item_uuid);

CREATE INDEX IF NOT EXISTS idx_emb_model_name ON kesplus_knowledge_base.kes_embedding_model(model_name);
CREATE INDEX IF NOT EXISTS idx_emb_model_dimension ON kesplus_knowledge_base.kes_embedding_model(embedding_dimension);
CREATE INDEX IF NOT EXISTS idx_emb_model_is_active ON kesplus_knowledge_base.kes_embedding_model(is_active);

INSERT INTO kesplus_knowledge_base.kes_embedding_model (uuid, model_name, embedding_dimension, model_type, base_url, is_active) 
SELECT 'default-bge-m3', 'BAAI/bge-m3', 1024, 'huggingface', 'https://api.siliconflow.cn/v1', true
WHERE NOT EXISTS (SELECT 1 FROM kesplus_knowledge_base.kes_embedding_model WHERE model_name = 'BAAI/bge-m3');
