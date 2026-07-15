---
paths:
  - "**/frontend/src/entities/**"
---
# Entities 层约束

> 分层通信与依赖方向详见前端架构规则。

## 核心原则

核心业务数据实体，提供跨 feature 的共享状态。

## 允许

- 定义核心业务数据实体
- 提供模块级单例共享状态，供多个 feature 读写
- 封装实体相关的 API 调用和通用 UI 组件

## 禁止

- 直接 import 其他 entity（同层禁止互引）。Why：entities 间耦合会破坏切片独立性，形成循环依赖。
- 向上依赖 features 或 pages
- 绕过 index 文件引用其他切片内部子目录

## 依赖方向

- 可依赖：shared
- 禁止依赖：pages、features、其他 entities
