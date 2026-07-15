---
paths:
  - "**/frontend/src/features/**"
---
# Features 层约束

> 分层通信与依赖方向详见前端架构规则。

## 核心原则

自包含的业务功能切片，每个 feature 可独立开发和测试。

## 允许

- 封装该功能对应的后端 API 调用
- 定义功能级状态管理和组件
- 依赖 entities 和 shared

## 禁止

- 直接 import 其他 feature。Why：feature 间耦合会导致删除或重构一个 feature 时波及其他 feature。
- 向上依赖 pages
- 绕过 index 文件引用其他切片内部子目录

## 跨 feature 通信

- 通过 entities 层的共享状态
- 或在 pages 层组合

## 常见误区

- ❌ `import { useSessionList } from '@/features/session-list/model/useSessionList'` → 绕过了 index，且跨 feature 直接引用
- ✅ 共享状态提升到 entities 层，两个 feature 都从 entities 读取
