---
paths:
  - "**/frontend/src/**"
---
# 前端架构 — Feature-Sliced Design (FSD)

## 依赖方向

pages → features → entities → shared。单向向下，同层之间禁止直接 import。

## FSD 分层通信规则

1. **公开 API 通过 index 文件**: 每个切片仅通过 index 文件暴露对外接口，内部子目录对外不可见。Why：强制封装，重构切片内部结构时不会破坏外部引用。

2. **features 之间禁止直接引用**: 跨 feature 通信通过 entities 层的共享状态，或在 pages 层组合。Why：保持 feature 可独立开发、测试、删除。

3. **entities 提供共享业务状态**: 模块级单例状态管理，供多个 feature 读写共享数据。

4. **api/ 封装 HTTP 调用**: 每个切片的 api/ 封装对应后端 endpoint，使用 shared/api/ 的客户端统一处理响应。

5. **model/ 存放业务逻辑**: 状态、计算逻辑。UI 组件只负责渲染和事件绑定。

6. **shared 是公共底座**: 任何层都可引用 shared/，但 shared/ 不能引用上层。
