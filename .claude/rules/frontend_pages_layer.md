---
paths:
  - "**/frontend/src/pages/**"
---
# Pages 层约束

> 分层通信与依赖方向详见前端架构规则。

## 核心原则

路由级页面，组合多个 features 构建完整视图。

## 允许

- 组合多个 features 构建完整页面
- 页面级状态管理（仅此页面使用的状态）

## 禁止

- 直接 import 其他 page（同层禁止互引）
- 绕过 index 文件引用其他切片内部子目录

## 依赖方向

- 可依赖：features、entities、shared
- 禁止依赖：其他 pages
