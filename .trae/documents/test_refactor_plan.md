# 测试用例重构计划

## 一、当前测试现状分析

### 1.1 测试文件分类

| 类型 | 文件 | 当前状态 | 说明 |
|------|------|----------|------|
| 集成测试 | KnowledgeBaseApplicationTests | @SpringBootTest | 上下文加载测试 |
| 单元测试 | HealthControllerTest | 纯单元测试 | 已移除mock |
| Controller测试 | KnowledgeBaseControllerTest | @WebMvcTest + @MockBean | 需要重构 |
| Controller测试 | KnowledgeBaseQAControllerTest | @WebMvcTest + @MockBean | 需要重构 |
| Controller测试 | RoleControllerTest | @WebMvcTest + @MockBean | 需要重构 |
| 纯单元测试 | ErrorCodeTest | 无mock | 无需修改 |
| 纯单元测试 | SmartDocumentSplitterTest | 无mock | 无需修改 |
| Service测试 | EmbeddingModelServiceTest | Mockito mock | 需要重构 |
| Service测试 | EmbeddingRagServiceTest | Mockito mock | 需要重构 |
| 纯单元测试 | FileStorageServiceTest | 无mock | 无需修改 |
| Service测试 | KnowledgeBaseItemServiceTest | Mockito mock | 需要重构 |
| Service测试 | KnowledgeBaseQaServiceTest | Mockito mock | 需要重构 |
| 纯单元测试 | KnowledgeBaseServiceTest | 无mock | 无需修改 |
| 集成测试 | ModelIntegrationTest | @SpringBootTest | 需要外部服务 |
| Service测试 | RoleServiceTest | Mockito mock | 需要重构 |
| Service测试 | TenantServiceTest | Mockito mock | 需要重构 |

### 1.2 需要重构的测试文件

共 **9** 个测试文件需要从mock测试改为实际场景测试：
1. `KnowledgeBaseControllerTest.java`
2. `KnowledgeBaseQAControllerTest.java`
3. `RoleControllerTest.java`
4. `EmbeddingModelServiceTest.java`
5. `EmbeddingRagServiceTest.java`
6. `KnowledgeBaseItemServiceTest.java`
7. `KnowledgeBaseQaServiceTest.java`
8. `RoleServiceTest.java`
9. `TenantServiceTest.java`

---

## 二、重构方案

### 2.1 技术选型

- **数据库**: H2 内存数据库（已在 `application.yml` 中配置）
- **测试注解**: `@SpringBootTest` + `@Transactional`
- **数据准备**: `@BeforeEach` 中初始化测试数据
- **测试数据**: 使用真实的实体对象和Mapper操作

### 2.2 测试配置

已存在的测试配置文件 `src/test/resources/application.yml` 配置：
- H2 内存数据库
- 排除 Redis 自动配置（测试环境不需要）
- MyBatis-Plus 配置

### 2.3 重构策略

#### Controller层测试
- 使用 `@SpringBootTest` + `MockMvc` 替代 `@WebMvcTest`
- 注入真实的Service而非MockBean
- 测试前准备真实数据，测试后回滚

#### Service层测试
- 使用 `@SpringBootTest` 替代 `@ExtendWith(MockitoExtension.class)`
- 注入真实的Mapper和依赖Service
- 通过真实数据库操作验证CRUD功能

---

## 三、实施步骤

### 步骤1：重构Service层测试（优先级高）

| 序号 | 文件 | 预计耗时 | 依赖 |
|------|------|----------|------|
| 1 | TenantServiceTest.java | 30分钟 | TenantMapper |
| 2 | RoleServiceTest.java | 30分钟 | RoleMapper, UserRoleMapper |
| 3 | KnowledgeBaseServiceTest.java | 20分钟 | 已完成 |
| 4 | KnowledgeBaseItemServiceTest.java | 30分钟 | KnowledgeBaseItemMapper |
| 5 | KnowledgeBaseQaServiceTest.java | 30分钟 | KnowledgeBaseQaMapper |
| 6 | EmbeddingModelServiceTest.java | 30分钟 | EmbeddingModelMapper |
| 7 | EmbeddingRagServiceTest.java | 40分钟 | EmbeddingMapper, DynamicTableService |

### 步骤2：重构Controller层测试

| 序号 | 文件 | 预计耗时 | 依赖 |
|------|------|----------|------|
| 1 | RoleControllerTest.java | 30分钟 | RoleService |
| 2 | KnowledgeBaseControllerTest.java | 30分钟 | KnowledgeBaseService |
| 3 | KnowledgeBaseQAControllerTest.java | 30分钟 | KnowledgeBaseQaService |

### 步骤3：验证所有测试通过

运行完整测试套件，确保所有测试通过。

---

## 四、风险与注意事项

### 4.1 风险评估

| 风险 | 描述 | 应对措施 |
|------|------|----------|
| 数据污染 | 测试数据影响其他测试 | 使用 `@Transactional` 自动回滚 |
| 依赖缺失 | 某些Service依赖外部服务 | 使用测试配置排除或mock外部依赖 |
| 性能问题 | 真实数据库操作较慢 | 使用H2内存数据库 |
| Redis依赖 | 部分代码依赖Redis | 在测试配置中排除Redis自动配置 |

### 4.2 注意事项

1. **事务管理**: 所有测试方法应使用 `@Transactional` 确保数据隔离
2. **数据准备**: 在 `@BeforeEach` 中创建测试数据，`@AfterEach` 清理
3. **测试顺序**: 确保测试方法之间相互独立
4. **断言覆盖**: 覆盖正常路径和异常路径

---

## 五、预期成果

重构完成后，测试用例将具备以下特点：

1. **真实性**: 使用真实的数据库操作验证业务逻辑
2. **可靠性**: 测试结果更接近生产环境
3. **可维护性**: 测试代码更简洁，无需维护mock配置
4. **完整性**: 覆盖更多边界情况和异常场景

---

## 六、文件修改清单

```
src/test/java/com/kes/service/
├── TenantServiceTest.java          # 重构
├── RoleServiceTest.java            # 重构
├── KnowledgeBaseItemServiceTest.java # 重构
├── KnowledgeBaseQaServiceTest.java # 重构
├── EmbeddingModelServiceTest.java  # 重构
└── EmbeddingRagServiceTest.java    # 重构

src/test/java/com/kes/controller/
├── RoleControllerTest.java         # 重构
├── KnowledgeBaseControllerTest.java # 重构
└── KnowledgeBaseQAControllerTest.java # 重构
```

---

## 七、测试验证

重构完成后执行以下命令验证：

```bash
mvn test -pl kesplus-knowledge
```

预期结果：所有测试通过，无失败或错误。
