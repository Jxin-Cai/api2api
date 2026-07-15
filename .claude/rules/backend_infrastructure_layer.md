---
paths:
  - "**/backend/infr/**"
---
# Infrastructure 层约束

> 分层通信与依赖方向详见架构层规则。

## 核心原则

实现领域层定义的抽象接口，封装所有技术细节。

## 允许

- 实现 domain/*/repository/ 和 domain/*/acl/ 中的抽象接口
- 领域对象与 ORM Model 的双向转换
- 数据库访问、外部系统集成、中间件配置
- 使用框架和 I/O 库

## 禁止

- 包含任何业务逻辑。Why：业务规则只在领域层表达，infr 层的变更只应因技术原因（换数据库、换 SDK）触发，不应因业务规则变更触发。
- 修改聚合根的业务状态（业务状态变更属于领域层）
- 在仓储方法中进行业务规则校验
- 依赖 OHS 层或 Application 层

## Repository 实现规范

- 方法入参：只能是领域模型或关键字段
- 方法返回值：必须是领域模型（禁止返回 ORM Model）。Why：调用方依赖领域模型接口，暴露 ORM Model 会泄漏持久化细节。
- 负责 Domain Model ↔ ORM Model 双向转换
- save 方法内部判断是新增还是更新

## 常见误区

- ❌ `def save(session): if session.status == "active": raise Error` → 业务校验属于领域层
- ✅ infr 只做持久化：`def save(session): self.db.merge(to_model(session))`
