---
paths:
  - "**/backend/ohs/**"
---
# OHS 层（Open Host Service）约束

> 分层通信与依赖方向详见架构层规则。

## 核心原则

协议适配层——接收外部请求，转为内部调用，再将结果转为协议响应。

## 允许

- 定义 Router、DTO（Request/Response）、Assembler
- 接收请求、参数校验
- 调用 ApplicationService 编排用例，或直接调用 Domain Service 处理简单逻辑
- 使用 Assembler 将 Domain Model 转为 Response DTO

## 禁止

- 包含任何业务逻辑或业务规则判断。Why：业务逻辑在 OHS 层会导致同一规则在 HTTP/WS 两个 router 中重复实现。
- 直接操作数据库或直接调用 Repository。Why：跳过 Application/Domain 层会使事务边界和业务校验失控。

## RESTful API 规范

- URL 使用小写 kebab-case，资源名词复数：`/api/quote-items/{id}`
- 标准 HTTP 方法：GET / POST / PUT / PATCH / DELETE
- Router 仅负责参数校验 + 调用服务层

## Assembler 规范

- 负责 Domain Model ↔ DTO 互转
- 转换逻辑不含业务规则

## 常见误区

- ❌ Router 中写 `if session.owner != current_user: raise 403` → 权限校验属于 Application/Domain 层
- ✅ Router 只做参数格式校验（类型、必填），业务校验委托给服务层
