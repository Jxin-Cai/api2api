---
alwaysApply: false
paths:
  - "**/frontend/src/pages/**"
---
# Pages 层（页面层）约束

当你在 `frontend/src/pages/` 目录下编写或修改代码时，必须遵守以下规则：

## 路径结构

- `pages/{page}/model/` — 页面级状态管理
- `pages/{page}/ui/` — 页面级组件

## 允许

- 组合多个 features 构建完整页面
- 定义路由级组件
- 页面级状态管理（仅此页面使用的状态）

## 禁止

- 直接 import 其他 pages 的内容（同层禁止互相引用）
- 直接 import entities 或 shared 的内部子目录（必须通过 index 文件）

## 依赖方向

- 可依赖：features、entities、shared
- 禁止依赖：其他 pages（同层）
