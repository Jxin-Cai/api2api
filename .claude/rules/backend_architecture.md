---
alwaysApply: false
paths:
  - "**/backend/**"
---
# 后端架构 — DDD 四层

当你在 `backend/` 目录下编写或修改代码时，必须遵守以下整体架构规则：

## 目录结构

```
backend/
├── domain/           # 纯业务逻辑，零框架依赖
│   └── {aggregate}/  # 每个聚合一个目录
│       ├── model/    # 聚合根、实体、值对象
│       ├── repository/  # Repository 抽象接口
│       ├── acl/      # 防腐层抽象接口（可选，跨上下文协作时）
│       └── service/  # 领域服务（可选，跨实体逻辑时）
├── application/      # 用例编排，按应用场景划分
│   └── {use_case}/   # 每个应用场景一个目录
│       ├── *_application_service
│       └── command/  # 输入 DTO（不可变数据对象）
├── infr/             # 基础设施实现
│   ├── config/       # 数据库连接、第三方配置
│   ├── repository/   # Repository 实现 + ORM model + 数据库迁移
│   ├── client/       # 外部网关实现（实现 domain/acl/ 中的抽象接口）
│   └── ...           # 其他适配器
└── ohs/              # Open Host Service — 对外暴露层
    ├── assembler/    # Domain Model ↔ DTO 转换器
    ├── http/         # REST routers + dto/
    └── ws/           # WebSocket router
```

## 分层通信规则

1. **依赖方向**: ohs → application / domain ← infr。上层可引用下层，反向禁止。infr 实现 domain 定义的接口但不被 domain 引用。

2. **OHS 可调用 Application 或 Domain**: OHS 层既可调用 ApplicationService 编排用例，也可直接调用 Domain Service 处理简单逻辑。

3. **Domain 零外部依赖**: 仅含纯语言类型（数据类、抽象接口）和领域服务。禁止 import 任何框架或 I/O 库。

4. **ACL 防腐层**: 外部系统协作通过 domain/*/acl/ 中的抽象接口定义，由 infr/client/ 提供具体实现。

5. **Application 按场景划分**: Application 层按应用场景（而非聚合）组织，一个 ApplicationService 可编排多个聚合。

6. **Application 通过 Command 接收输入**: OHS 将 HTTP/WS 请求转为不可变 Command 对象传入 ApplicationService。Application 层不接触 Request/Response。

7. **Application 返回 Domain Model**: ApplicationService 返回领域模型或基本类型，由 OHS 的 Assembler 转为 DTO 输出。

8. **OHS 负责协议适配**: dto/ 定义请求/响应结构；assembler/ 做 Domain ↔ DTO 互转；Router 仅参数校验 + 调用服务层。

9. **Infr 实现 Domain 接口**: domain/*/repository/ 定义 Repository 抽象接口 → infr/repository/ 实现。domain/*/acl/ 定义网关抽象接口 → infr/client/ 实现。

10. **跨聚合编排在 Application 层**: 聚合之间不直接引用，协调逻辑统一由 ApplicationService 编排。
