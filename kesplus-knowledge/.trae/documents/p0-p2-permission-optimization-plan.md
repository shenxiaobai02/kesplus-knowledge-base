# P0-P2阶段权限控制与架构优化改进计划

## 一、改进目标概述

基于开发计划文档（P0-P2阶段）和研发规范要求，对项目进行系统性优化，重点解决：
1. 权限控制机制落地（Controller层、Service层权限校验）
2. 用户上下文传递机制完善
3. 审计日志自动化记录
4. 业务服务层架构解耦

## 二、当前状态分析

### 2.1 已完成功能（无需修改）

| 模块 | 文件 | 状态 |
|------|------|------|
| 用户上下文工具 | [ThreadContext.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/util/ThreadContext.java) | ✅ 已实现，可直接使用 |
| 权限服务框架 | [KnowledgeBasePermissionService.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/service/KnowledgeBasePermissionService.java) | ✅ 基本逻辑完整 |
| 异常处理 | [GlobalExceptionHandler.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/exception/GlobalExceptionHandler.java) | ✅ 已实现 |
| 错误码定义 | [ErrorCode.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/exception/ErrorCode.java) | ✅ 已定义FORBIDDEN |
| 数据保护 | [DataProtectionService.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/service/DataProtectionService.java) | ✅ 已实现 |

### 2.2 待改进功能

