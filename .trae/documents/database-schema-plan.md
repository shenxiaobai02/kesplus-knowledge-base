# 数据库Schema配置修改计划

## 1. 需求分析

根据业务/产品描述，需要将PostgreSQL数据库相关表创建在指定的数据库环境中：
- 数据库名称：`kesplus-knowledge`（已配置）
- Schema名称：`kesplus_knowledge_base`（需要新增配置）

## 2. 当前状态分析

### 2.1 数据库连接配置
当前`application.yml`中的数据库连接字符串：
```yaml
datasource:
  url: jdbc:postgresql://152.136.30.130:5432/kesplus-knowledge
```

### 2.2 MyBatis-Plus配置
当前MyBatis-Plus未配置全局schema，需要添加schema配置。

### 2.3 实体类@TableName注解
所有实体类使用简单表名，例如：
- `@TableName("kes_knowledge_base")`
- `@TableName("kes_embedding_model")`
- `@TableName("kes_knowledge_base_item")`
- `@TableName("kes_tenant")`
- `@TableName("kes_role")`
- `@TableName("kes_user_role")`
- `@TableName("kes_audit_log")`
- `@TableName("kes_business_line")`
- `@TableName("kes_knowledge_base_qa")`

### 2.4 Schema文件
`schema.sql`中的表创建语句未包含schema前缀。

### 2.5 Mapper XML文件
- `KnowledgeBaseMapper.xml`：使用硬编码表名`knowledge_base`（缺少前缀）
- `EmbeddingMapper.xml`：使用动态表名`${tableName}`

## 3. 修改方案

### 3.1 方案选择
采用**MyBatis-Plus全局schema配置**方案，这样可以：
1. 统一管理schema，避免在每个实体类中重复配置
2. 简化维护，如需变更schema只需修改一处

### 3.2 修改清单

| 文件路径 | 修改内容 |
|---------|---------|
| `application.yml` | 数据库连接字符串添加currentSchema参数 |
| `application.yml` | MyBatis-Plus配置添加全局schema |
| `schema.sql` | 所有表名前添加schema前缀 |
| `KnowledgeBaseMapper.xml` | 表名前添加schema前缀 |

## 4. 实施步骤

### 步骤1：修改数据库连接字符串
修改`application.yml`中的datasource配置，添加currentSchema参数：
```yaml
url: jdbc:postgresql://152.136.30.130:5432/kesplus-knowledge?currentSchema=kesplus_knowledge_base
```

### 步骤2：配置MyBatis-Plus全局schema
在mybatis-plus配置中添加schema配置：
```yaml
mybatis-plus:
  global-config:
    db-config:
      schema: kesplus_knowledge_base
```

### 步骤3：更新schema.sql
为所有CREATE TABLE、INSERT、INDEX语句添加schema前缀。

### 步骤4：更新Mapper XML文件
为`KnowledgeBaseMapper.xml`中的SQL语句添加schema前缀。

## 5. 风险评估

| 风险点 | 风险等级 | 应对措施 |
|-------|---------|---------|
| 现有数据迁移 | 中等 | 建议在修改前备份数据库 |
| 连接字符串格式错误 | 高 | 严格按照PostgreSQL JDBC格式配置 |
| Mapper XML中的硬编码表名 | 中等 | 检查并更新所有Mapper文件 |
| 第三方工具兼容性 | 低 | currentSchema是标准JDBC参数，兼容性良好 |

## 6. 验证方法

1. 启动应用程序，检查数据库连接是否成功
2. 执行DDL语句创建表，验证表是否在正确的schema下创建
3. 执行CRUD操作，验证数据读写是否正常

## 7. 修改文件清单

1. `kesplus-knowledge/src/main/resources/application.yml`
2. `kesplus-knowledge/src/main/resources/schema.sql`
3. `kesplus-knowledge/src/main/resources/mapper/KnowledgeBaseMapper.xml`
