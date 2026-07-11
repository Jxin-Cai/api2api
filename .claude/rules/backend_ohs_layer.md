---
alwaysApply: false
paths:
  - "**/backend/ohs/**"
---
# OHS 层（Open Host Service 开放主机层）约束

当你在 `backend/ohs/` 目录下编写或修改代码时，必须遵守以下规则：

## 路径结构

- `ohs/assembler/` — Domain Model ↔ DTO 转换器
- `ohs/http/` — REST routers + dto/
- `ohs/ws/` — WebSocket router

## 允许

- 定义 Router、DTO（Request/Response）、Assembler
- 接收请求、参数校验
- DTO 与 Command 对象的转换
- 调用 ApplicationService 编排用例
- 直接调用 Domain Service 处理简单逻辑
- 异常捕获与协议级转换
- 使用 Assembler 将 Domain Model 转为 Response DTO

## 禁止

- 包含任何业务逻辑或业务规则判断
- 直接操作数据库
- 直接调用 Repository

## RESTful API 规范

- URL 使用小写 kebab-case，资源名词复数：`/api/quote-items/{id}`
- 标准 HTTP 方法：GET（查询）/ POST（创建）/ PUT（全量更新）/ PATCH（部分更新）/ DELETE（删除）
- Router 仅负责参数校验 + 调用服务层

## Assembler 规范

- 负责 Domain Model ↔ DTO 互转
- 每个聚合根对应一个 Assembler
- 转换逻辑不含业务规则

## DTO 规范

- Request DTO：定义请求体结构
- Response DTO：定义响应体结构
- DTO 不暴露领域模型内部结构

## 命名

- Router：`{业务名}_router`
- Request DTO：`{操作名}Request`
- Response DTO：`{操作名}Response`
- Assembler：`{聚合根名}Assembler`

## 依赖方向

- 可依赖：Application 层、Domain 层（Domain Service）
- 禁止依赖：Infrastructure 层（除工具类外）
