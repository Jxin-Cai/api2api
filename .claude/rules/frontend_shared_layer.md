---
paths:
  - "**/frontend/src/shared/**"
---
# Shared 层约束

> 分层通信与依赖方向详见前端架构规则。

## 核心原则

公共底座，只放与业务无关的基础设施代码。

## 允许

- HTTP/WebSocket 客户端封装
- 通用工具函数
- 通用 UI 组件（与业务无关的基础组件）

## 禁止

- import 任何上层模块（pages、features、entities）。Why：shared 是依赖图的叶子节点，向上引用会形成循环依赖。
- 包含业务逻辑或引用特定业务实体
