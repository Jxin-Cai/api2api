# Claude Messages 上游转换能力

本文描述 Claude Code 通过 `/v1/messages` 接入时，服务转换到 OpenAI Responses 的行为。原则是：可等价映射就转换；不能可靠映射就明确失败，避免静默丢参数导致能力降级。AWS Bedrock 上游使用原生 Claude Messages InvokeModel 协议，不再支持 Converse。

Bedrock InvokeModel 不托管执行 Anthropic `web_search_20250305`。该工具在 Bedrock 路径会转换成客户端执行的同名 `custom` 工具，输入 schema 要求 `query`；`max_uses`、`allowed_domains` / `blocked_domains` 与 `user_location` 会写入工具契约描述，避免把不受支持的 server tool type 原样发送给 Bedrock。上游返回普通 `tool_use`，调用方须执行搜索并以 `tool_result` 继续工具循环；若必须由模型提供商托管搜索，应选择原生支持 web search 的渠道，而不是 Bedrock InvokeModel。

审计基线：2026-07-16，Claude Code 2.1.210、Anthropic TypeScript SDK 0.111.0 与 OpenAI SDK 6.47.0；对照 Anthropic 与 OpenAI 最新官方协议文档。

## OpenAI Responses

已映射：system/developer 消息、文本/URL 或 base64 图片、URL/base64/file 文档、普通函数与 free-form custom tool、strict schema、tool choice、并行工具、tool search、web search、code interpreter、远程 MCP、programmatic tool calling、thinking/effort、加密 reasoning、显式 prompt cache breakpoint、JSON Schema 输出、compaction、metadata、service tier/fast mode、cache usage、完整流式结束和上游错误。

### Claude 与 Responses 同类能力字段对照

| 能力 | Claude Messages 字段/块 | OpenAI Responses 字段/item | 当前转换行为 |
| --- | --- | --- | --- |
| 系统指令 | `system` text/block | `input[].role=developer` + `input_text` | 保留块顺序；可缓存的 system text 可带显式 breakpoint |
| 普通对话 | `messages[].role/content` | `input[]` message | assistant 使用 `output_text`，user 使用 `input_text` |
| 工具前导语/计划文字 | assistant text 与 `tool_use` 同消息 | message `phase=commentary` | 无工具调用的完成文本使用 `phase=final_answer` |
| 客户端工具 | `tools[].name/input_schema/strict` | function `name/parameters/strict` | 名称和 JSON Schema 保留；`input_examples` 追加到 description |
| 延迟工具 | `tool_search_tool_*`、`defer_loading` | `tool_search`、`defer_loading` | GPT-5.4+ 原生映射；MCP deferred tool 同样保留 |
| 工具选择 | `tool_choice`、`disable_parallel_tool_use` | `tool_choice`、`parallel_tool_calls` | `auto/any/tool/none` 和并行开关映射 |
| 普通工具调用 | `tool_use{id,name,input}` | `function_call{call_id,name,arguments}` | 双向映射；`tool_result` → `function_call_output`，执行成功或失败均由 `output` 文本表达，不把已返回的失败结果误标成未完成 |
| free-form 工具 | Claude 无独立块，仍表现为 `tool_use` | `custom_tool_call{input}` | 用版本化 tool id 区分；非 JSON input 包装为 `{"input":"..."}`，结果恢复为 `custom_tool_call_output` |
| Programmatic tool calling | `allowed_callers=[direct,code_execution_*]`、`caller` | `allowed_callers=[direct,programmatic]`、`programmatic_tool_calling`、`caller.type=program` | GPT-5.6+ 映射；已支持 `code_execution_20250825`、`20260120`、`20260521`；`caller_id` 通过合成 code-execution tool id 可逆回传，响应采用当前 `code_execution_20260521` 标记 |
| 推理强度 | `thinking`、`output_config.effort` | `reasoning.effort/summary/context` | manual budget 近似为档位；GPT-5.6+ 支持 `max` 和 `context=all_turns` |
| 推理连续性 | `thinking{signature}` | `reasoning{id,encrypted_content}` | 用版本化 signature 双向封装；缺失加密状态会明确失败，不假装成功 |
| 上下文治理 | `clear_thinking`、`clear_tool_uses`、`compact_*`、`compaction` block | 网关本地编辑 + `context_management[{type:compaction}]`、encrypted compaction item | clear 策略在转换前执行；OpenAI encrypted compaction item 用 opaque thinking signature 回传并删除其前方历史；仅有压缩状态而无 final message 时返回 `pause_turn` |
| Prompt cache | `cache_control`、5m/1h | `prompt_cache_breakpoint`、`prompt_cache_options`、`prompt_cache_key` | GPT-5.6+ 对 `input_text/image/file` 使用 explicit breakpoint；生成跨 follow-up 稳定 key；OpenAI 当前唯一可配置的是至少 30m |
| 结构化输出 | `output_config.format` | `text.format` | JSON Schema 缺 name 时补稳定默认名 |
| Web search | `web_search_*` + domain/location | `web_search` + filters/location | allowed domains 和 location 映射；托管调用状态用 opaque signature 续传 |
| Code execution | `code_execution_*` | `code_interpreter` | container 可保留；托管 output item 用 opaque signature 续传 |
| 远程 MCP | `mcp_servers`、`mcp_toolset` | `mcp` tool、`allowed_tools`、`defer_loading` | URL、authorization、allowlist、deferred loading 映射；托管状态 opaque 续传 |
| 图片/文件 | image/document content block | `input_image/input_file` | URL/base64/file id 映射；text document 转 base64 file data |
| 用量 | `input/output/cache_creation/cache_read` | `input/output` + `input_tokens_details` | `cached_tokens` 和 `cache_write_tokens` 分别恢复为 Claude cache read/creation |
| 流式终止 | Claude content/message SSE | Responses typed SSE | 支持 delta 与 done fallback、reasoning/custom tool；无 final message 的可回放状态 → `pause_turn`；failed/提前 EOF 明确报错，不能伪造成 `end_turn` |

