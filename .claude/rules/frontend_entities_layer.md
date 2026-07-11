---
alwaysApply: false
paths:
  - "**/frontend/src/entities/**"
---
# Entities 层（实体层）约束

当你在 `frontend/src/entities/` 目录下编写或修改代码时，必须遵守以下规则：

## 路径结构

- `entities/{entity}/api/` — 该实体的后端 API 调用
- `entities/{entity}/model/` — 模块级单例状态管理
- `entities/{entity}/ui/` — 实体相关的通用 UI 组件
- `entities/{entity}/index` — 公开 API（对外唯一入口）

## 允许

- 定义核心业务数据实体
- 提供共享业务状态（模块级单例）
- 供多个 feature 读写共享数据
- 封装实体相关的 API 调用

## 禁止

- 直接 import 其他 entity 的内容（同层禁止互相引用）
- 直接 import features 或 pages 的内容（禁止向上依赖）
- 绕过 index 文件直接引用其他切片内部子目录

## 共享状态规范

- 使用模块级单例状态管理
- 供多个 feature 读写共享数据

## 公开 API 规范

- 每个 entity 仅通过 index 文件暴露对外接口
- 内部子目录对外不可见

## 依赖方向

- 可依赖：shared
- 禁止依赖：pages（上层）、features（上层）、其他 entities（同层）
