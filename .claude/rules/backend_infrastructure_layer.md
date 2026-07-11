---
alwaysApply: false
paths:
  - "**/backend/infr/**"
---
# Infrastructure 层（基础设施层）约束

当你在 `backend/infr/` 目录下编写或修改代码时，必须遵守以下规则：

## 路径结构

- `infr/config/` — 数据库连接、第三方配置
- `infr/repository/` — Repository 实现 + ORM model + 数据库迁移
- `infr/client/` — 外部网关实现（实现 domain/acl/ 中的抽象接口）
- 其他适配器按职责划分目录

## 允许

- 实现领域层定义的 Repository 抽象接口
- 实现领域层定义的 ACL 抽象接口
- 领域对象与 ORM Model 的双向转换
- 数据库访问、所有增删改查操作
- 外部系统集成、中间件配置、缓存管理
- 使用框架和 I/O 库

## 禁止

- 包含任何业务逻辑
- 修改聚合根的业务状态（业务状态变更属于领域层）
- 在仓储方法中进行业务规则校验
- 依赖 OHS 层或 Application 层

## Repository 实现规范

- 实现 domain/*/repository/ 中定义的抽象接口
- 方法入参：只能是领域模型或关键字段（如 ID）
- 方法返回值：必须是领域模型（禁止返回 ORM Model）
- 负责 Domain Model ↔ ORM Model 双向转换
- save 方法内部判断是新增还是更新

## Client 实现规范

- 实现 domain/*/acl/ 中定义的抽象接口
- 封装外部系统的协议细节（HTTP、gRPC 等）
- 将外部响应转换为领域模型返回

## 命名

- 仓储实现：`{聚合根名}RepositoryImpl`
- ORM Model：`{实体名}Model`
- Client：`{外部系统名}Client`

## 依赖方向

- 可依赖：Domain 层（实现其接口）
- 禁止依赖：OHS 层、Application 层
