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
                mapping("content[].type=document", "input[].content[].type=input_file", "文档块转为文件输入", MappingLossiness.PARTIAL, "CONTENT_BLOCK", "RESHAPE"),
                mapping("content[].type=tool_use", "function_call output item", "工具调用转为 function_call 引用", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].type=tool_result", "function_call_output item", "工具结果转为 function_call_output", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].type=thinking", "reasoning item (encrypted)", "推理块转为加密 reasoning 项", MappingLossiness.NONE, "REASONING", "RESHAPE"),
                mapping("model", "model", "模型标识符透传", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("max_tokens", "max_output_tokens", "最大 token 数字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "temperature", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_p", "top_p", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_k", "(dropped)", "top_k 不被 Responses API 支持", MappingLossiness.LOSSY, "MODEL", "DROP"),
                mapping("stop_sequences", "(dropped)", "stop_sequences 不被 Responses API 支持", MappingLossiness.LOSSY, "MODEL", "DROP"),
                mapping("tools", "tools", "工具定义转为 function 类型工具", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tool_choice", "tool_choice", "工具选择策略转换", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("thinking/reasoning", "reasoning", "推理配置转为 Responses reasoning 格式", MappingLossiness.PARTIAL, "REASONING", "RESHAPE"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT"),
                mapping("metadata", "metadata", "元数据透传", MappingLossiness.NONE, "METADATA", "DIRECT"),
                mapping("service_tier", "service_tier", "服务层级映射", MappingLossiness.NONE, "METADATA", "TRANSFORM"),
                mapping("cache_control", "prompt_cache_options", "缓存控制映射", MappingLossiness.PARTIAL, "METADATA", "RESHAPE"),
                mapping("output_config.format", "text.format", "输出格式约束映射", MappingLossiness.NONE, "MODEL", "RESHAPE")
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
                mapping("content[].type=tool_use", "tool_calls[]", "工具调用转为 tool_calls", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("content[].type=tool_result", "role=tool message", "工具结果转为 tool 角色消息", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("model", "model", "Direct passthrough", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("max_tokens", "max_completion_tokens", "字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "temperature", "Direct passthrough (reasoning model 时丢弃)", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_p", "top_p", "Direct passthrough (reasoning model 时丢弃)", MappingLossiness.NONE, "MODEL", "DIRECT"),
                mapping("top_k", "(dropped)", "Chat Completions 不支持 top_k", MappingLossiness.LOSSY, "MODEL", "DROP"),
                mapping("stop_sequences", "stop", "停止序列字段重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("tools", "tools", "工具定义转为 Chat function 格式", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tool_choice", "tool_choice", "工具选择策略映射", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("thinking/reasoning", "reasoning_effort", "推理配置转为 effort 级别", MappingLossiness.PARTIAL, "REASONING", "TRANSFORM"),
                mapping("output_config.format", "response_format", "输出格式转为 response_format", MappingLossiness.NONE, "MODEL", "RESHAPE"),
                mapping("stream", "stream + stream_options", "流式开关加配套 options", MappingLossiness.NONE, "STREAMING", "RESHAPE"),
                mapping("service_tier", "service_tier", "Direct passthrough", MappingLossiness.NONE, "METADATA", "DIRECT"),
                mapping("metadata.user_id", "user", "用户标识字段提升", MappingLossiness.PARTIAL, "METADATA", "RESHAPE")
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
        map.put(key(ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST), List.of(
                mapping("messages", "messages", "Claude 消息转为 Bedrock Converse 消息格式", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("messages[].content", "messages[].content", "内容块转为 Bedrock ContentBlock union", MappingLossiness.NONE, "CONTENT_BLOCK", "RESHAPE"),
                mapping("system", "system[]", "系统提示词转为 Bedrock system 数组", MappingLossiness.NONE, "MESSAGE", "RESHAPE"),
                mapping("model", "modelId (URI param)", "模型标识映射为 Bedrock modelId URI", MappingLossiness.NONE, "MODEL", "TRANSFORM"),
                mapping("max_tokens", "inferenceConfig.maxTokens", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("temperature", "inferenceConfig.temperature", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("top_p", "inferenceConfig.topP", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("stop_sequences", "inferenceConfig.stopSequences", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME"),
                mapping("tools", "toolConfig.tools", "工具定义转为 Bedrock toolSpec", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("tool_choice", "toolConfig.toolChoice", "工具选择策略映射", MappingLossiness.NONE, "TOOL", "RESHAPE"),
                mapping("thinking", "additionalModelRequestFields.thinking", "推理配置透传", MappingLossiness.NONE, "REASONING", "RENAME"),
                mapping("stream", "stream", "Direct passthrough", MappingLossiness.NONE, "STREAMING", "DIRECT"),
                mapping("top_k", "additionalModelRequestFields.top_k", "路径重命名", MappingLossiness.NONE, "MODEL", "RENAME")
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
        return FieldMapping.detailed(sourceField, targetField, rule, lossiness, category, mappingType,
                sourceField, targetField, null, null, null, true, null, null, null);
    }
}
