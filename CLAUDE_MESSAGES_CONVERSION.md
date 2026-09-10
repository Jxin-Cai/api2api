# Claude Messages 上游转换能力

本文描述 Claude Code 通过 `/v1/messages` 接入时，服务转换到 OpenAI Responses 或 OpenAI Chat Completions 的行为。原则是：优先保留可执行能力和上下文信息；可近似的能力明确说明差异；无法保留执行契约或约束的请求明确失败。AWS Bedrock 上游使用原生 Claude Messages InvokeModel 协议，不再支持 Converse。

Bedrock InvokeModel 不托管执行 Anthropic `web_search_20250305`。该工具在 Bedrock 路径会转换成客户端执行的同名 `custom` 工具，输入 schema 要求 `query`；`max_uses`、`allowed_domains` / `blocked_domains` 与 `user_location` 会写入工具契约描述，避免把不受支持的 server tool type 原样发送给 Bedrock。上游返回普通 `tool_use`，调用方须执行搜索并以 `tool_result` 继续工具循环；若必须由模型提供商托管搜索，应选择原生支持 web search 的渠道，而不是 Bedrock InvokeModel。

本次协议复核：2026-09-11，直接对照 Anthropic Messages 和 OpenAI Responses 官方 API 文档。此前的 SDK 审计基线为 2026-07-16（Claude Code 2.1.210、Anthropic TypeScript SDK 0.111.0、OpenAI SDK 6.47.0），这些版本不代表当前最新版。

## OpenAI Chat Completions

Messages 与 Chat Completions 使用直接双向桥接，不经过 Responses 中间结构。Claude `tool_use/tool_result` 会转换为 Chat `tool_calls/tool`，并按调用顺序修复相邻关系；反向转换会把并行工具结果合并到紧跟 assistant 工具调用的单个 user 消息，保证 Anthropic 所需的角色交替与工具配对。Chat 请求没有 token 上限时会补 `max_tokens: 8192`，工具 schema 会归一为 object schema，无效或未声明的命名 `tool_choice` 会被省略，避免兼容上游返回 400。

推理模型的工具循环会保留必要状态：Chat 响应的 `reasoning_content` 转为 Claude `thinking`；Claude Code 在下一轮回传包含 `tool_use` 的 assistant 消息时，明文 `thinking` 恢复为 Chat `reasoning_content`。纯文本完成轮不回放该字段，`redacted_thinking` 和只有签名的内容也不会伪造明文。非流式和流式响应都优先根据实际工具块生成 `tool_use` stop reason，即使兼容上游把同一片段标成 `stop` 或 `content_filter`；缺失响应 ID、空工具参数和 cache write/read usage 也会生成合法的 Messages 结果。

## OpenAI Responses

已映射：system/developer 消息、文本/URL 或 base64 图片、URL/base64/file 文档、普通函数与 free-form custom tool、bash/text-editor/memory 客户端工具、custom tool strict schema、tool choice、并行工具、tool search、web search、code interpreter、远程 MCP、programmatic tool calling、thinking/effort、加密 reasoning、隐式 prompt cache key、JSON Schema 输出、compaction、metadata、service tier/fast mode、cache usage、完整流式结束和上游错误。

### 2026-09 工具兼容增强

- **客户端内置工具**：`bash_20241022/20250124`、`text_editor_20241022/20250124/20250429/20250728`、`memory_20250818` 转成同名 Responses function，补齐 Anthropic 原本隐式提供的参数定义。执行仍由 Messages 客户端负责。旧 editor 保留 `undo_edit`，新版不提供；memory 的 rename 使用 `old_path/new_path`，无需 `path`；bash 的 restart 无需 command。`max_characters` 写入工具说明，实际截断仍由客户端实现。
- **工具发现与续用**：成功 `tool_result` 中的 `tool_reference`、历史 `tool_use`、命名 `tool_choice` 会立即加载对应已声明函数的完整 schema。未发现的函数仍延迟加载；错误结果不会激活工具。未声明的引用只保留文字，不凭空生成工具定义。
- **旧模型回退**：不支持原生 `tool_search` 的模型直接加载所有已声明 function/MCP 工具，移除延迟标记，保留 MCP allowlist，并记录 `claude_responses_tool_search_eager_fallback`。代价是输入 token 增加。显式强制调用无法提供的搜索工具仍报错。
- **更完整的工具结果**：`search_result` 在工具结果内保留标题、来源和正文；`browser_state` 保留 tabs 与 state_changes 的 JSON 文本；`is_error=true` 增加 `[Tool execution failed]` 标记并保留原正文/图片，避免把仅含数字等内容的失败误读为成功。成功的 JSON 字符串不额外包装，programmatic 工具仍可直接解析。
- **正确的工具选择**：普通函数继续按 name 选择；web search、code interpreter、tool search 按转换后的类型使用 `allowed_tools` + `required`，避免生成不存在的同名 function。命名选择的 deferred function 先加载再强制选择。

