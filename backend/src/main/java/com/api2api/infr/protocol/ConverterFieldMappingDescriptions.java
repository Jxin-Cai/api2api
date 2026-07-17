package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.domain.protocol.model.MappingLossiness;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converter 字段映射描述注册表。
 * 与 ProtocolConverterConfiguration/BedrockConverseProtocolMessageConverter 中的执行逻辑共存于同一包，
 * 确保映射描述与实际转换逻辑的同源性。
 */
final class ConverterFieldMappingDescriptions {

    private static final Map<String, List<FieldMapping>> REGISTRY;

    static {
        Map<String, List<FieldMapping>> map = new HashMap<>();

        // ===== Claude Messages → OpenAI Responses =====
        map.put(key(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.REQUEST), List.of(
                mapping("messages", "input", "消息数组结构转换为 input 数组", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("messages[].role", "input[].role", "Direct passthrough", MappingLossiness.NONE, "MESSAGE", "DIRECT"),
                mapping("messages[].content", "input[].content", "内容块转为 Responses 内容格式", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("system", "input[developer message]", "系统提示词映射为 developer 角色消息", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("content[].type=text", "input[].content[].type=input_text", "文本块类型名转换", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].type=image", "input[].content[].type=input_image", "图片块格式转换 (base64→data URI)", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].source.media_type", "input[].content[].image_url", "作为 data URI 的 MIME 类型部分", MappingLossiness.NONE, "CONTENT_BLOCK", "TRANSFORM"),
                mapping("content[].source.data", "input[].content[].image_url", "base64 数据组装为 data URI", MappingLossiness.NONE, "CONTENT_BLOCK", "TRANSFORM"),
                mapping("content[].source.url", "input[].content[].image_url", "URL 原样写入 image_url", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].type=document", "input[].content[].type=input_file", "文档块转为文件输入", MappingLossiness.PARTIAL, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].source.data", "input[].content[].file_data", "base64 文档数据写入 file_data", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].source.url", "input[].content[].file_url", "文档 URL 写入 file_url", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].source.file_id", "input[].content[].file_id", "文件 ID 直接映射", MappingLossiness.NONE, "CONTENT_BLOCK", "DIRECT"),
                mapping("content[].cache_control", "input[].content[].cache_control", "显式缓存断点按 Responses 能力转换；不可放置位置依赖 prompt_cache_key", MappingLossiness.PARTIAL, "METADATA", "RESHAPE"),
                mapping("content[].type=tool_use", "function_call output item", "工具调用转为 function_call 引用", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].id", "function_call.call_id", "工具调用 ID 映射为 call_id", MappingLossiness.NONE, "TOOL", "RENAME"),
                mapping("content[].name", "function_call.name", "工具名称直接映射", MappingLossiness.NONE, "TOOL", "DIRECT"),
                mapping("content[].input", "function_call.arguments", "input JSON 对象序列化为 arguments 字符串", MappingLossiness.NONE, "TOOL", "TRANSFORM"),
                mapping("content[].caller", "function_call.caller", "code_execution_* 调用者映射为 programmatic caller 并保留 caller_id", MappingLossiness.NONE, "TOOL", "TRANSFORM"),
                mapping("content[].type=tool_result", "function_call_output item", "工具结果转为 function_call_output", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].tool_use_id", "function_call_output.call_id", "关联工具调用 ID", MappingLossiness.NONE, "TOOL", "RENAME"),
                mapping("content[].content", "function_call_output.output", "工具结果文本写入 output", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                unmapped("content[].is_error", "Responses 没有独立错误标记；错误语义仅保留在 output 文本中", "TOOL"),
                mapping("content[].type=thinking", "reasoning item (encrypted)", "推理块转为加密 reasoning 项", MappingLossiness.NONE, "REASONING", "RESHAPE"),
                mapping("content[].thinking", "reasoning.summary", "可读推理文本映射为摘要", MappingLossiness.NONE, "REASONING", "RESHAPE"),
                mapping("content[].signature", "reasoning.encrypted_content", "不透明签名映射为加密内容以便跨轮回放", MappingLossiness.NONE, "REASONING", "RESHAPE"),
                mapping("content[].type=compaction", "context_management[type=compaction]", "Claude 可读压缩块映射为 Responses 服务端压缩状态", MappingLossiness.PARTIAL, "REASONING", "RESHAPE"),
                mapping("content[].type=mid_conv_system", "input[].role=developer", "按原消息位置转换为 developer 输入项", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("model", "model", "模型标识符透传", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("max_tokens", "max_output_tokens", "最大 token 数字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "temperature", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_p", "top_p", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_k", "(dropped)", "top_k 不被 Responses API 支持", MappingLossiness.LOSSY, "MODEL", "DROP"),
                mapping("stop_sequences", "(dropped)", "stop_sequences 不被 Responses API 支持", MappingLossiness.LOSSY, "MODEL", "DROP"),
                mapping("tools", "tools", "工具定义转为 function 类型工具", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tools[].name", "tools[].name", "工具名称直接映射", MappingLossiness.NONE, "TOOL", "DIRECT"),
                mapping("tools[].description", "tools[].description", "工具描述直接映射", MappingLossiness.NONE, "TOOL", "DIRECT"),
                mapping("tools[].input_schema", "tools[].parameters", "输入 Schema 重命名为 parameters", MappingLossiness.NONE, "TOOL", "RENAME"),
                mapping("tools[].strict", "tools[].strict", "严格模式直接映射", MappingLossiness.NONE, "TOOL", "DIRECT"),
                mapping("tools[].defer_loading", "tools[].defer_loading + tool_search", "GPT-5.4+ 保留延迟加载并启用原生 tool_search", MappingLossiness.NONE, "TOOL", "TRANSFORM"),
                mapping("tools[].allowed_callers", "tools[].allowed_callers", "GPT-5.6+ 将 code_execution_* 转为 programmatic 调用者", MappingLossiness.NONE, "TOOL", "TRANSFORM"),
                mapping("tools[].input_examples", "tools[].description", "输入示例追加到工具描述以保留提示信息", MappingLossiness.PARTIAL, "TOOL", "RESHAPE"),
                mapping("tools[].type=web_search_*", "tools[].type=web_search", "映射域名过滤与用户位置；blocked_domains 无法无损表达", MappingLossiness.PARTIAL, "TOOL", "RESHAPE"),
                mapping("tools[].type=code_execution_*", "tools[].type=code_interpreter", "Anthropic 代码执行映射到 Responses 容器工具", MappingLossiness.PARTIAL, "TOOL", "RESHAPE"),
                mapping("mcp_servers", "tools[].type=mcp", "远程 MCP URL、授权、allowlist 与延迟加载转换为 MCP 工具", MappingLossiness.PARTIAL, "TOOL", "RESHAPE"),
                mapping("tool_choice", "tool_choice", "工具选择策略转换", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tool_choice.disable_parallel_tool_use", "parallel_tool_calls", "布尔语义取反", MappingLossiness.NONE, "TOOL", "TRANSFORM"),
                mapping("thinking", "reasoning", "thinking 配置转为 Responses reasoning 格式", MappingLossiness.PARTIAL, "REASONING", "RESHAPE"),
                mapping("reasoning", "reasoning", "reasoning 配置归一化后映射", MappingLossiness.PARTIAL, "REASONING", "TRANSFORM"),
                mapping("thinking.type", "reasoning.effort", "adaptive thinking 映射为 effort", MappingLossiness.PARTIAL, "REASONING", "TRANSFORM"),
                mapping("thinking.display", "reasoning.summary", "summarized/omitted 映射为摘要策略", MappingLossiness.PARTIAL, "REASONING", "TRANSFORM"),
                mapping("output_config.effort", "reasoning.effort", "low/medium/high/xhigh/max 按目标模型能力转换", MappingLossiness.NONE, "REASONING", "RENAME"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT"),
                mapping("metadata", "metadata", "元数据透传", MappingLossiness.NONE, "METADATA", "DIRECT"),
                mapping("service_tier", "service_tier", "服务层级映射", MappingLossiness.NONE, "METADATA", "TRANSFORM"),
                mapping("speed=fast", "service_tier=priority", "Claude fast mode 映射为 Responses priority 服务层级", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM"),
                mapping("container", "tools[].container", "容器标识绑定到 code_interpreter 工具", MappingLossiness.PARTIAL, "METADATA", "RESHAPE"),
                mapping("cache_control", "prompt_cache_options", "缓存控制映射", MappingLossiness.PARTIAL, "METADATA", "RESHAPE"),
                mapping("context_management.edits", "context_management", "clear 策略由网关执行；compact 映射为 Responses compaction", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM"),
                mapping("output_config.format", "text.format", "输出格式约束映射", MappingLossiness.NONE, "MODEL", "RESHAPE"),
                unsupported("output_config.task_budget", "Responses 无跨上下文总任务预算字段", "REASONING"),
                unsupported("inference_geo", "Responses 无请求级推理地域字段", "METADATA"),
                unsupported("diagnostics", "Responses 无 Claude prompt-cache 差异诊断字段", "METADATA"),
                unsupported("fallbacks", "Responses 无等价的按拒绝原因服务端模型链", "MODEL")
        ));

        map.put(key(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("output[].content (text)", "content[].type=text", "输出文本转为 Claude text 内容块", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("output[].type=function_call", "content[].type=tool_use", "function_call 转为 tool_use 块", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("output[].type=reasoning", "content[].type=thinking", "推理输出转为 thinking 块", MappingLossiness.PARTIAL, "REASONING", "RESHAPE"),
                mapping("status", "stop_reason", "状态映射为停止原因", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("usage.input_tokens", "usage.input_tokens", "Direct passthrough", MappingLossiness.NONE, "USAGE", "DIRECT"),
                mapping("usage.output_tokens", "usage.output_tokens", "Direct passthrough", MappingLossiness.NONE, "USAGE", "DIRECT"),
                mapping("usage.input_tokens_details.cached_tokens", "usage.cache_read_input_tokens", "缓存 token 字段路径转换", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("id", "id", "响应 ID 生成", MappingLossiness.NONE, "METADATA", "TRANSFORM"),
                mapping("model", "model", "模型标识符透传", MappingLossiness.NONE, "MODEL", "DIRECT")
        ));

        // ===== OpenAI Responses → Claude Messages =====
        map.put(key(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.REQUEST), List.of(
                mapping("input", "messages", "输入项转为 Claude 消息结构", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("input[].role", "messages[].role", "角色映射 (developer→system)", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("input[].content", "messages[].content", "内容格式转换", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("max_output_tokens", "max_tokens", "字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "temperature", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_p", "top_p", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("tools", "tools", "工具定义转换", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tool_choice", "tool_choice", "工具选择策略映射", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("reasoning", "thinking", "推理配置映射", MappingLossiness.PARTIAL, "REASONING", "RESHAPE"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT")
        ));

        map.put(key(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("content[].type=text", "output[].content (output_text)", "文本块转为 output_text", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("content[].type=tool_use", "output[].type=function_call", "tool_use 转为 function_call", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].type=thinking", "output[].type=reasoning", "thinking 块转为 reasoning 项", MappingLossiness.PARTIAL, "REASONING", "RESHAPE"),
                mapping("stop_reason", "status", "停止原因映射为状态", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("usage.input_tokens", "usage.input_tokens", "Direct passthrough", MappingLossiness.NONE, "USAGE", "DIRECT"),
                mapping("usage.output_tokens", "usage.output_tokens", "Direct passthrough", MappingLossiness.NONE, "USAGE", "DIRECT"),
                mapping("usage.cache_read_input_tokens", "usage.input_tokens_details.cached_tokens", "缓存 token 路径转换", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("id", "id", "Direct passthrough", MappingLossiness.NONE, "METADATA", "DIRECT"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT")
        ));

        // ===== Claude Messages → OpenAI Chat Completions =====
        map.put(key(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.REQUEST), List.of(
                mapping("messages", "messages", "Claude 消息转为 Chat 消息格式", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("system", "messages[0] (system/developer)", "系统提示词变为消息列表首项", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("content[].type=text", "content (string)", "文本内容块转为字符串内容", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].type=image", "content[].type=image_url", "图片块转为 image_url 格式", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].source.media_type", "content[].image_url.url", "作为 data URI 的 MIME 类型部分", MappingLossiness.NONE, "CONTENT_BLOCK", "TRANSFORM"),
                mapping("content[].source.data", "content[].image_url.url", "base64 数据组装为 data URI", MappingLossiness.NONE, "CONTENT_BLOCK", "TRANSFORM"),
                mapping("content[].source.url", "content[].image_url.url", "URL 原样写入 image_url.url", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                unsupported("content[].type=document", "Chat Completions 消息内容不支持通用文件输入", "CONTENT_BLOCK"),
                mapping("content[].type=tool_use", "tool_calls[]", "工具调用转为 tool_calls", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].id", "tool_calls[].id", "工具调用 ID 直接映射", MappingLossiness.NONE, "TOOL", "DIRECT"),
                mapping("content[].name", "tool_calls[].function.name", "工具名称映射为 function.name", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].input", "tool_calls[].function.arguments", "input JSON 对象序列化为 arguments 字符串", MappingLossiness.NONE, "TOOL", "TRANSFORM"),
                mapping("content[].type=tool_result", "role=tool message", "工具结果转为 tool 角色消息", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].tool_use_id", "tool_call_id", "关联工具调用 ID", MappingLossiness.NONE, "TOOL", "RENAME"),
                mapping("content[].content", "content", "工具结果文本写入 content", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                unmapped("content[].is_error", "Chat Completions 没有独立错误标记", "TOOL"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("max_tokens", "max_completion_tokens", "字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "temperature", "Direct passthrough (reasoning model 时丢弃)", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_p", "top_p", "Direct passthrough (reasoning model 时丢弃)", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_k", "(dropped)", "Chat Completions 不支持 top_k", MappingLossiness.LOSSY, "MODEL", "DROP"),
                mapping("stop_sequences", "stop", "停止序列字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("tools", "tools", "工具定义转为 Chat function 格式", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tools[].name", "tools[].function.name", "工具名称映射为 function.name", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tools[].description", "tools[].function.description", "工具描述映射为 function.description", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tools[].input_schema", "tools[].function.parameters", "输入 Schema 映射为 function.parameters", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tools[].strict", "tools[].function.strict", "严格模式映射为 function.strict", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tool_choice", "tool_choice", "工具选择策略映射", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tool_choice.disable_parallel_tool_use", "parallel_tool_calls", "布尔语义取反", MappingLossiness.NONE, "TOOL", "TRANSFORM"),
                mapping("thinking", "reasoning_effort", "thinking 配置转为 effort 级别", MappingLossiness.PARTIAL, "REASONING", "TRANSFORM"),
                mapping("reasoning", "reasoning_effort", "reasoning 配置转为 effort 级别", MappingLossiness.PARTIAL, "REASONING", "TRANSFORM"),
                mapping("output_config.format", "response_format", "输出格式转为 response_format", MappingLossiness.NONE, "MODEL", "RESHAPE"),
                mapping("stream", "stream + stream_options", "流式开关加配套 options", MappingLossiness.NONE, "STREAMING", "RESHAPE"),
                mapping("service_tier", "service_tier", "Direct passthrough", MappingLossiness.NONE, "METADATA", "DIRECT"),
                mapping("metadata.user_id", "user", "用户标识字段提升", MappingLossiness.PARTIAL, "METADATA", "RESHAPE"),
                unsupported("content[].type=thinking", "Chat Completions 没有可逆的推理 item", "REASONING"),
                unsupported("content[].type=redacted_thinking", "Chat Completions 没有加密推理 item", "REASONING"),
                unsupported("content[].type=compaction", "Chat Completions 没有压缩 item", "REASONING"),
                unsupported("tools[].defer_loading", "Chat Completions 不支持 tool search", "TOOL"),
                unsupported("tools[].allowed_callers", "Chat Completions 不支持 programmatic caller", "TOOL"),
                unsupported("mcp_servers", "Chat Completions 没有远程 MCP 等价定义", "TOOL"),
                unsupported("server tools", "Chat Completions 没有 Claude 托管工具", "TOOL"),
                unsupported("cache_control", "Chat Completions 没有显式缓存断点", "METADATA"),
                unsupported("context_management", "Chat Completions 没有上下文编辑语义", "METADATA"),
                unsupported("container", "Chat Completions 没有等价容器字段", "METADATA"),
                unsupported("inference_geo", "Chat Completions 没有请求级推理地域字段", "METADATA"),
                unsupported("diagnostics", "Chat Completions 没有 Claude 诊断字段", "METADATA"),
                unsupported("fallbacks", "Chat Completions 没有服务端模型回退链", "METADATA")
        ));

        map.put(key(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("choices[].message.content", "content[].type=text", "文本内容转为 Claude text 块", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("choices[].message.tool_calls", "content[].type=tool_use", "tool_calls 转为 tool_use 块", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("choices[].finish_reason", "stop_reason", "完成原因映射为 Claude 停止原因", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("usage.prompt_tokens", "usage.input_tokens", "字段重命名", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.completion_tokens", "usage.output_tokens", "字段重命名", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.prompt_tokens_details.cached_tokens", "usage.cache_read_input_tokens", "缓存 token 路径映射", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("id", "id", "响应 ID 透传", MappingLossiness.NONE, "METADATA", "DIRECT"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT")
        ));

        // ===== OpenAI Chat Completions → Claude Messages =====
        map.put(key(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.REQUEST), List.of(
                mapping("messages", "messages", "Chat 消息转为 Claude 消息格式", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("messages[role=system]", "system", "系统消息提取为顶级 system 字段", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("max_completion_tokens", "max_tokens", "字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "temperature", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_p", "top_p", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("stop", "stop_sequences", "停止序列字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("tools", "tools", "Chat function 工具转为 Claude 格式", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tool_choice", "tool_choice", "工具选择策略映射", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT")
        ));

        map.put(key(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("content[].type=text", "choices[].message.content", "文本块拼接为 content 字符串", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("content[].type=tool_use", "choices[].message.tool_calls", "tool_use 块转为 tool_calls", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("stop_reason", "choices[].finish_reason", "停止原因转为 Chat finish_reason", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("usage.input_tokens", "usage.prompt_tokens", "字段重命名", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.output_tokens", "usage.completion_tokens", "字段重命名", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("id", "id", "响应 ID 透传", MappingLossiness.NONE, "METADATA", "DIRECT"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT")
        ));

        // ===== OpenAI Responses → OpenAI Chat Completions =====
        map.put(key(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.REQUEST), List.of(
                mapping("input", "messages", "Responses input 转为 Chat messages", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("max_output_tokens", "max_completion_tokens", "字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "temperature", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_p", "top_p", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("tools", "tools", "工具定义格式转换", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT")
        ));

        map.put(key(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("choices[].message.content", "output[].content (output_text)", "文本内容转为 output_text", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("choices[].message.tool_calls", "output[].type=function_call", "tool_calls 转为 function_call 项", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("choices[].finish_reason", "status", "finish_reason 转为 Responses status", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("usage.prompt_tokens", "usage.input_tokens", "字段重命名", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.completion_tokens", "usage.output_tokens", "字段重命名", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("id", "id", "Direct passthrough", MappingLossiness.NONE, "METADATA", "DIRECT"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT")
        ));

        // ===== OpenAI Chat Completions → OpenAI Responses =====
        map.put(key(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.REQUEST), List.of(
                mapping("messages", "input", "Chat messages 转为 Responses input", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("max_completion_tokens", "max_output_tokens", "字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "temperature", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_p", "top_p", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("tools", "tools", "工具定义格式转换", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT")
        ));

        map.put(key(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("output[].content (output_text)", "choices[].message.content", "output_text 转为 content 字符串", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("output[].type=function_call", "choices[].message.tool_calls", "function_call 项转为 tool_calls", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("status", "choices[].finish_reason", "status 转为 finish_reason", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("usage.input_tokens", "usage.prompt_tokens", "字段重命名", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.output_tokens", "usage.completion_tokens", "字段重命名", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("id", "id", "Direct passthrough", MappingLossiness.NONE, "METADATA", "DIRECT"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT")
        ));

        // ===== Claude Messages → AWS Bedrock Converse =====
        map.put(key(ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolConversionDirection.REQUEST), List.of(
                mapping("model", "modelId (URI param)", "模型标识移动到 Bedrock InvokeModel URI", MappingLossiness.NONE, "MODEL", "TRANSFORM"),
                mapping("stream", "InvokeModelWithResponseStream operation", "流式标记改为选择 Bedrock 流式操作", MappingLossiness.NONE, "STREAMING", "TRANSFORM"),
                mapping("messages", "messages", "Anthropic Messages 内容保持原始结构", MappingLossiness.NONE, "MESSAGE", "DIRECT"),
                mapping("system", "system", "系统提示保持原始结构", MappingLossiness.NONE, "MESSAGE", "DIRECT"),
                mapping("tools", "tools", "工具按目标模型规范化；Claude Code custom.defer_loading 提升为 Bedrock defer_loading", MappingLossiness.PARTIAL, "TOOL", "TRANSFORM"),
                mapping("tool_choice", "tool_choice", "工具选择策略原样保留", MappingLossiness.NONE, "TOOL", "DIRECT"),
                mapping("thinking", "thinking", "Thinking 与 tool_choice 按目标模型能力规范化", MappingLossiness.PARTIAL, "REASONING", "TRANSFORM"),
                mapping("context_management", "context_management", "仅保留目标模型支持的 clearing 或 compaction 编辑", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM"),
                mapping("cache_control", "cache_control", "移除 Bedrock 不支持的 scope 和无效/不兼容 ttl", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM"),
                mapping("anthropic-beta header", "anthropic_beta", "Beta 请求头按 Bedrock 与目标模型能力转换、补齐和过滤", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM"),
                mapping("service_tier", "X-Amzn-Bedrock-Service-Tier", "Claude 服务等级转换为 Bedrock InvokeModel 请求头", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM"),
                mapping("speed", "X-Amzn-Bedrock-PerformanceConfig-Latency", "Claude 速度模式转换为 Bedrock 延迟请求头", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM")
        ));

        map.put(key(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("payload", "payload", "Bedrock Claude Messages 响应保持 Anthropic 消息结构", MappingLossiness.NONE, "MESSAGE", "DIRECT"),
                mapping("content", "content", "文本、工具调用、Thinking 和服务端工具块原样保留", MappingLossiness.NONE, "CONTENT_BLOCK", "DIRECT"),
                mapping("usage", "usage", "输入、输出和缓存 token 用量原样保留", MappingLossiness.NONE, "USAGE", "DIRECT"),
                mapping("stop_reason", "stop_reason", "Claude 停止原因原样保留", MappingLossiness.NONE, "METADATA", "DIRECT")
        ));

        map.put(key(ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST), List.of(
                mapping("messages", "messages", "Claude 消息转为 Bedrock Converse 消息格式", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("messages[].content", "messages[].content", "内容块转为 Bedrock ContentBlock union", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("system", "system[]", "系统提示词转为 Bedrock system 数组", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("content[].text", "messages[].content[].text", "文本块内容写入 Bedrock text union", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].source.media_type", "messages[].content[].image.format", "MIME 类型转换为 Bedrock 图片格式", MappingLossiness.NONE, "CONTENT_BLOCK", "TRANSFORM"),
                mapping("content[].source.data", "messages[].content[].image.source.bytes", "base64 图片数据转换为 bytes", MappingLossiness.NONE, "CONTENT_BLOCK", "TRANSFORM"),
                mapping("content[].source", "messages[].content[].document.source", "文档来源组装为 Bedrock document source", MappingLossiness.PARTIAL, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].title", "messages[].content[].document.name", "文档标题映射为 Bedrock document name", MappingLossiness.PARTIAL, "CONTENT_BLOCK", "RENAME"),
                mapping("content[].context", "messages[].content[].document.context", "文档上下文写入 Bedrock document", MappingLossiness.PARTIAL, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].id", "messages[].content[].toolUse.toolUseId", "工具调用 ID 映射为 toolUseId", MappingLossiness.NONE, "TOOL", "RENAME"),
                mapping("content[].name", "messages[].content[].toolUse.name", "工具名称直接映射", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].input", "messages[].content[].toolUse.input", "工具输入对象直接映射", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].tool_use_id", "messages[].content[].toolResult.toolUseId", "关联调用 ID 映射为 toolUseId", MappingLossiness.NONE, "TOOL", "RENAME"),
                mapping("content[].content", "messages[].content[].toolResult.content", "工具结果内容转换为 Bedrock content", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].is_error", "messages[].content[].toolResult.status", "错误标记转换为 status=error", MappingLossiness.NONE, "TOOL", "TRANSFORM"),
                mapping("content[].thinking", "messages[].content[].reasoningContent.reasoningText.text", "推理文本封装为 reasoningText", MappingLossiness.NONE, "REASONING", "RESHAPE"),
                mapping("content[].signature", "messages[].content[].reasoningContent.reasoningText.signature", "推理签名写入 reasoningText.signature", MappingLossiness.NONE, "REASONING", "RESHAPE"),
                mapping("content[].data", "messages[].content[].reasoningContent.redactedContent", "加密推理数据转为 redactedContent", MappingLossiness.NONE, "REASONING", "RESHAPE"),
                mapping("content[].type=mid_conv_system", "messages[].content[].text", "使用显式边界标记降级为 user 文本，避免生成非法 Converse system 角色", MappingLossiness.PARTIAL, "MESSAGE", "RESHAPE"),
                mapping("model", "modelId (URI param)", "模型标识映射为 Bedrock modelId URI", MappingLossiness.NONE, "MODEL", "TRANSFORM"),
                mapping("max_tokens", "inferenceConfig.maxTokens", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "inferenceConfig.temperature", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("top_p", "inferenceConfig.topP", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("stop_sequences", "inferenceConfig.stopSequences", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("tools", "toolConfig.tools", "工具定义转为 Bedrock toolSpec", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tools[].name", "toolConfig.tools[].toolSpec.name", "工具名称映射为 toolSpec.name", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tools[].description", "toolConfig.tools[].toolSpec.description", "工具描述映射为 toolSpec.description", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tools[].input_schema", "toolConfig.tools[].toolSpec.inputSchema", "输入 Schema 映射为 toolSpec.inputSchema", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tools[].defer_loading", "toolConfig.tools[].toolSpec", "延迟工具展开为普通 toolSpec，保留可调用性但增加上下文", MappingLossiness.PARTIAL, "TOOL", "TRANSFORM"),
                mapping("tools[].input_examples", "toolConfig.tools[].toolSpec.description", "输入示例追加到工具描述", MappingLossiness.PARTIAL, "TOOL", "RESHAPE"),
                mapping("tool_choice", "toolConfig.toolChoice", "工具选择策略映射", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("thinking", "additionalModelRequestFields.thinking", "推理配置透传", MappingLossiness.NONE, "REASONING", "RENAME"),
                mapping("output_config.effort", "additionalModelRequestFields.output_config.effort", "推理强度写入 Claude 模型扩展字段", MappingLossiness.NONE, "REASONING", "RESHAPE"),
                mapping("output_config.format", "outputConfig.textFormat", "JSON Schema 输出映射为 Converse Structured Outputs", MappingLossiness.PARTIAL, "REASONING", "RESHAPE"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT"),
                mapping("top_k", "additionalModelRequestFields.top_k", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("metadata", "requestMetadata", "满足 Bedrock 字符限制的元数据转为 requestMetadata", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM"),
                mapping("cache_control", "cachePoint", "顶级、system、content 与 tools 缓存断点转为 Bedrock cachePoint", MappingLossiness.PARTIAL, "METADATA", "RESHAPE"),
                mapping("context_management.edits", "gateway local context edit", "支持的 clear 策略由网关在转换前执行", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM"),
                mapping("speed", "performanceConfig.latency", "fast/standard 映射为 Bedrock 延迟配置", MappingLossiness.PARTIAL, "METADATA", "TRANSFORM"),
                unsupported("context_management.edits[type=compact_*]", "Converse 无法生成 Claude compaction 摘要；必须路由到原生 Bedrock Messages Invoke", "METADATA"),
                unsupported("tools[].allowed_callers", "Bedrock Converse 不支持 programmatic tool calling", "TOOL"),
                unsupported("tool_choice.disable_parallel_tool_use", "Bedrock Converse 没有禁用并行工具的开关", "TOOL"),
                unsupported("server tools", "Bedrock Converse 只能声明客户端 toolSpec", "TOOL"),
                unsupported("mcp_servers", "Bedrock Converse 不支持远程 MCP 定义", "TOOL"),
                unsupported("output_config.task_budget", "Bedrock Converse 无跨上下文任务预算", "REASONING"),
                unsupported("container", "Bedrock Converse 无等价容器字段", "METADATA"),
                unsupported("inference_geo", "Bedrock Converse 无请求级推理地域字段", "METADATA"),
                unsupported("diagnostics", "Bedrock Converse 无 Claude 诊断字段", "METADATA"),
                unsupported("fallbacks", "Bedrock Converse 无服务端模型回退链", "METADATA")
        ));

        map.put(key(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("output.message.content", "content", "Bedrock ContentBlock 转为 Claude content 块", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("output.message.content[].toolUse", "content[].type=tool_use", "Bedrock toolUse 转为 Claude tool_use", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("output.message.content[].reasoningContent", "content[].type=thinking", "Bedrock reasoning 转为 Claude thinking", MappingLossiness.NONE, "REASONING", "RESHAPE"),
                mapping("stopReason", "stop_reason", "停止原因映射", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("usage.inputTokens", "usage.input_tokens", "字段名转换 (camelCase→snake_case)", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.outputTokens", "usage.output_tokens", "字段名转换", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.cacheReadInputTokenCount", "usage.cache_read_input_tokens", "缓存读取 token 映射", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.cacheWriteInputTokenCount", "usage.cache_creation_input_tokens", "缓存写入 token 映射", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("model", "model", "模型标识符透传", MappingLossiness.NONE, "MODEL", "DIRECT")
        ));

        // ===== OpenAI Chat Completions → AWS Bedrock Converse =====
        map.put(key(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST), List.of(
                mapping("messages", "messages", "Chat 消息转为 Bedrock Converse 格式", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("model", "modelId (URI param)", "模型标识转为 Bedrock modelId", MappingLossiness.NONE, "MODEL", "TRANSFORM"),
                mapping("max_completion_tokens", "inferenceConfig.maxTokens", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "inferenceConfig.temperature", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("top_p", "inferenceConfig.topP", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("stop", "inferenceConfig.stopSequences", "字段和路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("tools", "toolConfig.tools", "Chat function 工具转为 Bedrock toolSpec", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT")
        ));

        map.put(key(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("output.message.content[].text", "choices[].message.content", "Bedrock text 转为 Chat content", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("output.message.content[].toolUse", "choices[].message.tool_calls", "Bedrock toolUse 转为 tool_calls", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("stopReason", "choices[].finish_reason", "停止原因映射", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("usage.inputTokens", "usage.prompt_tokens", "字段名转换", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.outputTokens", "usage.completion_tokens", "字段名转换", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT")
        ));

        // ===== OpenAI Responses → AWS Bedrock Converse =====
        map.put(key(ProtocolType.OPENAI_RESPONSES, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST), List.of(
                mapping("input", "messages", "Responses input 转为 Bedrock messages", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("model", "modelId (URI param)", "模型标识转为 Bedrock modelId", MappingLossiness.NONE, "MODEL", "TRANSFORM"),
                mapping("max_output_tokens", "inferenceConfig.maxTokens", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "inferenceConfig.temperature", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("top_p", "inferenceConfig.topP", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("tools", "toolConfig.tools", "Responses 工具转为 Bedrock toolSpec", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT")
        ));

        map.put(key(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.RESPONSE), List.of(
                mapping("output.message.content[].text", "output[].content (output_text)", "Bedrock text 转为 output_text", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("output.message.content[].toolUse", "output[].type=function_call", "Bedrock toolUse 转为 function_call", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("stopReason", "status", "停止原因转为 Responses status", MappingLossiness.NONE, "MESSAGE", "TRANSFORM"),
                mapping("usage.inputTokens", "usage.input_tokens", "字段名转换", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("usage.outputTokens", "usage.output_tokens", "字段名转换", MappingLossiness.NONE, "USAGE", "RENAME"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT")
        ));

        REGISTRY = Collections.unmodifiableMap(map);
    }

    private ConverterFieldMappingDescriptions() {}

    static Optional<List<FieldMapping>> lookup(ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {
        return Optional.ofNullable(REGISTRY.get(key(source, target, direction)));
    }

    private static String key(ProtocolType source, ProtocolType target, ProtocolConversionDirection direction) {
        return source.name() + "_" + target.name() + "_" + direction.name();
    }

    private static FieldMapping mapping(String sourceField, String targetField, String rule, MappingLossiness lossiness, String category, String mappingType) {
        boolean supported = !"DROP".equals(mappingType) && !"UNSUPPORTED".equals(mappingType);
        return FieldMapping.detailed(sourceField, targetField, rule, lossiness, category, mappingType,
                sourceField, targetField, null, null, null, supported, null, null, null);
    }

    private static FieldMapping unmapped(String sourceField, String reason, String category) {
        return FieldMapping.detailed(sourceField, "(unmapped)", reason, MappingLossiness.PARTIAL,
                category, "UNMAPPED", sourceField, "(unmapped)", null, null,
                null, false, null, null, "该字段没有独立目标字段；转换会保留可表达的相邻语义。");
    }

    private static FieldMapping unsupported(String sourceField, String reason, String category) {
        return FieldMapping.detailed(sourceField, "(unsupported)", reason, MappingLossiness.LOSSY,
                category, "UNSUPPORTED", sourceField, "(unsupported)", null, null,
                null, false, null, null, "转换器采用 fail-closed：请求包含该字段时明确报错，不会静默丢弃。");
    }
}
