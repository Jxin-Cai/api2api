# Claude Messages 上游转换能力

本文描述 Claude Code 通过 `/v1/messages` 接入时，服务转换到 AWS Bedrock Converse 或 OpenAI Responses 的行为。原则是：可等价映射就转换；不能可靠映射就明确失败，避免静默丢参数导致能力降级。

审计基线：2026-07-12，本机 Claude Code 2.1.156；对照 Anthropic、AWS Bedrock 与 OpenAI 最新官方协议文档。这里仅评估 Claude Messages → Bedrock Converse 和 Claude Messages → OpenAI Responses，不包含 AWS 上的 Claude Messages/InvokeModel 路径。

## AWS Bedrock Converse

已映射：system 与 `mid_conv_system`、多轮消息及连续同角色消息、文本/base64 图片/文档、client tool use/result、thinking 与 redacted thinking、`max_tokens`、temperature、top_p、top_k、stop sequences、JSON Schema 输出、adaptive thinking/effort、cache point（含 5m/1h TTL 字段）、metadata、service tier、fast mode、usage、客户端请求模型、精确 stop sequence、过滤/拒绝/上下文窗口停止原因、流式事件和上游错误。

Claude Code 的延迟工具会降级为“完整工具列表”：`tool_search_tool_*` 定义不发送给 Converse，所有 `defer_loading` custom tools 作为普通 Converse `toolSpec` 发送。模型仍可调用这些工具，但失去按需发现带来的上下文节省。`input_examples` 会追加到工具描述；`eager_input_streaming` 不需要专门开关，ConverseStream 本身仍可流式输出工具参数。

无法等价映射：

- Claude server tools（web search、web fetch、code execution、advisor、MCP toolset、memory、bash、text editor、computer）的“工具定义”。Converse 只能承载普通 client `toolSpec`；请求会明确失败。tool search 是上述“展开延迟工具”的特例。
- `allowed_callers` 中的 `code_execution_*`，以及响应里的 `caller`。这属于 programmatic tool calling，Anthropic 官方明确说明 Bedrock 不支持；请求会明确失败。仅 `direct` 可无损接受。
- `tool_choice.disable_parallel_tool_use`。Converse 没有等价开关，请求会明确失败。
- `output_config.task_budget`。Converse 没有等价字段，请求会明确失败。
- `context_management`。Claude Code 默认发送的 `clear_thinking_20251015 + keep: "all"` 或 `keep: {"type":"all"}` 会被兼容接收，但不会写入 Converse 请求；该配置本身不删除 thinking，因此语义不变。`clear_tool_uses`、实际清除 thinking、compaction 等其他策略会明确失败。AWS server-side compaction 目前仅支持 InvokeModel，不支持 Converse。
- URL 图片。Converse 图片块要求字节数据；客户端需发送 base64。
- citation 的完整结构化元数据。引用文本可以保留，但 Claude 与 Converse 的引用对象并非完全同构。
- prompt cache 的可移植保障。字段可转换，但具体模型是否支持 1h TTL、可放置的 cache point 数量及最小 token 数由 Bedrock 模型决定；例如不支持 1h 的模型会由 AWS 拒绝。
- `mid_conv_system` 仅部分 Bedrock Claude 模型支持，且位置受限；转换器保留结构，但最终能力由目标模型决定。
- `inference_geo`。Claude Platform 的地域路由参数在 Converse 中没有请求级等价物；Bedrock 地域由 endpoint/inference profile 决定，请求会明确失败。
- `max_tokens: 0` 的“只预热 prompt cache”语义。Converse 没有等价请求，请求会由转换/上游拒绝。
- 未识别的新 Claude 顶层字段或内容块。转换器采用 fail-closed，升级协议前不会静默忽略。

## OpenAI Responses

已映射：system/developer 消息、文本/URL 或 base64 图片、URL/base64/file 文档、普通函数工具与 strict schema、tool choice、并行工具开关、web search、code interpreter、远程 MCP、thinking/effort、加密 reasoning 状态、JSON Schema 输出、compaction、metadata、service tier/fast mode、usage、流式事件和上游错误。

Claude Code 新工具能力的映射如下：

- `tool_search_tool_regex_*` / `tool_search_tool_bm25_*` → Responses `tool_search`；custom/MCP `defer_loading` 原样保留。目标必须是 GPT-5.4+，否则明确失败。
- `input_examples` → 追加到 function description。功能信息保留，但不是独立结构化字段。
- `eager_input_streaming` → 不发送专门字段；Responses 原生流式 function arguments 已提供相同调用链能力。
- assistant 工具调用前的文字 → Responses message `phase: "commentary"`；普通完成文本 → `phase: "final_answer"`，避免 GPT-5.4+ 把计划/工具前导语误判为最终答案而提前停止。
- Claude `effort: max` → GPT-5.6+ 的 Responses `max`；旧模型降为 `xhigh`。由本服务封装后重放的 reasoning state，在 GPT-5.6+ 设置 `reasoning.context: "all_turns"`。