原生客户端工具的参数随 command 变化，无法直接满足 Responses strict 模式“所有属性必填”的要求。此处使用 `strict=false` 保留原客户端输入形状；如请求指定了 `strict=true`，记录 `claude_responses_client_tool_strict_approximated`。普通 custom 工具的显式 strict 设置保持原行为。客户端应按其原有契约校验工具输入。

转换入口是 `GenericProtocolMessageConverter.claudeRequestToResponses`：先校验请求、执行本地上下文编辑，再组装输入项、工具和推理配置。`ClaudeClientToolBridge` 补客户端工具定义，`ClaudeDeferredToolBridge` 处理发现与命名选择。非流式输出和 `UnifiedStreamingConversionAdapter` 的 SSE 输出均继续走原有 function-call → tool-use 桥接，因此新增客户端工具复用同一套参数、ID 和结果续传链路。

### Claude 与 Responses 同类能力字段对照

| 能力 | Claude Messages 字段/块 | OpenAI Responses 字段/item | 当前转换行为 |
| --- | --- | --- | --- |
| 系统指令 | `system` text/block | `input[].role=developer` + `input_text` | 保留块顺序；当前依赖隐式缓存，不发送显式 breakpoint |
| 普通对话 | `messages[].role/content` | `input[]` message | assistant 使用 `output_text`，user 使用 `input_text` |
| 工具前导语/计划文字 | assistant text 与 `tool_use` 同消息 | message `phase=commentary` | 无工具调用的完成文本使用 `phase=final_answer` |
| 客户端工具 | `tools[].name/input_schema/strict`、bash/text-editor/memory 定义 | function `name/parameters/strict` | custom schema 保留；已知内置客户端工具补 schema，strict 的差异见上节；`input_examples` 追加到 description |
| 延迟工具 | `tool_search_tool_*`、`defer_loading`、`tool_reference` | `tool_search`、`defer_loading` | GPT-5.4+ 原生映射；已发现函数立即加载；旧模型全量加载 function/MCP |
| 工具选择 | `tool_choice`、`disable_parallel_tool_use` | `tool_choice`、`parallel_tool_calls` | `auto/any/tool/none` 和并行开关映射 |
| 普通工具调用 | `tool_use{id,name,input}` | `function_call{call_id,name,arguments}` | 双向映射；`tool_result` → `function_call_output`，执行成功或失败均由 `output` 文本表达，不把已返回的失败结果误标成未完成 |
| free-form 工具 | Claude 无独立块，仍表现为 `tool_use` | `custom_tool_call{input}` | 用版本化 tool id 区分；非 JSON input 包装为 `{"input":"..."}`，结果恢复为 `custom_tool_call_output` |
| Programmatic tool calling | `allowed_callers=[direct,code_execution_*]`、`caller` | `allowed_callers=[direct,programmatic]`、`programmatic_tool_calling`、`caller.type=program` | GPT-5.6+ 映射；已支持 `code_execution_20250825`、`20260120`、`20260521`；`caller_id` 通过合成 code-execution tool id 可逆回传，响应采用当前 `code_execution_20260521` 标记 |
| 推理强度 | `thinking`、`output_config.effort` | `reasoning.effort/summary/context` | manual budget 近似为档位；GPT-5.6+ 支持 `max` 和 `context=all_turns` |
| 推理连续性 | `thinking{signature}` | `reasoning{id,encrypted_content}` | 用版本化 signature 双向封装；缺失加密状态会明确失败，不假装成功 |
| 上下文治理 | `clear_thinking`、`clear_tool_uses`、`compact_*`、`compaction` block | 网关本地编辑 + `context_management[{type:compaction}]`、encrypted compaction item | clear 策略在转换前执行；OpenAI encrypted compaction item 用 opaque thinking signature 回传并删除其前方历史；仅有压缩状态而无 final message 时返回 `pause_turn` |
| Prompt cache | `cache_control`、5m/1h | `prompt_cache_key` | 当前显式 breakpoint/options 开关关闭，使用稳定 key 与上游隐式缓存，不保证 Claude TTL/断点语义 |
| 结构化输出 | `output_config.format` | `text.format` | JSON Schema 缺 name 时补稳定默认名 |
| Web search | `web_search_*` + domain/location | `web_search` + filters/location | allowed domains 和 location 映射；托管调用状态用 opaque signature 续传 |
| Code execution | `code_execution_*` | `code_interpreter` | container 可保留；托管 output item 用 opaque signature 续传 |
| 远程 MCP | `mcp_servers`、`mcp_toolset` | `mcp` tool、`allowed_tools`、`defer_loading` | URL、authorization、allowlist、deferred loading 映射；托管状态 opaque 续传 |
| 图片/文件 | image/document content block | `input_image/input_file` | URL/base64/file id 映射；text document 转 base64 file data |
| 用量 | `input/output/cache_creation/cache_read` | `input/output` + `input_tokens_details` | `cached_tokens` 和 `cache_write_tokens` 分别恢复为 Claude cache read/creation |
| 流式终止 | Claude content/message SSE | Responses typed SSE | 支持 delta 与 done fallback、reasoning/custom tool；无 final message 的可回放状态 → `pause_turn`；failed/提前 EOF 明确报错，不能伪造成 `end_turn` |

