# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

API2API 是一个 API 协议转换与供应商渠道路由服务 。仓库为 monorepo，但 `backend/`（Java Spring Boot）与 `frontend/`（React/Vite）作为两个独立项目维护，各自有独立的依赖、构建与 Dockerfile。

核心能力：对外暴露三种兼容网关协议入口（Claude Messages、OpenAI Responses、OpenAI Chat Completions），按渠道模型优先级路由到上游供应商，并在协议不一致时做转换；同时提供管理台用于渠道、模型、协议转换定义、API Key、用户与使用记录管理。

## 常用命令

后端（在 `backend/` 下执行）：
```bash
mvn spring-boot:run          # 本地启动（默认连本机 5432 PostgreSQL）
mvn package                  # 打包（含编译 + 测试）
mvn test                     # 运行测试
mvn test -Dtest=ClassName#method   # 运行单个测试
```

前端（在 `frontend/` 下执行）：
```bash
npm install
npm run dev                  # Vite 开发服务器
npm run build                # tsc -b + vite build
npm run typecheck            # 仅类型检查
```
注意：前端目前**没有配置 lint 或测试脚本**；类型检查用 `npm run typecheck`。

整体（仓库根目录）：
```bash
docker compose up --build    # 启动 postgres + backend(8080) + frontend(3000)
```

数据库依赖 PostgreSQL，连接信息通过 `SPRING_DATASOURCE_*` 环境变量配置（见 `backend/.env.example`）。Flyway 在启动时自动执行 `backend/src/main/resources/db/migration` 下的迁移，`V3` 会写入种子数据（含 `admin` / `user` 两个账号）。

## 后端架构（DDD 分层）

包根 `com.api2api`，严格分四层，依赖方向自外向内（`ohs` → `application` → `domain`，`infr` 实现 `domain` 定义的接口）：

- **`domain/`** — 纯领域模型与领域服务，无框架依赖。按限界上下文划分：`channel`、`credential`、`gateway`、`protocol`、`routing`、`usage`、`user`、`analytics`。每个上下文含 `model`、`repository`（接口）、`service`、`event`。领域服务接口在 `service` 下，默认实现以 `Default*` 命名。
- **`application/`** — 应用服务（`*ApplicationService`），负责编排领域对象与事务（`@Transactional`），入参为 `command/` 下的 Command 对象。`BusinessException` 在此层抛出，携带错误码。应用层还会定义对外部系统的出站端口接口（如 `gateway/ProviderGatewayCallPort`）。
- **`infr/`** — 基础设施实现。`repository/` 用 **Spring JDBC**（非 JPA）实现领域仓储，每个上下文遵循 `RepositoryImpl` + `mapper`（JDBC RowMapper）+ `po`（持久化对象）+ `converter`（PO↔领域模型）的固定结构；`client/provider/` 实现上游供应商 HTTP 调用与密钥解析；`protocol/` 实现协议转换适配器。
- **`ohs/http/`** — Open Host Service，即 HTTP 适配层。Controller 按上下文分目录，每个目录含 `dto/`（请求/响应）+ `converter/`（DTO↔Command/领域模型，使用 MapStruct）。

关键约定与流程：
- **两类对外接口**：管理类接口统一前缀 `/api/*`，全部用 `ApiResponse<T>`（`{code, message, data}`，成功码 `SUCCESS`）包装；网关协议接口 `/v1/*`（`/v1/messages`、`/v1/responses`、`/v1/chat/completions`）返回**原始协议响应**，不做包装。
- **网关调用主流程**集中在 `GatewayInvocationApplicationService.invoke()`：鉴权 → 构建路由计划（`RoutingPolicyService`）→ 遍历候选渠道 → 请求/响应协议转换（`ProtocolConversionService`）→ 调用上游（`ProviderGatewayCallPort`）→ 记录用量。失败时按 `FailoverDecision` 决定是否切换下一候选渠道。
- **协议转换定义**存于数据库，分 `PASSTHROUGH`（同协议直通，已实现）与 `TRANSFORM`（跨协议，多数标记为 `NOT_IMPLEMENTED`）。新增跨协议转换需同时更新 DB 定义状态与 `infr/protocol` 下的转换实现。
- **流式（streaming）当前被网关拒绝**（`GatewayProtocolController.rejectStreaming`），相关配置已预留但功能未启用。
- **鉴权机制**：管理台用基于服务端 `HttpSession` 的会话（`CurrentUserContextResolver`，无 Spring Security），MVP 阶段**仅凭用户名登录、无密码**；网关接口用 `Authorization` 头里的 API Key 哈希匹配（`ApiCredentialRepository.findByKeyHash`）。
- **异常映射**统一在 `GlobalExceptionAdvice`：`AuthenticationRequiredException` → 401，`BusinessException` → 400（带错误码），校验失败 → 400。
- MapStruct 与 Lombok 通过 `maven-compiler-plugin` 的 annotation processor 协作，注意两者顺序与 `lombok-mapstruct-binding`。

## 前端架构（Feature-Sliced Design）

React 19 + Vite 6 + TypeScript + Ant Design + TanStack Query + Zustand + React Router 7。采用 FSD 分层，路径别名见 `vite.config.ts`（`@app`/`@pages`/`@widgets`/`@features`/`@entities`/`@shared`）。

层级（依赖只能从上往下）：
- **`app/`** — 应用装配：`providers`（QueryClient、主题）、`router`（路由表与按角色拆分的 `adminRoutes`/懒加载 `lazyPages`）、`layouts`（受保护/认证路由布局）、全局样式。
- **`pages/`** — 页面级组件，组合 widgets 与 features。
- **`widgets/`** — 组合性 UI 区块（如 `app-shell`、`usage-records-panel`）。
- **`features/`** — 用户用例切片（命名形如 `manage-*`/`view-*`/`login-by-username`），内部结构为 `ui/` + `model/`（hooks、types）+ 可选 `api/`。
- **`entities/`** — 业务实体切片（`api-credential`、`provider-channel`、`user-account` 等），结构为 `ui/` + `model/`（含数据查询 hook）+ `api/`（实体级请求）。
- **`shared/`** — 通用基建：`api/`（`apiClient` 封装 fetch、`contracts/` 后端契约类型、会话过期事件）、`ui/`、`lib/`、`config/`（含 Zustand store）、`types/`。

前端约定：
- 所有请求经 `shared/api/client.ts` 的 `apiClient`，自动 `credentials: 'include'`（带会话 cookie）、解包 `ApiResponse`、对 401/`AUTHENTICATION_REQUIRED` 触发 `notifyAuthExpired` 事件供路由守卫跳转登录。开发模式下 baseURL 为空（走 Vite 代理），生产用 `VITE_API_BASE_URL`。
- 后端契约类型集中在 `shared/api/contracts/`，修改后端 DTO 时需同步。

## 部署代理

前端 Nginx（`frontend/nginx.conf`）与 Docker 部署时将 `/api/*`（管理接口）和 `/v1/*`（网关协议接口）反向代理到后端，实现同源；本地分开启动时通过 `VITE_API_BASE_URL` 指向 `http://localhost:8080`。
