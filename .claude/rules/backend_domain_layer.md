---
paths:
  - "**/backend/domain/**"
---
# Domain 层约束

> 分层通信与依赖方向详见架构层规则。

## 核心原则

领域层是架构核心，零外部依赖，纯业务逻辑。

## 允许

- 定义聚合根、实体、值对象，并在其中实现业务规则
- 定义领域服务处理跨实体的业务逻辑
- 定义 Repository / ACL 抽象接口（只定义，不实现）

## 禁止

- import 任何框架或 I/O 库。Why：保证领域模型可独立单元测试，且不因技术栈升级而被迫修改业务代码。
- import 任何 `infr.*`、`ohs.*`、`application.*` 模块。Why：依赖反转原则——领域层定义接口，基础设施层实现接口。
- 包含技术细节（数据库访问、HTTP 调用、缓存操作）

## Repository 接口规范

- 入参：只能是领域模型或关键字段（如 ID）
- 返回值：必须是领域模型或可空领域模型
- 使用集合语义命名（save/remove），而非数据库语义（insert/delete）。Why：Repository 模拟内存中的集合，调用方不应感知持久化机制。
- 禁止入参或返回值使用 PO、DTO

## 常见误区

- ❌ 在 domain 中 `from sqlalchemy import Column` → 框架依赖，属于 infr 层
- ❌ Repository 接口定义 `def insert(model: ORMModel)` → 应使用领域模型而非 ORM Model
- ✅ `def save(session: ClaudeSession) -> None` — 纯领域语义
