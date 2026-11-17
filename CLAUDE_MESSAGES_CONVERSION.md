# Messages 与 Responses 协议转换

核对日期：2026-11-17。主要场景是客户端调用 `/v1/messages`，网关请求上游 `/v1/responses`，再将普通响应或 SSE 转回 Messages。

本项目通过 Jackson JSON 树和可执行协议契约实现转换，没有安装 Anthropic/OpenAI SDK。因此协议升级修改的是 `ProtocolContractDefinitions`、转换程序及流式状态机，而不是 Maven SDK 版本。原生同协议请求继续保留供应商字段。

## 本次升级

| 范围 | 原行为 | 当前行为 |
| --- | --- | --- |
| Messages 定义 | 缺少线程和引用字段；拒绝分类不全；fallbacks 只接受数组 | 注册 `thread`、引用事件，补充 `stop_details` 分类；接受 `fallbacks: "default"`，保留回退计费凭据的原生联合类型 |
| Responses 定义 | 缺少图像压缩、审核策略明细、缓存诊断和引用事件；TTL 说明只有 30m | 注册 `image.context_compression`、`moderation.policy.*.mode`、`prompt_cache_diagnostics`、`response.output_text.annotation.added`；记录 `30m/12h` |
| 结构化输出 | 没有明确设置 strict；直接转发可能不满足 Responses 的对象约束 | 全部属性已 required 的兼容对象默认开启 strict，递归补 `additionalProperties: false`；含可选字段或开放对象时默认显式关闭 strict，不改变参数含义 |
| 显式 strict 工具/输出 | 不兼容 schema 发送上游才报错 | 在网关检查必填属性与对象封闭性；无法满足的显式 strict 返回明确错误，不擅自把可选字段变为必填或 nullable |
| 流式文本/拒绝 | 收到任何 delta 后就忽略完成事件中的文本 | 对照已发送前缀补发缺失尾部；兼容 text/part/item/response 各级完成快照，避免重复；冲突快照明确报错 |
| 流式工具参数 | 发过增量就跳过最终参数；可能将 item_id 当作 call_id | 校验最终 JSON 后补齐未发送尾部；缺失 added 事件时等待真实调用身份，避免空名称和错误调用关联 |
| URL 引用 | annotations 被丢弃 | 映射到 Messages citations / citations_delta，按内容块去重；引用摘录按 Unicode 码点定位；历史回放恢复原始 annotation |
| 停止原因 | tool_use 覆盖 max_tokens；普通响应中 refusal 可能变 end_turn | 普通与流式响应共享优先级：输出上限 → 拒绝 → 工具调用 → 暂停/完成；普通响应补 `stop_sequence: null` |

这里的 strict 检查覆盖对象形状和可选字段差异，不替代上游完整 JSON Schema 子集验证。默认非严格输出保留原 schema，但不提供 strict 模式的强制匹配保证。

## 现有能力与边界

- 保留已有的 `store: false`、`reasoning.encrypted_content`、推理签名桥接、工具结果、多模态内容、工具命名空间和 assistant `phase` 处理。
- 默认继续使用隐式缓存与稳定 cache key。本次更新 `12h` 的原生协议定义，没有把 Claude 的 `1h` TTL 擅自换成 Responses 的 `12h`。
- Messages 的 `thread`、`fallbacks`、`fallback_credit_token` 依赖 Anthropic 服务端状态或计费语义，跨到 Responses 时明确拒绝；注册原生字段不代表支持无损跨协议转换。
- URL 引用的 `encrypted_index` 使用 `api2api_url_v1_` 前缀封装原始 Responses annotation。这是可解码的网关往返状态，不是加密内容，也不是 Anthropic 原生来源凭据；应通过本桥接回放。原生 Anthropic 索引不会被解密。文件引用没有等价的文档定位，暂不转换。
- 流式缺失尾部只能从上游实际提供的完整快照恢复。缺少终止事件、无法解析的工具参数或与已发送文本冲突的快照仍报错，不制造成功响应。
- 本次验证是本地协议与后端回归测试，没有使用真实模型密钥进行线上质量或延迟对比，不能据此宣称模型准确率提升。

