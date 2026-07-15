---
paths:
  - "**/backend/**"
---
# 后端架构 — DDD 四层通信规则

## 依赖方向

ohs → application / domain ← infr。上层可引用下层，反向禁止。infr 实现 domain 定义的接口但不被 domain 引用。

## 分层通信规则

1. **OHS 可调用 Application 或 Domain**: OHS 层既可调用 ApplicationService 编排用例，也可直接调用 Domain Service 处理简单逻辑。

2. **Domain 零外部依赖**: 仅含纯语言类型（数据类、抽象接口）和领域服务。禁止 import 任何框架或 I/O 库。Why：保证领域模型可独立测试、可移植，不被技术栈绑定。

3. **ACL 防腐层**: 外部系统协作通过 domain/*/acl/ 中的抽象接口定义，由 infr/client/ 提供具体实现。

4. **Application 按场景划分**: 按应用场景（而非聚合）组织，一个 ApplicationService 可编排多个聚合。

5. **Application 通过 Command 接收输入**: OHS 将 HTTP/WS 请求转为不可变 Command 对象传入 ApplicationService。Application 层不接触 Request/Response。Why：解耦协议细节，使同一用例可被 HTTP/WS/CLI 等多协议复用。

6. **Application 返回 Domain Model**: ApplicationService 返回领域模型或基本类型，由 OHS 的 Assembler 转为 DTO 输出。

7. **OHS 负责协议适配**: dto/ 定义请求/响应结构；assembler/ 做 Domain ↔ DTO 互转；Router 仅参数校验 + 调用服务层。

8. **Infr 实现 Domain 接口**: domain/*/repository/ 定义 Repository 抽象接口 → infr/repository/ 实现。domain/*/acl/ 定义网关抽象接口 → infr/client/ 实现。

9. **跨聚合编排在 Application 层**: 聚合之间不直接引用，协调逻辑统一由 ApplicationService 编排。Why：避免聚合间隐式耦合，变更一个聚合不会波及另一个。
