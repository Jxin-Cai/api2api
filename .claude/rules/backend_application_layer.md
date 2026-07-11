---
alwaysApply: false
paths:
  - "**/backend/application/**"
---
# Application 层（应用层）约束

当你在 `backend/application/` 目录下编写或修改代码时，必须遵守以下规则：

## 路径结构

- `application/{use_case}/` — 每个应用场景一个目录
- `application/{use_case}/*_application_service` — 应用服务
- `application/{use_case}/command/` — 输入 DTO（不可变数据对象）

## 允许

- 编排多个 Domain Service / Repository 完成业务用例
- 协调多个聚合根之间的交互
- 管理事务边界
- 调用基础设施层接口（事件发布、缓存、消息等）

## 禁止

- 包含业务规则（业务规则属于领域层）
- 包含复杂计算逻辑
- 直接操作数据库（必须通过 Repository）
- 依赖 OHS 层
- 接触 Request/Response 对象

## 核心原则

- 薄薄一层，只做编排不做计算
- 一个公有方法对应一个业务用例
- 按应用场景（而非聚合）组织，一个 ApplicationService 可编排多个聚合

## Command 对象规范

- 使用不可变数据对象（创建后不可修改）
- 由 OHS 层将 HTTP/WS 请求转为 Command 传入
- Command 只包含用例所需的输入数据

## 返回值规范

- 返回领域模型或基本类型
- 由 OHS 层的 Assembler 负责转为 DTO 输出
- 禁止返回 Response DTO

## 命名

- 应用服务：`{业务名}ApplicationService`
- Command 对象：`{操作名}Command`

## 依赖方向

- 可依赖：Domain 层
- 禁止依赖：OHS 层
