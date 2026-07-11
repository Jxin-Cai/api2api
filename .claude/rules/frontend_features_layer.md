---
alwaysApply: false
paths:
  - "**/frontend/src/features/**"
---
# Features 层（功能切片层）约束

当你在 `frontend/src/features/` 目录下编写或修改代码时，必须遵守以下规则：

## 路径结构

- `features/{feature}/api/` — 封装该功能的后端 API 调用
- `features/{feature}/model/` — 状态管理 + 业务逻辑
- `features/{feature}/ui/` — 组件
- `features/{feature}/index` — 公开 API（对外唯一入口）

## 允许

- 自包含的业务功能实现
- 封装该功能对应的后端 API 调用
- 定义功能级状态管理
- 定义功能级组件

## 禁止

- 直接 import 其他 feature 的内容（features 之间禁止直接引用）
- 直接 import pages 的内容（禁止向上依赖）
- 绕过 index 文件直接引用其他切片内部子目录

## 跨 feature 通信方式

- 通过 entities 层的共享状态
- 或在 pages 层组合

## 公开 API 规范

- 每个 feature 仅通过 index 文件暴露对外接口
- 内部子目录（api/、model/、ui/）对外不可见

## api/ 规范

- 封装对应后端 endpoint
- 使用 shared/api/ 的客户端统一处理响应

## model/ 规范

- 存放状态、计算逻辑
- UI 组件只负责渲染和事件绑定，逻辑放 model/

## ui/ 规范

- 纯展示组件
- 通过属性传递或调用 model 层获取数据

## 依赖方向

- 可依赖：entities、shared
- 禁止依赖：pages（上层）、其他 features（同层）
