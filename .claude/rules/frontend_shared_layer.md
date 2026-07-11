---
alwaysApply: false
paths:
  - "**/frontend/src/shared/**"
---
# Shared 层（共享基础设施层）约束

当你在 `frontend/src/shared/` 目录下编写或修改代码时，必须遵守以下规则：

## 路径结构

- `shared/api/` — HTTP/WS 客户端封装
- `shared/lib/` — 通用工具函数
- `shared/ui/` — 通用 UI 组件

## 允许

- 定义 HTTP/WebSocket 客户端封装
- 定义通用工具函数
- 定义通用 UI 组件（与业务无关的基础组件）

## 禁止

- import 任何上层模块（pages、features、entities）
- 包含业务逻辑
- 引用特定业务实体

## 核心原则

- shared 是公共底座，任何层都可引用 shared/
- shared/ 不能引用上层
- 只放与业务无关的基础设施代码

## 依赖方向

- 可依赖：无（shared 是最底层）
- 禁止依赖：pages、features、entities