Responses 的 `reasoning`、`program`、`program_output`、web search、code interpreter、MCP 等 provider-hosted item，在 Claude 没有完全同构的内容块。服务把原始 item 封装进带版本前缀的 Claude thinking signature；Claude Code 下一轮回传后恢复为原始 Responses input item。对于 `program`/`program_output`，还会额外生成配对的 Claude `server_tool_use(code_execution)` / `code_execution_tool_result`，使 `caller.tool_id` 有真实可见的对应块；opaque signature 负责保留 OpenAI 的 JavaScript fingerprint 和完整回放状态。其他托管工具的完整内部事件仍不会原生展示在 Claude Code UI。

`mid_conv_system` 会按原消息位置转换为 Responses developer item；显式 cache breakpoint 当前不发送。Claude beta 的 `fallback` 回放块按官方定义不会进入提示词，Responses 路径会兼容接收并省略它；`fallbacks` 模型链本身没有 Responses 等价物，仍会明确失败，不能伪造为同一模型路由策略。

`Read` 工具有一个专门兼容处理：如果 Responses/Codex 输出 `pages: ""`，非流式和流式转换都会删除该字段，避免 Claude Code 因空页码参数拒绝执行。

### 六轮增量检查（2026-09-11）

1. 推理往返保留完整 Responses reasoning item（含摘要分段、状态），仍可读取早期只包含 id/encrypted_content 的签名。只有完成事件的摘要也会展示。
2. Responses `phase=commentary` 不再误报 `end_turn`；普通响应和 SSE 均返回 `pause_turn`。SSE 后到的 phase 可修正先前文本的默认判断，后续省略 phase 不会覆盖已知值。
3. MCP 兼容旧 `tool_configuration.enabled/allowed_tools` 和新 `default_config.defer_loading/configs`；禁用服务器不发送，旧新允许列表取交集。混合加载模式降级为立即加载，避免工具不可见；denylist 仍不放宽权限。
4. 从 Claude 切换到 Responses 模型时，历史中的原生 redacted thinking 不再使整个请求失败。密文不具备跨模型可用性，省略后推理连续性无法保证。
5. SSE 按 `output_index + content_index` 独立跟踪正文，支持多段文本、混合 delta/done，以及仅 item.done 的正文补发，避免误去重丢段。
6. SSE 按 `summary_index` 独立跟踪推理摘要；done 事件可补齐已发送前缀的剩余文字，重复完成事件不重复输出。