## 官方依据

- [Anthropic Messages API](https://platform.claude.com/docs/en/api/messages/create)；[核对时的 SDK 请求定义](https://github.com/anthropics/anthropic-sdk-python/blob/3fa305d99c4a1b8017bce15b1f1378a5871ca057/src/anthropic/types/beta/message_create_params.py)。线程和 fallback 是 Beta 能力，实际启用取决于模型与请求头。
- [Anthropic URL 引用结构](https://github.com/anthropics/anthropic-sdk-python/blob/3fa305d99c4a1b8017bce15b1f1378a5871ca057/src/anthropic/types/citations_web_search_result_location.py)；[拒绝详情结构](https://github.com/anthropics/anthropic-sdk-python/blob/3fa305d99c4a1b8017bce15b1f1378a5871ca057/src/anthropic/types/refusal_stop_details.py)。
- [Responses API](https://developers.openai.com/api/reference/resources/responses/methods/create)；[核对时的 SDK 请求定义](https://github.com/openai/openai-python/blob/311924f055110122991cc23b988a1836793551a1/src/openai/types/responses/response_create_params.py)；[响应定义](https://github.com/openai/openai-python/blob/311924f055110122991cc23b988a1836793551a1/src/openai/types/responses/response.py)。
- [Responses 引用事件](https://github.com/openai/openai-python/blob/311924f055110122991cc23b988a1836793551a1/src/openai/types/responses/response_output_text_annotation_added_event.py)；[结构化输出约束](https://developers.openai.com/api/docs/guides/structured-outputs)。

## GitHub 对照

星数为核对当天 GitHub API 返回的快照；代码链接固定到实际阅读的提交。仅借鉴行为和设计，Java 实现独立编写，没有引入这些项目的代码或依赖。

| 项目 | 星数 | 参考实现与采用方式 |
| --- | ---: | --- |
| LiteLLM | 63,406 | [Messages → Responses adapter](https://github.com/BerriAI/litellm/blob/0cc072c999514faefd891efcd1a8b406e5350f04/litellm/llms/anthropic/messages/responses_adapters/transformation.py)：核对工具 strict、结构化输出和推理配置。保留现有普通工具 `strict: false`；没有采用把可选字段全部改为必填的策略。 |
| CLIProxyAPI | 58,805 | [请求转换](https://github.com/router-for-me/CLIProxyAPI/blob/7df56074b252384d878ec4fca840552c6fb5236e/internal/translator/codex/claude/codex_claude_request.go)、[响应转换](https://github.com/router-for-me/CLIProxyAPI/blob/7df56074b252384d878ec4fca840552c6fb5236e/internal/translator/codex/claude/codex_claude_response.go)：借鉴可选字段兼容策略、累积参数与完成快照协调；保留本项目已有 phase/推理状态桥接。 |
| new-api | 52,077 | [Responses → Messages 响应](https://github.com/QuantumNous/new-api/blob/669d774976369e986f03b6e24fff1042f6f94832/relaykit/relayconvert/internal/oai_responses/to_claude_messages_resp.go)、[流式响应](https://github.com/QuantumNous/new-api/blob/669d774976369e986f03b6e24fff1042f6f94832/relaykit/relayconvert/internal/oai_responses/to_claude_messages_stream_resp.go)：借鉴 URL 引用与 Unicode 摘录、终态优先于工具调用的处理。 |

## 验证

在 Java 17 下运行 `mvn test`。新增回归场景覆盖完成快照补尾、不重复输出、缺失工具 added 事件、停止原因优先级、引用的流式顺序/去重/多内容块/Unicode/历史回放、strict schema 以及新增原生协议字段。`ClaudeResponsesCurrentSchemaTest` 的 schema 预期和协议版本断言同步更新。
