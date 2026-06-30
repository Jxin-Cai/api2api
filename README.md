# API2API MVP

API2API 是一个 API 协议转换与供应商渠道路由服务，包含 Java Spring Boot 后端和 React/Vite 管理前端。

## 项目结构

```text
api2api/
  backend/          # Spring Boot / Maven 后端项目
  frontend/         # React / Vite / TypeScript 前端项目
  docker-compose.yml
  README.md
```

## 本地开发

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认读取 PostgreSQL：

- `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/api2api`
- `SPRING_DATASOURCE_USERNAME=api2api`
- `SPRING_DATASOURCE_PASSWORD=api2api`

后端启动时还会读取管理员账号初始化配置：

- `ADMIN_USERNAME=admin`
- `ADMIN_PASSWORD=change-me`

`ADMIN_PASSWORD` 不能为空。可参考 `backend/.env.example`。

### 前端

```bash
cd frontend
npm install
npm run dev
```

`frontend/.env.example` 中的 `VITE_API_BASE_URL` 可用于配置后端地址；Docker/Nginx 同源代理部署时可留空，本地前后端分开启动时可设置为 `http://localhost:8080`。

## Docker Compose

```bash
docker compose up --build
```

根目录 `.env.example` 仅用于 Docker Compose 的项目运行配置（数据库名、数据库账号密码、管理员账号密码、前端入口端口等），可复制为 `.env` 调整部署参数。部署前请务必修改 `ADMIN_PASSWORD`，后端启动时会将 `.env` 中的 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 同步为管理员账号凭据，数据库只保存密码 hash。

Docker 镜像版本由 `docker-compose.yml`、`backend/Dockerfile` 与 `frontend/Dockerfile` 维护；如果需要切换镜像源、镜像代理或基础镜像版本，请修改这些 Docker 相关文件，不要放入 `.env`。

该命令会启动 PostgreSQL、后端 API 服务和前端 Nginx 服务。默认只有前端 Nginx 对宿主机暴露端口：

- 前端管理台：`http://localhost:8989`
- 管理接口：`http://localhost:8989/api/*`
- 网关协议接口：`http://localhost:8989/v1/*`

后端 `8080` 与 PostgreSQL `5432` 不再直接暴露到宿主机，仅在 Docker 网络内访问。三个服务默认加入 Docker 网络 `shared-backend-network`。后端容器会读取根目录 `.env`，因此渠道密钥引用（例如 `OPENAI_API_KEY`）也可以通过 `.env` 注入；管理台中配置的供应商密钥 `keyRef` 必须与环境变量名一致。

前端容器会将以下路径代理到后端：

- 管理接口：`/api/*`
- 网关协议接口：`/v1/*`

## 生产部署（GitHub Actions）

仓库提供 GitHub Actions workflow：`.github/workflows/deploy.yml`。它会在代码 push 到 `master`（包括 PR 合并后产生的 push）时自动部署，也支持在 Actions 页面通过 `workflow_dispatch` 手动触发。

部署流程：

1. GitHub Actions 使用专用 SSH 私钥登录生产服务器。
2. 按 GitHub Repository Variables / Secrets 渲染生产 `.env`。
3. 将 `.env` 写入服务器部署目录。
4. 在服务器拉取 `master` 最新代码。
5. 执行 `docker compose --env-file .env -p api2api up -d --build --remove-orphans`。

### GitHub Actions Variables

在 `Settings → Secrets and variables → Actions → Variables` 配置普通参数：

| Name | 示例值 | 说明 |
| --- | --- | --- |
| `DEPLOY_HOST` | `187.124.157.143` | 生产服务器 IP |
| `DEPLOY_USER` | `root` | SSH 用户 |
| `DEPLOY_PORT` | `22` | SSH 端口 |
| `DEPLOY_PATH` | `/opt/api2api` | 服务器部署目录 |
| `DEPLOY_BRANCH` | `master` | 部署分支 |
| `POSTGRES_DB` | `api2api` | PostgreSQL 数据库名 |
| `POSTGRES_USER` | `api2api` | PostgreSQL 用户名 |
| `ADMIN_USERNAME` | `admin` | 管理员用户名 |
| `FRONTEND_PORT` | `8989` | 对外暴露的前端端口 |
| `COMPOSE_PROJECT_NAME` | `api2api` | Compose 项目名，固定 volume/network 名称 |
| `SSH_KNOWN_HOSTS` | `187.124.157.143 ssh-ed25519 ...` | 已确认的服务器 SSH host key |

`SSH_KNOWN_HOSTS` 应在可信环境中获取并确认后再保存，例如：

```bash
ssh-keyscan -p 22 187.124.157.143
```

### GitHub Actions Secrets

在 `Settings → Secrets and variables → Actions → Secrets` 配置敏感参数：

| Name | 说明 |
| --- | --- |
| `DEPLOY_SSH_PRIVATE_KEY` | GitHub Actions 登录服务器用的专用私钥 |
| `POSTGRES_PASSWORD` | PostgreSQL 生产密码 |
| `ADMIN_PASSWORD` | 管理员生产密码，不能为 `change-me` |
| `PROVIDER_SECRET_ENV` | 可选，多行 dotenv 格式的供应商密钥变量 |

`PROVIDER_SECRET_ENV` 示例：

```dotenv
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=...
```

管理台中渠道配置的供应商密钥 `keyRef` 要与变量名完全一致，例如 `OPENAI_API_KEY`。不要把真实 `.env`、私钥或供应商密钥提交到仓库。

### 服务器首次准备

在 `root@187.124.157.143` 上准备：

1. 安装 `git`、Docker Engine 和 Docker Compose plugin。
2. 确认 GitHub Actions 部署公钥已加入 `/root/.ssh/authorized_keys`。
3. 确认 `/opt/api2api` 不存在、为空目录，或已 clone 当前仓库并能 `git fetch origin master`。
4. 如果仓库是私有仓库，确保服务器可以拉取仓库，例如为仓库配置 GitHub Deploy Key。
5. 放行 `FRONTEND_PORT` 对应端口，默认 `8989`。

### 验证部署

手动触发 workflow 后，在服务器检查：

```bash
cd /opt/api2api
docker compose --env-file .env -p api2api ps
docker compose --env-file .env -p api2api logs --tail=100 backend
```

浏览器访问：

```text
http://187.124.157.143:8989/
```

使用 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 登录管理台。

## 登录

管理台登录需要用户名和密码。Docker Compose 部署时使用根目录 `.env` 中的 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 初始化或更新管理员账号；本地后端开发时使用后端环境变量中的同名配置。

## 主要能力

- 三种兼容网关协议入口：Claude Messages、OpenAI Responses、OpenAI Chat Completions。
- 供应商渠道、渠道模型、模型优先级和协议转换定义管理。
- API Key、模型白名单、累计 token 上限与使用记录。
- 前台/后台仪表盘、使用记录、用户管理与协议转换查看页面。