协议依据：[Responses 请求与输入类型](https://developers.openai.com/api/reference/resources/responses/methods/create)、[Claude MCP 配置和迁移](https://platform.claude.com/docs/en/agents-and-tools/mcp-connector)。这些桥接仍要求客户端回传网关生成的 thinking signature，缺失上游 encrypted state 的处理不变。

### 通过映射仍可工作的 Claude Code 功能

- 常规编码工具循环：`Read`、`Write`、`Edit`、`Bash`、`Glob`、`Grep`、Todo/Task、plan mode、AskUserQuestion 等都作为普通 function tool 保留名称、schema、调用 id 和结果。
- 子 Agent/后台任务：工具 schema、调用和 `tool_result` 可结构化映射；Responses 的 delta/done、失败和提前断流已按 Claude SSE 终止语义处理，不会把未完成上游流伪装成主 Agent 正常结束。
- adaptive/manual thinking、extended thinking 的摘要展示，以及跨轮 encrypted reasoning 恢复。
- Claude Code 的 deferred tools/tool search；GPT-5.4+ 可使用 Responses 原生能力。
- Claude programmatic tool calling；GPT-5.6+ 可让 Responses program 调用 Claude Code 暴露的函数，并在结果回传时保留 `caller`。
- 普通远程 MCP、Responses web search/code interpreter 的模型侧能力和跨轮状态。后两者的完整托管事件不会原生显示在 Claude Code UI。
- 已知版本的 Anthropic bash、text editor、memory 客户端工具；工具内的检索结果和浏览器状态文本；已发现 deferred 工具的继续调用。
- 自动/显式 prompt caching、Responses server-side compaction，以及 Claude readable compaction summary 的降级续传。
- JSON Schema 输出、图片、PDF/文件输入、fast/service tier 和精确 cache usage。

### 无法无损映射或只能近似映射

- `stop_sequences` 与 `top_k`：Responses 没有对应参数，请求明确失败。
- `output_config.task_budget`：Responses 没有等价预算参数，请求明确失败。
- manual thinking 的精确 `budget_tokens`：只能近似为 OpenAI reasoning effort 档位。
- cache TTL：OpenAI 官方已支持 GPT-5.6+ explicit breakpoint 与 `prompt_cache_options`，但当前代码的兼容开关关闭，未发送这些字段；只生成稳定 `prompt_cache_key` 并依赖上游隐式缓存。Claude 5m/1h TTL 不被保证。`max_tokens: 0` cache-only 与 Claude cache diagnostics 没有等价语义，会明确失败。
- compaction 表示：Claude 原生 compaction 是可读 summary，OpenAI 是不可读 encrypted item，不能伪装为同一内容块；服务保留上下文效果和可重放状态，但 UI 形态不同。`clear_tool_uses_20250919` 和 `clear_thinking_20251015` 由网关本地执行；compaction instructions、pause 及未知 memory edits 仍会明确失败。
- Programmatic 的 runtime 并非同一个实现：OpenAI 执行 JavaScript program，Claude 原生 code execution 以 Python/bash container 为主。服务会生成可见的 Claude code-execution call/result 并用 opaque signature 精确续传 OpenAI fingerprint，但 container 生命周期、语言/runtime 不能伪装成同一个；GPT-5.5 及更早模型不启用该映射。
- Programmatic client tool result 在两边都必须是字符串或 text blocks；图片、文档等结果不能交给正在等待的 program，转换器会明确失败。Claude 工具协议没有 OpenAI function `output_schema` 字段，结构化返回格式只能继续依赖工具 description。
- Responses free-form custom tool 输入只有字符串，而 Claude `tool_use.input` 必须是对象；包装后的工具只有在 Claude Code 确实暴露同名且接受 `input` 字段时才可执行。
- web search `blocked_domains`、Claude web-fetch/advisor/computer/browser toolset 等执行契约，以及 Claude 原生 server-tool 历史块仍没有完整对应；明确失败。bash/text-editor/memory 是客户端工具，已按前述版本支持，未知新版本仍需核对 schema。web search/code execution/MCP 只转换部分能力。
- MCP “默认允许、逐项禁用”的 denylist 无法用 Responses `allowed_tools` allowlist 无损表达；明确失败。
- 原生 Claude signed/redacted thinking 不能伪装成 OpenAI encrypted reasoning；外部 signed thinking 和 redacted thinking 省略，保留其余对话并对 redacted 降级记录结构化日志。只有本服务生成的版本化 Responses 签名可恢复上游推理状态。
- Claude Messages 没有 `phase` 字段；当前按同一 assistant message 是否包含 `tool_use` 推断，第三方构造的复杂交错内容不能百分之百还原意图。
- Claude citation 与 OpenAI annotation 的加密索引/来源结构不相同；正文保留，完整结构化引用元数据不伪造。
- OpenAI 独有的 shell、apply_patch、skills、image generation、computer、file search、conversation/`previous_response_id` 等服务端能力，不能仅从 Claude Messages 请求无损表达。已出现在上游 output 中的未知 item 会 opaque 续传，但不冒充 Claude 原生工具。
- `inference_geo` 没有 Responses 请求级等价物；未识别的新 Claude 顶层字段或内容块继续 fail-closed，避免静默降级。

## 对 Claude Code 的实际结论

- 常规编码循环（读写文件、命令执行、Todo/计划、普通 MCP、自定义工具、thinking、结构化输出）可通过 Responses 转换工作。
- Responses GPT-5.4+ 对延迟工具和工具前导语的保真度更高；GPT-5.6+ 还能保留 `max` effort、跨轮 persisted reasoning 和 programmatic tool calling。显式 cache breakpoint 当前未启用。
- OpenAI 路径使用无状态循环保护：相同名称和执行参数的工具连续成功 2 次时，仅在本次上游请求中追加纠偏提示；连续成功 3 次时转换 fail-closed，阻止第 4 次执行。Bash 调用的自然语言 `description` 不属于执行参数，修改描述不会绕过保护。新的用户指令会重置计数，不影响用户明确要求的重复操作。
- 未支持的执行契约或约束返回 conversion error；允许的近似转换见前述兼容增强与限制。受支持的 clear 策略先在网关本地执行，`clear_thinking + keep all` 按无操作语义保留完整 thinking。

## 模型与错误语义

路由选定渠道后，会先把 Claude 请求中的模型改写为实际上游模型，再进行协议转换。Bedrock InvokeModel 的模型只写入 URI，不写进请求 JSON；返回客户端时，非流式响应和流式 `message_start` 都恢复为客户端请求的模型名。跨协议上游错误会转换为正确的 Claude error envelope，并尽量保留 HTTP 状态和上游错误消息。流式异常事件以及未收到 `message_stop` 的提前 EOF 都会明确失败；即使部分 SSE 已经写出，网关也会追加协议对应的 `error` 事件，避免 Claude Code 把连接关闭误判为正常 `end_turn`。

企业上游在并发子 Agent 场景返回 429 时，网关会在尚未输出任何流数据前进行有限退避重试；重试仍失败时向 Claude Code 保留 HTTP 429 和 `rate_limit_error`，不再统一伪装为 502。流式连接同时具有真实的首 body 数据超时和数据间 idle timeout，避免后台 Agent 永久停在最后一个 `tool_result`。上游首包、流间空闲和 Servlet 异步响应默认分别允许 2 分钟、10 分钟和 15 分钟，Nginx `/v1/` 代理读写超时为 20 分钟，避免长 thinking 或计划生成被容器默认的 30 秒异步超时截断，并为上游超时后的 SSE `error` 留出传输窗口。相关参数可通过 `API2API_GATEWAY_ASYNC_TIMEOUT`、`API2API_UPSTREAM_READ_TIMEOUT`、`API2API_STREAMING_FIRST_BYTE_TIMEOUT`、`API2API_STREAMING_IDLE_TIMEOUT`、`API2API_STREAMING_MAX_RETRIES` 和 `API2API_STREAMING_RETRY_BACKOFF` 调整。

## 官方协议依据

- 本次工具补齐依据：[Anthropic bash](https://platform.claude.com/docs/en/agents-and-tools/tool-use/bash-tool)、[text editor](https://platform.claude.com/docs/en/agents-and-tools/tool-use/text-editor-tool)、[memory](https://platform.claude.com/docs/en/agents-and-tools/tool-use/memory-tool)、[Messages 内容块定义](https://platform.claude.com/docs/en/api/messages/create)、[OpenAI tool search](https://developers.openai.com/api/docs/guides/tools-tool-search)、[function calling / strict / allowed tools](https://developers.openai.com/api/docs/guides/function-calling)、[Responses create](https://developers.openai.com/api/reference/resources/responses/methods/create)。
- Anthropic Messages create、tool reference、programmatic tool calling、context editing、compaction、prompt caching：<https://platform.claude.com/docs/en/api/messages/create>、<https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-reference>、<https://platform.claude.com/docs/en/agents-and-tools/tool-use/programmatic-tool-calling>、<https://platform.claude.com/docs/en/build-with-claude/context-editing>、<https://platform.claude.com/docs/en/build-with-claude/compaction>、<https://platform.claude.com/docs/en/build-with-claude/prompt-caching>
- AWS Bedrock Claude Messages InvokeModel：<https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters-anthropic-claude-messages.html>
- OpenAI Agents SDK 的 max turns / tool-loop safety：<https://openai.github.io/openai-agents-python/running_agents/>、<https://openai.github.io/openai-agents-js/guides/agents/>
- OpenAI Responses programmatic tool calling、prompt caching、compaction、reasoning：<https://developers.openai.com/api/docs/guides/tools-programmatic-tool-calling>、<https://developers.openai.com/api/docs/guides/prompt-caching>、<https://developers.openai.com/api/docs/guides/compaction>、<https://developers.openai.com/api/docs/guides/reasoning>
