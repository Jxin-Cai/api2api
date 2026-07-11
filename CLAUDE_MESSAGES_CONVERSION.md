# Claude Messages 上游转换能力

本文描述 Claude Code 通过 `/v1/messages` 接入时，服务转换到 AWS Bedrock Converse 或 OpenAI Responses 的行为。原则是：可等价映射就转换；不能可靠映射就明确失败，避免静默丢参数导致能力降级。

## AWS Bedrock Converse

已映射：system 与 `mid_conv_system`、多轮消息及连续同角色消息、文本/图片/文档、tool use/result、thinking 与 redacted thinking、`max_tokens`、temperature、top_p、top_k、stop sequences、JSON Schema 输出、effort、cache point（含 5m/1h TTL）、metadata、service tier、fast mode、usage、精确 stop sequence、流式事件和上游错误。

无法等价映射：

- Claude server tools（web search、web fetch、code execution、tool search、MCP toolset）的“工具定义”。Converse 仅能接收普通 tool spec；请求会明确失败。历史中的 server tool use/result 可以作为通用 tool use/result 转换。
- `tool_choice.disable_parallel_tool_use`。Converse 没有等价开关，请求会明确失败。
- `output_config.task_budget`。Converse 没有等价字段，请求会明确失败。
- `context_management`。Claude Code 默认发送的 `clear_thinking_20251015 + keep: all` 会被兼容接收，但不会写入 Converse 请求；该配置本身不删除 thinking，因此语义不变。其他 context editing 策略会明确失败，AWS server-side compaction 目前仅支持 InvokeModel。
- URL 图片。Converse 图片块要求字节数据；客户端需发送 base64。
- citation 的完整结构化元数据。引用文本可以保留，但 Claude 与 Converse 的引用对象并非完全同构。
- 未识别的新 Claude 顶层字段或内容块。转换器采用 fail-closed，升级协议前不会静默忽略。

## OpenAI Responses

已映射：system/developer 消息、文本/URL 或 base64 图片、URL/base64/file 文档、普通函数工具与 strict schema、tool choice、并行工具开关、web search、code interpreter、远程 MCP、thinking/effort、加密 reasoning 状态、JSON Schema 输出、compaction、metadata、service tier/fast mode、usage、流式事件和上游错误。

Responses 的 reasoning encrypted state，以及 web search/code interpreter/MCP 等 provider-hosted output item，在 Claude 没有对应内容块。服务会把原始状态封装进带版本前缀的 Claude thinking signature；Claude Code 下一轮回传后再恢复为原始 Responses input item。这样可保持无状态网关下的模型上下文连续性，但 Claude Code UI 不会展示这些托管工具的完整原生事件结构。

无法等价映射：

- `stop_sequences` 与 `top_k`。Responses 没有对应参数，请求会明确失败。
- Claude `output_config.task_budget`。Responses 没有等价预算参数，请求会明确失败。
- Claude manual thinking 的精确 `budget_tokens`。只能近似映射为 OpenAI reasoning effort；adaptive/effort 也按档位映射。
- Claude cache breakpoint 与 5m/1h TTL。Responses 使用自动 prompt caching，无法保留显式断点和相同 TTL 语义。
- web search `blocked_domains`。Responses 只有可等价使用的 allowed-domain filter；denylist 会明确失败。
- MCP 默认启用再逐项禁用的 denylist。Responses 的 `allowed_tools` 是 allowlist，无法无损表达该组合，请求会明确失败。
- custom tool 的 `defer_loading`、`eager_input_streaming`、`allowed_callers`，以及尚无对应 Responses 工具类型的 Claude server tools。请求会明确失败。
- context editing 中除基础 compaction 外的 clear-tool-uses、clear-thinking、memory、compaction instructions/pause 选项。请求会明确失败。
- 原生 Claude signed thinking/redacted thinking 不能伪装成 OpenAI reasoning state；只有由本服务生成的版本化签名可安全回传。
- 未识别的新 Claude 顶层字段或内容块。转换器采用 fail-closed，避免静默降级。

## 模型与错误语义

路由选定渠道后，会先把 Claude 请求中的模型改写为实际上游模型，再进行协议转换；Bedrock 模型只写入 Converse URI，不写进请求 JSON。跨协议上游错误会转换为正确的 Claude error envelope，并尽量保留 HTTP 状态和上游错误消息。流式异常事件不会再被当成正常结束。
