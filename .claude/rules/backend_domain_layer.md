---
alwaysApply: false
paths:
  - "**/backend/domain/**"
---
# Domain 层（领域层）约束

当你在 `backend/domain/` 目录下编写或修改代码时，必须遵守以下规则：

## 路径结构

- `domain/{aggregate}/model/` — 聚合根、实体、值对象
- `domain/{aggregate}/repository/` — Repository 抽象接口（只定义接口）
- `domain/{aggregate}/acl/` — 防腐层抽象接口（可选，跨上下文协作时）
- `domain/{aggregate}/service/` — 领域服务（可选，跨实体逻辑时）

## 允许

- 定义聚合根、实体、值对象，并在其中实现业务规则
- 定义领域服务处理跨实体的业务逻辑
- 定义 Repository 抽象接口（只定义，不实现）
- 定义 ACL 防腐层抽象接口（只定义，不实现）
- 使用纯语言类型：数据类、抽象接口

## 禁止

- 依赖任何其他层（application、infr、ohs）
- import 任何框架或 I/O 库
- 包含技术细节（数据库访问、HTTP 调用、缓存操作、消息队列）
- 直接持久化数据
- import 任何 `infr.*`、`ohs.*`、`application.*` 模块

## Repository 接口签名规范

- 入参：只能是领域模型或关键字段（如 ID）
- 返回值：必须是领域模型或可空领域模型
- 使用集合语义命名（save/remove），而非数据库语义（insert/delete）
- 禁止入参或返回值使用 PO、DTO

## ACL 防腐层接口规范

- 定义与外部系统协作的抽象接口
- 使用领域语言命名方法，不暴露外部系统实现细节
- 入参和返回值只使用领域模型或值对象

## 命名

- 聚合根/实体：`{业务概念名}`（无后缀）
- 值对象：`{业务概念名}`
- 领域服务：`{业务名}Service`
- 仓储接口：`{聚合根名}Repository`
- 防腐层接口：`{外部系统概念名}Acl`

## 依赖方向

- 可依赖：无（领域层是核心，不依赖任何层）
- 禁止依赖：所有其他层
