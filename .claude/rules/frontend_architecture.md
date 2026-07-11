---
alwaysApply: false
paths:
  - "**/frontend/**"
---
# 前端架构 — Feature-Sliced Design (FSD)

当你在 `frontend/` 目录下编写或修改代码时，必须遵守以下整体架构规则：

## 目录结构

```
frontend/src/
├── app/              # 应用壳层（入口文件、路由、全局样式）
├── pages/            # 路由级页面（model/ + ui/）
├── features/         # 自包含业务功能切片
│   └── {feature}/    # 每个功能一个目录
│       ├── api/      # 封装该功能的后端 API 调用
│       ├── model/    # 状态管理 + 业务逻辑
│       └── ui/       # 组件
├── entities/         # 核心业务数据实体
│   └── {entity}/     # api/ + model/ + ui/
└── shared/           # 跨层共享基础设施
    ├── api/          # HTTP/WS 客户端封装
    ├── lib/          # 通用工具函数
    └── ui/           # 通用 UI 组件
```

## FSD 分层通信规则

1. **依赖方向单向向下**: pages → features → entities → shared。同层之间禁止直接 import。

2. **公开 API 通过 index 文件**: 每个切片仅通过 index 文件暴露对外接口，内部子目录对外不可见。

3. **features 之间禁止直接引用**: 跨 feature 通信通过 entities 层的共享状态，或在 pages 层组合。

4. **entities 提供共享业务状态**: 模块级单例状态管理，供多个 feature 读写共享数据。

5. **api/ 封装 HTTP 调用**: 每个切片的 api/ 封装对应后端 endpoint，使用 shared/api/ 的客户端统一处理响应。

6. **model/ 存放业务逻辑**: 状态、计算逻辑。UI 组件只负责渲染和事件绑定。

7. **ui/ 存放组件**: 纯展示，通过属性传递或调用 model 层获取数据。

8. **shared 是公共底座**: 任何层都可引用 shared/，但 shared/ 不能引用上层。