Responses 的 reasoning encrypted state，以及 web search/code interpreter/MCP 等 provider-hosted output item，在 Claude 没有对应内容块。服务会把原始状态封装进带版本前缀的 Claude thinking signature；Claude Code 下一轮回传后再恢复为原始 Responses input item。这样可保持无状态网关下的模型上下文连续性，但 Claude Code UI 不会展示这些托管工具的完整原生事件结构。

无法等价映射：

- `stop_sequences` 与 `top_k`。Responses 没有对应参数，请求会明确失败。
- Claude `output_config.task_budget`。Responses 没有等价预算参数，请求会明确失败。
- Claude manual thinking 的精确 `budget_tokens`。只能近似映射为 OpenAI reasoning effort；adaptive/effort 也按档位映射。
- Claude cache breakpoint 与 5m/1h TTL。Responses 默认使用自动 prompt caching；GPT-5.6+ 虽支持显式 breakpoint，但目前 TTL 只有 30m，无法保留 Claude 的相同过期语义，因此转换器不伪装为等价映射。
- web search `blocked_domains`。Responses 只有可等价使用的 allowed-domain filter；denylist 会明确失败。
- MCP 默认启用再逐项禁用的 denylist。Responses 的 `allowed_tools` 是 allowlist，无法无损表达该组合，请求会明确失败。
- `allowed_callers` 中的 programmatic/code-execution caller，以及 Claude 响应 `caller` 字段。Responses 有自身的 programmatic tool calling，但调用器协议和 Claude 内容块不相同，当前不做有风险的伪映射；请求明确失败。仅 `direct` 可接受。
- 尚无对应 Responses 工具类型的 Claude server tools：web fetch、advisor、memory、bash、text editor、computer，以及 Claude 原生 server-tool 历史块。请求会明确失败。web search、code execution 和 MCP 只转换其可等价子集，版本、超时、域名 denylist 等专属选项不保留。
- context editing 中除基础 compaction 外的 clear-tool-uses、clear-thinking、memory、compaction instructions/pause 选项。请求会明确失败。
- 原生 Claude signed thinking/redacted thinking 不能伪装成 OpenAI reasoning state；只有由本服务生成的版本化签名可安全回传。
- Claude Messages 本身没有 `phase` 字段；转换器按“同一 assistant 消息是否包含 tool_use”推断。Claude Code 常规计划/工具链可正确保留，但任意第三方构造的复杂交错内容不能做到百分之百还原意图。
- citation/annotation 的完整结构化元数据，以及 OpenAI 独有的 shell、apply_patch、skills、image generation、computer、file search 等服务端工具，不能从 Claude Messages 请求无损表达。
- `inference_geo` 和 `max_tokens: 0` 的 cache-only 请求。Responses 没有对应的 Claude Platform 地域路由或完全相同的只写缓存语义。
- 未识别的新 Claude 顶层字段或内容块。转换器采用 fail-closed，避免静默降级。

## 对 Claude Code 的实际结论

- 常规编码循环（读写文件、命令执行、Todo/计划、普通 MCP、自定义工具、thinking、结构化输出）两条路径都可工作。
- Responses GPT-5.4+ 对延迟工具和工具前导语的保真度更高；GPT-5.6+ 还能保留 `max` effort 和跨轮 persisted reasoning。
- Bedrock Converse 的主要硬上限是没有 Anthropic server tools、tool search、programmatic tool calling 和 server-side context editing。当前实现优先保住“工具可调用”，代价是把 deferred tools 全量展开。
- 任何无法可靠表达的字段均显式返回 conversion error，不再静默丢弃。Claude Code 的 `clear_thinking + keep all` 是唯一按无操作语义兼容忽略的 context edit。

## 模型与错误语义

路由选定渠道后，会先把 Claude 请求中的模型改写为实际上游模型，再进行协议转换；Bedrock 模型只写入 Converse URI，不写进请求 JSON。返回客户端时，非流式响应和流式 `message_start` 都恢复为客户端请求的模型名。跨协议上游错误会转换为正确的 Claude error envelope，并尽量保留 HTTP 状态和上游错误消息。流式异常事件、`malformed_model_output` / `malformed_tool_use`，以及未收到 Bedrock `messageStop` 的提前 EOF 都会明确失败，不再伪造成 `end_turn`。
