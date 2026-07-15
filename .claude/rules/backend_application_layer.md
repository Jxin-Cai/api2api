---
paths:
  - "**/backend/application/**"
---
# Application 层约束

> 分层通信与依赖方向详见架构层规则。

## 核心原则

薄薄一层，只做编排不做计算。一个公有方法对应一个业务用例。

## 允许

- 编排多个 Domain Service / Repository 完成业务用例
- 协调多个聚合根之间的交互
- 管理事务边界

## 禁止

- 包含业务规则或复杂计算逻辑。Why：业务规则散落到 Application 层会导致霰弹式修改——一条规则变更需要同时改多个 Service。
- 直接操作数据库（必须通过 Repository）。Why：绕过 Repository 会使领域模型与持久化耦合，无法替换存储实现。
- 依赖 OHS 层、接触 Request/Response 对象
- 返回 Response DTO（由 OHS Assembler 负责转换）

## 常见误区

- ❌ 在 ApplicationService 中写 `if order.can_cancel():` 判断逻辑 → 这属于领域规则，应放在 Order 聚合根方法中
- ✅ ApplicationService 只调用 `order.cancel()`，由领域模型内部校验
- ❌ ApplicationService 直接返回 `SessionResponse(...)` → 应返回领域模型，OHS 层转 DTO
