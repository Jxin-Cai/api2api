# 代码健康盘点与小步治理

盘点日期：2026-10-01。起点：`fbac765`，`master` 与 `origin/master` 一致，工作区干净。

## 方法与边界

- 扫描后端 504 个 Java 生产文件、87 个测试文件，前端 244 个源码文件。
- 死代码以入口可达性、全仓引用和框架注册共同判断；Spring Bean、HTTP 入口、MapStruct/Lombok 生成访问器不能仅凭文本无引用删除。
- 每项单独修改、验证、提交；保持接口、协议转换规则和数据库结构不变。
- 这是静态盘点和自动化回归，不代表覆盖真实供应商及生产流量的全部情况。

## 盘点结果与顺序

| 顺序 | 问题与证据 | 处理方式 | 状态 |
| --- | --- | --- | --- |
| 1 | 后端测试基线失效：6 个测试文件仍使用增加 clientIp / firstTokenMillis 之前的签名，`mvn test` 报 13 条编译错误 | 更新测试夹具及历史变更后的陈旧断言，恢复完整回归 | 完成 |
| 2 | 后端 `ChannelName` 无引用；包内静态工具 `ClaudeRequestSanitizer` 仅被自身测试调用，无生产入口 | 删除类及只覆盖不可达实现的测试 | 完成 |
| 3 | 前端评估模块 8 个文件从 `main.tsx`（含动态 import / 桶导出）不可达；另有 4 个组件只有声明和桶导出；`FrontDashboardPanel` 有未使用 import | 删除不可达前端代码及失效导出，保留后端评估接口；启用编译器未使用检查 | 完成 |
| 4 | `shared/api/http.ts` 与 `shared/lib/queryString.ts` 重复处理数组、空值和 URL 编码 | 复用一个序列化实现，用边界用例锁定行为 | 完成 |
| 5 | `GatewayApiKeyHashHelper` 与 `ApiKeyMaterialHelper` 各自实现 SHA-256 / 十六进制编码 | 合并底层哈希计算，保持密钥生成及请求头优先级不变 | 待处理 |
| 6 | `GenericProtocolMessageConverter` 4035 行，同时负责请求、响应、模型判定、用量结构转换 | 先提取独立的用量转换职责，通过现有协议回归验证；后续按协议方向继续拆分 | 待处理 |
| 7 | 评估时区 / 时间戳解析捕获 `RuntimeException`，错误响应构造捕获全部 `Exception` 且静默兜底 | 收窄到明确解析/序列化异常，补充异常路径测试 | 待处理 |

## 需要持续治理的较大问题

- `UnifiedStreamingConversionAdapter`（1661 行）、`GatewayInvocationApplicationService`（1037 行）存在多职责与重复流程。流式生命周期、重试、额度预留相互关联，需要分批建立状态转换测试后再拆，不能用机械替换完成。
- `ProviderChannelFormDrawer`（511 行）混合表单、模型预览、协议映射与保存流程；前端当前只有一个测试文件（8 个用例），UI 交互覆盖不足。
- `UsageRecord.rehydrate` 有 19 个参数，调用方易遗漏新增参数，本次测试基线故障就是实例。后续适合引入明确的参数对象，同时保留持久化转换的回归覆盖。
- 前端构建已存在超过 500 kB 的 chunk 警告，属于性能治理项；不能凭未引用页面判断图表依赖或运行时模块是死代码。
- 前端多处 `refetch().catch(() => undefined)` 需结合 TanStack Query 的错误状态与 UI 提示核实，不能直接删捕获而引入未处理 Promise。

## 验证记录

- 修改前前端：`npm test`（8 个用例）、`npm run build` 通过；额外未使用检查发现一个 `useMemo` import。
- 修改前后端：`mvn test` 在测试编译阶段失败，未运行测试。

- 第 1 步：修复旧测试签名；依据 `c7edb4e` 保留标准缓存读字段显式为 0 的优先级，并新增字段缺失时回退别名的测试；字段数量硬编码改为验证契约字段完整映射。`mvn test`：768 个用例全部通过。

- 第 2 步：删除 2 个无生产调用的类及旧清理器的 5 个测试；全仓核对无剩余引用。`mvn clean test`：763 个用例全部通过，clean 确保没有旧 class 掩盖误删。

- 第 3 步：删除 8 个不可达评估模块文件、其独占的契约类型文件、4 个只被桶导出的组件；清除失效导出与未使用 import。启用 `noUnusedLocals` / `noUnusedParameters`。`npm test`：8 个用例通过；`npm run build` 通过，既有大包警告保留。

- 第 4 步：HTTP 与用量筛选共用查询参数序列化和类型；保持已有参数追加、空值过滤、编码及零值行为。测试运行器自动发现 `tests/*.test.ts`，避免新增测试未执行。`npm test`：15 个用例通过；`npm run typecheck` 通过。