Responses 的 `reasoning`、`program`、`program_output`、web search、code interpreter、MCP 等 provider-hosted item，在 Claude 没有完全同构的内容块。服务把原始 item 封装进带版本前缀的 Claude thinking signature；Claude Code 下一轮回传后恢复为原始 Responses input item。对于 `program`/`program_output`，还会额外生成配对的 Claude `server_tool_use(code_execution)` / `code_execution_tool_result`，使 `caller.tool_id` 有真实可见的对应块；opaque signature 负责保留 OpenAI 的 JavaScript fingerprint 和完整回放状态。其他托管工具的完整内部事件仍不会原生展示在 Claude Code UI。

`mid_conv_system` 会按原消息位置转换为 Responses developer item，外层和内部 text block 的 cache breakpoint 都会保留。Claude beta 的 `fallback` 回放块按官方定义不会进入提示词，Responses 路径会兼容接收并省略它；`fallbacks` 模型链本身没有 Responses 等价物，仍会明确失败，不能伪造为同一模型路由策略。

`Read` 工具有一个专门兼容处理：如果 Responses/Codex 输出 `pages: ""`，非流式和流式转换都会删除该字段，避免 Claude Code 因空页码参数拒绝执行。

### 通过映射仍可工作的 Claude Code 功能

- 常规编码工具循环：`Read`、`Write`、`Edit`、`Bash`、`Glob`、`Grep`、Todo/Task、plan mode、AskUserQuestion 等都作为普通 function tool 保留名称、schema、调用 id 和结果。
- 子 Agent/后台任务：工具 schema、调用和 `tool_result` 可结构化映射；Responses 的 delta/done、失败和提前断流已按 Claude SSE 终止语义处理，不会把未完成上游流伪装成主 Agent 正常结束。
- adaptive/manual thinking、extended thinking 的摘要展示，以及跨轮 encrypted reasoning 恢复。
- Claude Code 的 deferred tools/tool search；GPT-5.4+ 可使用 Responses 原生能力。
- Claude programmatic tool calling；GPT-5.6+ 可让 Responses program 调用 Claude Code 暴露的函数，并在结果回传时保留 `caller`。
- 普通远程 MCP、Responses web search/code interpreter 的模型侧能力和跨轮状态。后两者的完整托管事件不会原生显示在 Claude Code UI。
- 自动/显式 prompt caching、Responses server-side compaction，以及 Claude readable compaction summary 的降级续传。
- JSON Schema 输出、图片、PDF/文件输入、fast/service tier 和精确 cache usage。

