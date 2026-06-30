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

后端 `8080` 与 PostgreSQL `5432` 不再直接暴露到宿主机，仅在 Docker 网络内访问。三个服务默认加入 Docker 网络 `shared-backend-network`。

前端容器会将以下路径代理到后端：

- 管理接口：`/api/*`
- 网关协议接口：`/v1/*`

## 登录

管理台登录需要用户名和密码。Docker Compose 部署时使用根目录 `.env` 中的 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 初始化或更新管理员账号；本地后端开发时使用后端环境变量中的同名配置。

## 主要能力

- 三种兼容网关协议入口：Claude Messages、OpenAI Responses、OpenAI Chat Completions。
- 供应商渠道、渠道模型、模型优先级和协议转换定义管理。
- API Key、模型白名单、累计 token 上限与使用记录。
- 前台/后台仪表盘、使用记录、用户管理与协议转换查看页面。