| 问题 | 位置 | 影响 |
|------|------|------|
| evaluateExpression()直接返回true | [FineGrainedPermissionService.java#L49-51](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/service/FineGrainedPermissionService.java#L49-L51) | 权限校验无效 |
| Controller无权限校验 | [KnowledgeBaseController.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/controller/KnowledgeBaseController.java) | 越权访问风险 |
| 问答服务无权限校验 | [KnowledgeBaseQaService.java#L64-95](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/service/KnowledgeBaseQaService.java#L64-L95) | 越权访问风险 |
| 缺少用户上下文注入过滤器 | - | ThreadContext未自动填充 |
| 审计日志未自动调用 | [AuditService.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/service/AuditService.java) | 操作无记录 |

## 三、具体改进方案

### 3.1 新增文件

#### 3.1.1 用户上下文注入过滤器
**文件**: `src/main/java/com/kes/filter/UserContextFilter.java`
**目的**: 从请求头获取用户信息，注入ThreadContext
**实现要点**:
- 从请求头读取 X-User-Id、X-User-Name、X-Tenant-Uuid
- 构建 ThreadContext.UserContext 对象
- 在请求结束后清理 ThreadContext

#### 3.1.2 审计日志注解
**文件**: `src/main/java/com/kes/annotation/AuditLog.java`
**目的**: 标记需要记录审计日志的方法
**实现要点**:
- 定义 operationType、resourceType 属性

#### 3.1.3 审计日志切面
**文件**: `src/main/java/com/kes/aspect/AuditLogAspect.java`
**目的**: 自动拦截标记注解的方法，记录审计日志
**实现要点**:
- 使用 @Around 拦截
- 成功/失败分别记录
- 从 ThreadContext 获取用户信息

### 3.2 修改文件

#### 3.2.1 完善 FineGrainedPermissionService
**文件**: [FineGrainedPermissionService.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/service/FineGrainedPermissionService.java)
**修改内容**:
- 实现 evaluateExpression() 方法，基于角色权限进行评估
- 添加 RoleService 依赖注入
- 添加资源级别权限检查逻辑

#### 3.2.2 KnowledgeBaseController 添加权限校验
**文件**: [KnowledgeBaseController.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/controller/KnowledgeBaseController.java)
**修改内容**:
- 注入 KnowledgeBasePermissionService
- 在 get()、update()、delete()、uploadDocument() 等方法添加权限校验
- 使用 ThreadContext.getCurrentUserId() 获取用户ID
- 无权限时返回 ResponseWrapper.error("无权限访问", 403)

#### 3.2.3 KnowledgeBaseQaService 添加权限校验
**文件**: [KnowledgeBaseQaService.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/service/KnowledgeBaseQaService.java)
**修改内容**:
- 注入 KnowledgeBasePermissionService
- 在 qa() 方法开头添加权限校验
- 在 streamQa() 方法开头添加权限校验
- 无权限时抛出 BaseException(ErrorCode.FORBIDDEN)

#### 3.2.4 AuditService 增强用户上下文获取
**文件**: [AuditService.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/service/AuditService.java)
**修改内容**:
- 在 log() 方法中使用 ThreadContext 获取用户信息
- 添加 clientIp、userAgent 获取

#### 3.2.5 ErrorCode 添加权限相关错误码
**文件**: [ErrorCode.java](file:///e:/Desktop/Java/aideepin/kesplus-knowledge-base/kesplus-knowledge/src/main/java/com/kes/exception/ErrorCode.java)
**修改内容**:
- 添加 PERMISSION_DENIED(4031, "Permission denied")
- 添加 KNOWLEDGE_BASE_ACCESS_DENIED(4032, "Knowledge base access denied")

### 3.3 配置文件修改

#### 3.3.1 注册过滤器
**文件**: `src/main/java/com/kes/config/WebConfig.java`（新增）
**内容**: 注册 UserContextFilter 到过滤器链

## 四、实施步骤

### Step 1: 创建用户上下文注入过滤器（优先级：P0）
1. 创建 `filter/UserContextFilter.java`
2. 创建 `config/WebConfig.java` 注册过滤器
3. 编写单元测试验证过滤器功能

### Step 2: 完善权限服务逻辑（优先级：P0）
1. 修改 `FineGrainedPermissionService.java`
2. 实现 evaluateExpression() 方法
3. 编写单元测试验证权限逻辑

### Step 3: Controller层权限集成（优先级：P0）
1. 修改 `KnowledgeBaseController.java`
2. 修改 `KnowledgeBaseQAController.java`
3. 修改 `RoleController.java`
4. 修改 `TenantController.java`
5. 编写集成测试验证权限控制

### Step 4: Service层权限集成（优先级：P1）
1. 修改 `KnowledgeBaseQaService.java`
2. 在问答流程添加权限校验

### Step 5: 审计日志自动化（优先级：P1）
1. 创建 `annotation/AuditLog.java`
2. 创建 `aspect/AuditLogAspect.java`
3. 修改 `AuditService.java` 增强用户信息获取
4. 在关键方法添加 @AuditLog 注解

### Step 6: 错误码完善（优先级：P2）
1. 修改 `ErrorCode.java` 添加权限相关错误码

## 五、技术选型与规范遵循

### 5.1 遵循研发规范要点

| 规范项 | 要求 | 实施方式 |
|------|------|---------|
| 命名规范 | 小驼峰方法名、语义化变量 | checkPermission、hasAccess 等 |
| 注释规范 | 公有方法添加文档注释 | 所有新增public方法添加JavaDoc |
| 异常处理 | 禁止空catch、统一业务异常 | 使用 BaseException 包装 |
| 日志规范 | 携带链路ID、脱敏入参 | 使用 ThreadContext.getRequestId() |
| 分层隔离 | Controller仅做参数校验 | 权限校验调用Service，不写逻辑 |

### 5.2 Git提交规范

提交格式：【类型】[需求单号] 简短描述 | 核心代码变更点

示例：
- 【feat】[P2-权限] 新增用户上下文注入过滤器 | 1.创建UserContextFilter 2.注册过滤器链
- 【refactor】[P2-权限] 完善权限校验逻辑 | 1.实现evaluateExpression 2.添加角色权限检查

## 六、潜在风险与缓解措施

| 风险 | 概率 | 缓解措施 |
|------|------|---------|
| ThreadContext未清理导致内存泄漏 | 中 | Filter finally块强制清理 |
| 权限校验影响API响应性能 | 低 | 权限结果缓存（后续优化） |
| 测试环境无认证网关 | 高 | 提供Mock用户头信息测试方案 |

## 七、验证步骤

### 7.1 单元测试验证
- UserContextFilter 测试：模拟请求头，验证ThreadContext填充
- FineGrainedPermissionService 测试：模拟角色，验证权限判断
- 各Controller测试：模拟无权限场景，验证403响应

### 7.2 集成测试验证
- 启动应用，使用Mock用户头信息调用API
- 验证无权限用户无法访问私有知识库
- 验证审计日志正确记录

### 7.3 验收标准
- 所有Controller接口添加权限校验 ✅
- 问答流程权限校验生效 ✅
- 审计日志自动记录 ✅
- 单元测试覆盖率 ≥80% ✅
- 代码符合研发规范 ✅

## 八、文件变更清单

### 新增文件（3个）
1. `src/main/java/com/kes/filter/UserContextFilter.java`
2. `src/main/java/com/kes/annotation/AuditLog.java`
3. `src/main/java/com/kes/aspect/AuditLogAspect.java`

### 修改文件（6个）
1. `src/main/java/com/kes/service/FineGrainedPermissionService.java`
2. `src/main/java/com/kes/controller/KnowledgeBaseController.java`
3. `src/main/java/com/kes/controller/KnowledgeBaseQAController.java`
4. `src/main/java/com/kes/service/KnowledgeBaseQaService.java`
5. `src/main/java/com/kes/service/AuditService.java`
6. `src/main/java/com/kes/exception/ErrorCode.java`

---

**计划版本**: v1.0
**创建时间**: 2026-06-12
**遵循规范**: 研发工程师【纯开发阶段】规范（精简落地版）