### 无法无损映射或只能近似映射

- `stop_sequences` 与 `top_k`：Responses 没有对应参数，请求明确失败。
- `output_config.task_budget`：Responses 没有等价预算参数，请求明确失败。
- manual thinking 的精确 `budget_tokens`：只能近似为 OpenAI reasoning effort 档位。
- cache TTL：Claude 的 5m/1h 统一映射到 OpenAI 当前唯一可配置的至少 30m（实际可能保留更久）；assistant `output_text` 上的显式 cache marker 没有合法 breakpoint 位置，只能依赖相同 `prompt_cache_key` 的隐式缓存。`max_tokens: 0` cache-only 与 Claude cache diagnostics 也没有等价语义，会明确失败。
- compaction 表示：Claude 原生 compaction 是可读 summary，OpenAI 是不可读 encrypted item，不能伪装为同一内容块；服务保留上下文效果和可重放状态，但 UI 形态不同。`clear_tool_uses_20250919` 和 `clear_thinking_20251015` 由网关本地执行；compaction instructions、pause 及未知 memory edits 仍会明确失败。
- Programmatic 的 runtime 并非同一个实现：OpenAI 执行 JavaScript program，Claude 原生 code execution 以 Python/bash container 为主。服务会生成可见的 Claude code-execution call/result 并用 opaque signature 精确续传 OpenAI fingerprint，但 container 生命周期、语言/runtime 不能伪装成同一个；GPT-5.5 及更早模型不启用该映射。
- Programmatic client tool result 在两边都必须是字符串或 text blocks；图片、文档等结果不能交给正在等待的 program，转换器会明确失败。Claude 工具协议没有 OpenAI function `output_schema` 字段，结构化返回格式只能继续依赖工具 description。
- Responses free-form custom tool 输入只有字符串，而 Claude `tool_use.input` 必须是对象；包装后的工具只有在 Claude Code 确实暴露同名且接受 `input` 字段时才可执行。
- web search `blocked_domains`、Claude web-fetch/advisor/memory/bash/text-editor/computer server tools，以及 Claude 原生 server-tool 历史块没有无损对应；明确失败。web search/code execution/MCP 只转换可等价子集。
- MCP “默认允许、逐项禁用”的 denylist 无法用 Responses `allowed_tools` allowlist 无损表达；明确失败。
- 原生 Claude signed/redacted thinking 不能伪装成 OpenAI encrypted reasoning；只有本服务生成的版本化签名可安全回传。
- Claude Messages 没有 `phase` 字段；当前按同一 assistant message 是否包含 `tool_use` 推断，第三方构造的复杂交错内容不能百分之百还原意图。
- Claude citation 与 OpenAI annotation 的加密索引/来源结构不相同；正文保留，完整结构化引用元数据不伪造。
- OpenAI 独有的 shell、apply_patch、skills、image generation、computer、file search、conversation/`previous_response_id` 等服务端能力，不能仅从 Claude Messages 请求无损表达。已出现在上游 output 中的未知 item 会 opaque 续传，但不冒充 Claude 原生工具。
- `inference_geo` 没有 Responses 请求级等价物；未识别的新 Claude 顶层字段或内容块继续 fail-closed，避免静默降级。

## 对 Claude Code 的实际结论

