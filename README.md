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

可参考 `backend/.env.example`。

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

如果本机网络无法直接访问 Docker Hub（例如拉取 `node:22-alpine`、`maven:3.9.9-eclipse-temurin-17` 时出现 `failed to fetch anonymous token` / `connection reset by peer` / `Bad Gateway`），可以复制根目录 `.env.example` 为 `.env`，或通过环境变量切换到可访问的镜像仓库或镜像代理：

```bash
NODE_IMAGE=<镜像代理>/library/node:22-alpine \
NGINX_IMAGE=<镜像代理>/library/nginx:1.27-alpine \
POSTGRES_IMAGE=<镜像代理>/library/postgres:16-alpine \
MAVEN_IMAGE=<镜像代理>/library/maven:3.9.9-eclipse-temurin-17 \
JRE_IMAGE=<镜像代理>/library/eclipse-temurin:17-jre \
docker compose up --build
```

该命令会启动 PostgreSQL、后端 API 服务和前端 Nginx 服务：

- 前端管理台：`http://localhost:3000`
- 后端 API：`http://localhost:8080`
- PostgreSQL：`localhost:5432`

前端容器会将以下路径代理到后端：

- 管理接口：`/api/*`
- 网关协议接口：`/v1/*`

## 主要能力

- 三种兼容网关协议入口：Claude Messages、OpenAI Responses、OpenAI Chat Completions。
- 供应商渠道、渠道模型、模型优先级和协议转换定义管理。
- API Key、模型白名单、累计 token 上限与使用记录。
- 前台/后台仪表盘、使用记录、用户管理与协议转换查看页面。