- 常规编码循环（读写文件、命令执行、Todo/计划、普通 MCP、自定义工具、thinking、结构化输出）可通过 Responses 转换工作。
- Responses GPT-5.4+ 对延迟工具和工具前导语的保真度更高；GPT-5.6+ 还能保留 `max` effort、跨轮 persisted reasoning、显式 cache breakpoint 和 programmatic tool calling。
- OpenAI 路径使用无状态循环保护：相同名称和执行参数的工具连续成功 2 次时，仅在本次上游请求中追加纠偏提示；连续成功 3 次时转换 fail-closed，阻止第 4 次执行。Bash 调用的自然语言 `description` 不属于执行参数，修改描述不会绕过保护。新的用户指令会重置计数，不影响用户明确要求的重复操作。
- 任何无法可靠表达的字段均显式返回 conversion error，不再静默丢弃；受支持的 clear 策略会先在网关本地执行，`clear_thinking + keep all` 则按无操作语义保留完整 thinking。

## 模型与错误语义

路由选定渠道后，会先把 Claude 请求中的模型改写为实际上游模型，再进行协议转换。Bedrock InvokeModel 的模型只写入 URI，不写进请求 JSON；返回客户端时，非流式响应和流式 `message_start` 都恢复为客户端请求的模型名。跨协议上游错误会转换为正确的 Claude error envelope，并尽量保留 HTTP 状态和上游错误消息。流式异常事件以及未收到 `message_stop` 的提前 EOF 都会明确失败；即使部分 SSE 已经写出，网关也会追加协议对应的 `error` 事件，避免 Claude Code 把连接关闭误判为正常 `end_turn`。

企业上游在并发子 Agent 场景返回 429 时，网关会在尚未输出任何流数据前进行有限退避重试；重试仍失败时向 Claude Code 保留 HTTP 429 和 `rate_limit_error`，不再统一伪装为 502。流式连接同时具有真实的首 body 数据超时和数据间 idle timeout，避免后台 Agent 永久停在最后一个 `tool_result`。上游首包、流间空闲和 Servlet 异步响应默认分别允许 2 分钟、10 分钟和 15 分钟，Nginx `/v1/` 代理读写超时为 20 分钟，避免长 thinking 或计划生成被容器默认的 30 秒异步超时截断，并为上游超时后的 SSE `error` 留出传输窗口。相关参数可通过 `API2API_GATEWAY_ASYNC_TIMEOUT`、`API2API_UPSTREAM_READ_TIMEOUT`、`API2API_STREAMING_FIRST_BYTE_TIMEOUT`、`API2API_STREAMING_IDLE_TIMEOUT`、`API2API_STREAMING_MAX_RETRIES` 和 `API2API_STREAMING_RETRY_BACKOFF` 调整。

## 官方协议依据

- Anthropic Messages create、tool reference、programmatic tool calling、context editing、compaction、prompt caching：<https://platform.claude.com/docs/en/api/messages/create>、<https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-reference>、<https://platform.claude.com/docs/en/agents-and-tools/tool-use/programmatic-tool-calling>、<https://platform.claude.com/docs/en/build-with-claude/context-editing>、<https://platform.claude.com/docs/en/build-with-claude/compaction>、<https://platform.claude.com/docs/en/build-with-claude/prompt-caching>
- AWS Bedrock Claude Messages InvokeModel：<https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters-anthropic-claude-messages.html>
- OpenAI Agents SDK 的 max turns / tool-loop safety：<https://openai.github.io/openai-agents-python/running_agents/>、<https://openai.github.io/openai-agents-js/guides/agents/>
- OpenAI Responses programmatic tool calling、prompt caching、compaction、reasoning：<https://developers.openai.com/api/docs/guides/tools-programmatic-tool-calling>、<https://developers.openai.com/api/docs/guides/prompt-caching>、<https://developers.openai.com/api/docs/guides/compaction>、<https://developers.openai.com/api/docs/guides/reasoning>
