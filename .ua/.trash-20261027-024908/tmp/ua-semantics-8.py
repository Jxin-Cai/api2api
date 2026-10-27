from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_8','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={
'ClaudeMessagesToResponsesStreamingConverter.java':('将 Claude Messages 的 SSE 生命周期转换为 Responses 事件流，维护文本、工具调用及思考内容块状态，并在流结束时汇总响应与令牌用量。',['协议转换','流式响应','状态机','工具调用'],'Claude 到 Responses 的流式转换'),
'ClaudeMessagesUsageExtractor.java':('从 Claude Messages 响应中提取统一令牌用量，优先采用顶层统计，兼容迭代用量及缓存创建、读取字段。',['用量统计','缓存令牌','协议适配'],'Claude 用量统计'),
'ClaudeResponsesMediaMapper.java':('双向映射 Claude 图片及文档块与 Responses 多模态输入，保留 MIME 类型、文件名及内联数据，并将不能附在文件上的说明转换为伴随文本。',['多模态','媒体映射','协议转换','文件处理'],'多协议图片与文档映射'),
'ClaudeThinkingStateBridge.java':('通过 Responses 的 encrypted_content 字段封装与恢复 Claude 原生签名思考及脱敏思考块，使无状态客户端能够回放推理上下文。',['推理状态','状态桥接','序列化'],'Claude 思考状态的往返桥接'),
'ClaudeToolsetBridge.java':('将带版本的 Claude 计算机及浏览器工具集重建为 Responses 函数命名空间，校验字段、调用方限制及命名冲突。',['工具集','命名空间','字段校验'],'Claude 工具集映射'),
'ConversationEffortBridge.java':('解析跨协议会话中的推理强度更新，在新用户输入边界激活 Claude 待生效配置，并转换 Responses 的会话推理设置。',['会话配置','推理强度','状态桥接'],'会话推理强度配置解析'),
'ConverterFieldMappingDescriptions.java':('注册不同协议及请求响应方向的字段映射说明，记录转换规则、内容类别与有损等级，供执行程序及管理界面展示。',['字段映射','转换元数据','注册表'],'协议转换字段映射说明'),
'DefaultProtocolConversionServiceAdapter.java':('将领域协议转换服务接入 Spring 转换策略与用量提取器，按协议对及方向分派请求和响应转换，并支持同协议兼容处理。',['服务适配','策略分派','协议转换','用量统计'],'网关协议转换分派'),
'GenericProtocolMessageConverter.java':('实现 Claude Messages、Chat Completions 与 Responses 的请求和响应双向转换，覆盖消息、多模态、工具历史、推理状态、缓存及结构化输出。',['协议转换','消息映射','工具调用','推理状态','多模态'],'通用协议请求与响应转换'),
'OpenAIChatCompletionsUsageExtractor.java':('从 Chat Completions 响应提取统一用量，拆分提示词令牌与缓存读取、写入令牌，并处理缺失统计。',['用量统计','缓存令牌','协议适配'],'Chat Completions 用量归一化'),
'OpenAIImagesUsageExtractor.java':('提取 Images 响应的输入与输出令牌，缓存用量固定为零；没有 usage 的模型响应返回未知用量。',['图像接口','用量统计','协议适配'],'Images 用量归一化'),
'OpenAIResponsesRequestConverter.java':('规范化同协议 Responses 请求的客户端历史封装，移除 author 扩展并修正助手历史文本类型及输入缓存断点。',['历史兼容','请求转换','字段映射'],'Responses 客户端历史规范化'),
'OpenAIResponsesUsageExtractor.java':('将 Responses 用量转换为统一统计，兼容多种缓存写入字段并分离缓存读取与普通输入令牌。',['用量统计','缓存令牌','协议适配'],'Responses 用量归一化'),
'ProtocolConversionDirection.java':('定义请求和响应两种转换方向，作为转换器与转换程序注册索引的组成部分。',['枚举','转换方向','协议模型'],'协议转换方向选择'),
'ProtocolConversionProgramRegistry.java':('按源协议、目标协议及方向建立不可变转换程序索引，拒绝重复注册，并提供请求与响应字段映射查询。',['注册表','转换程序','字段映射'],'协议转换程序注册与查询'),
'ProtocolConverterConfiguration.java':('通过 Spring 配置注册 Responses 同协议兼容转换器，以及 Claude、Chat Completions、Responses 各方向的通用转换策略。',['依赖注入','转换器注册','配置工厂'],'协议转换器依赖装配'),
'ProtocolJsonSupport.java':('提供协议转换所需的 JSON 解析、序列化及字段读取，统一字段类型校验并在异常中保留路径信息。',['工具函数','序列化','字段校验'],'协议 JSON 处理'),
'ProtocolMessageConverter.java':('定义基础设施消息转换策略接口，统一协议方向、能力判断、载荷转换及同源可执行字段映射描述。',['策略接口','协议转换','能力声明'],'协议转换策略约定'),
'ResponsesProgrammaticToolBridge.java':('转换 Claude 代码执行与 Responses 程序化工具调用的调用方关联，保留允许调用方配置及程序工具标识。',['工具调用','调用方关联','状态桥接'],'程序化工具调用关联转换'),
'ResponsesReasoningBridge.java':('把 Responses 不透明推理状态或条目编码到 Claude thinking 签名，并提供解码恢复，以支持跨协议多轮上下文回放。',['推理状态','状态桥接','序列化'],'Responses 推理状态往返桥接'),
'ResponsesToClaudeRequestConverter.java':('直接将 Responses 请求转换为 Claude Messages，保留结构化工具、推理与压缩状态，并修复工具调用结果配对及相邻同角色消息。',['请求转换','历史修复','工具调用','推理状态'],'Responses 到 Claude 的请求转换'),
'ResponsesToolCallBridge.java':('在 Responses 函数调用及自由格式自定义工具与 Claude tool_use 之间转换，通过工具标识保留调用类型并校验输入。',['工具调用','类型保留','参数解析'],'跨协议工具调用桥接'),
'SseEventTransformer.java':('逐行转换 SSE 的 data 数据而保留事件流框架，避免为整段流式响应构造完整业务对象。',['工具函数','流式响应','事件转换'],'SSE 数据行转换'),
'StreamingPassthroughUsageExtractor.java':('透传上游流式字节并按协议识别用量事件与终止标记，累计 Claude、Responses、Chat Completions 及 Images 的统一用量。',['流式透传','用量统计','事件解析'],'网关流式透传与计量'),
'UnifiedStreamingConversionAdapter.java':('实现网关流式转换端口，按协议对选择 SSE 转换状态机，处理工具和推理增量、完成事件回填及 Bedrock 事件流转换。',['网关适配','流式响应','状态机','工具调用','用量统计'],'网关跨协议流式响应转换'),
'UnifiedUsageExtractor.java':('定义按协议识别响应并提取统一令牌用量的基础设施扩展接口。',['用量统计','扩展接口','协议适配'],'统一令牌用量提取约定'),
'ProtocolContractRegistry.java':('注册可执行协议契约并按协议解析网关请求或构建响应，为 HTTP 接入与协议元数据提供统一契约入口。',['协议契约','注册表','请求解析'],'网关可执行协议契约访问'),
'ConversionRuleContext.java':('以不可变记录携带 JSON 源节点、目标节点及转换需求，作为可执行字段映射规则的上下文。',['转换上下文','不可变记录','字段映射'],'转换规则上下文传递'),
'ExecutableConversionRule.java':('约定转换规则名称、字段映射描述与上下文执行入口，使转换行为与展示元数据共用同一规则。',['规则接口','字段映射','执行约定'],'可执行协议转换规则'),
'JsonConversionOperation.java':('定义 JSON 转换函数式接口，接收源节点与转换需求并返回目标节点。',['函数式接口','JSON','协议转换'],'JSON 转换操作约定'),
'ProtocolConversionProgram.java':('封装协议对、方向及有序规则列表，提供单规则程序构建、顺序执行及字段映射集合查询。',['转换程序','规则编排','不可变配置'],'协议转换规则执行'),
'ContractBackedGatewayRequest.java':('将已解析的协议契约请求封装为网关请求对象，向 HTTP 网关暴露原始载荷、模型、流式及工具推理需求。',['网关请求','协议契约','请求适配'],'契约解析结果的网关适配')
}
overrides={
'ClaudeMessagesToResponsesStreamingConverter.java:transform':'读取 Claude SSE 事件并驱动内容块状态机，将 Responses 事件写入客户端输出流并返回统一用量。',
'ClaudeMessagesToResponsesStreamingConverter.java:openBlock':'按 Claude 内容块类型分配输出条目，初始化文本、工具、思考或脱敏思考事件。',
'ClaudeMessagesToResponsesStreamingConverter.java:finalizeStream':'关闭尚未完成的内容块并组装最终 Responses 响应和用量，写出流式完成事件。',
'ClaudeMessagesUsageExtractor.java:extractUsageNode':'校验 Claude 用量节点，优先使用顶层权威统计并在需要时汇总迭代与缓存令牌。',
'ConversationEffortBridge.java:resolveClaudeRequest':'遍历消息历史中的会话配置更新，仅在新增用户输入后激活待处理的推理强度，避免在工具循环中切换。',
'ConversationEffortBridge.java:resolveResponsesReasoning':'读取 Responses 会话配置中的推理强度并合并到当前请求的 reasoning 设置。',
'ClaudeThinkingStateBridge.java:encode':'校验 Claude 思考块类型，将签名或脱敏内容序列化为带版本前缀的桥接状态。',
'ClaudeThinkingStateBridge.java:decode':'识别并解码桥接状态，恢复原生 Claude 思考块并拒绝无效载荷。',
'ResponsesReasoningBridge.java:encode':'将带标识及加密内容的 Responses 推理条目编码为 Claude 可携带的桥接签名。',
'ResponsesReasoningBridge.java:decode':'解码带桥接前缀的 Claude 签名，恢复 Responses 不透明推理状态。',
'GenericProtocolMessageConverter.java:normalizeChatToolHistory':'对 Chat 工具调用历史进行规范化，使助手工具调用与用户结果消息形成可接受的顺序。',
'GenericProtocolMessageConverter.java:claudeRequestToResponses':'将 Claude 请求映射为 Responses 结构，协调工具、推理、缓存控制、上下文管理及多模态消息转换。',
'GenericProtocolMessageConverter.java:responsesResponseToChat':'将 Responses 输出条目聚合为 Chat 助手消息，映射工具调用、推理、结束原因与用量。',
'ResponsesToClaudeRequestConverter.java:normalizeToolPairing':'收集工具调用结果并重排消息，保证 Claude 工具调用与结果相邻且调用标识可配对。',
'ResponsesToClaudeRequestConverter.java:convert':'把 Responses 结构化请求直接映射为 Claude Messages，并应用工具、推理、输出格式和元数据配置。',
'StreamingPassthroughUsageExtractor.java:transferAndExtract':'按上游协议选择事件识别策略，在透传响应字节的同时提取最终统一令牌用量。',
'UnifiedStreamingConversionAdapter.java:transform':'根据上游协议与客户端协议分派流式转换路径，传递模型上下文并返回转换后的统一用量。',
'UnifiedStreamingConversionAdapter.java:handleResponsesSseEvent':'按 Responses 事件类型更新文本、推理、工具和响应完成状态，输出对应 Claude SSE 事件。',
'UnifiedStreamingConversionAdapter.java:transformBedrockInvokeModelToClaude':'读取 Bedrock InvokeModel 事件流帧，解析消息载荷并转写为 Claude SSE，同时汇总用量。',
'UnifiedStreamingConversionAdapter.java:handleClaudeEventForChat':'将 Claude 消息和内容块事件转换为 Chat 增量片段，维护工具参数、推理、结束原因及用量。',
'SseEventTransformer.java:transform':'保留 SSE 事件及空行分隔，对非终止 data 行调用指定转换函数并重建事件文本。',
'ProtocolConversionProgram.java:singleRule':'将 JSON 转换操作与字段映射说明封装为单条可执行规则，构造对应协议转换程序。',
'ProtocolConversionProgram.java:execute':'依次执行转换规则，以源 JSON、目标 JSON 和转换需求构造规则上下文并返回结果。',
'ProtocolConversionProgramRegistry.java:findProgram':'依据源协议、目标协议及请求响应方向查找已注册的可执行转换程序。',
'DefaultProtocolConversionServiceAdapter.java:convertRequest':'查找满足转换需求的请求转换器；支持同协议兼容转换，并在需要时回退到领域转换流程。',
'DefaultProtocolConversionServiceAdapter.java:convertResponse':'按原始客户端协议选择响应转换器，并在同协议场景中保留载荷与统一用量。',
'OpenAIResponsesRequestConverter.java:normalizeInputItem':'移除客户端历史条目的 author，并将助手历史中的输入文本规范化为输出文本。'
}
m.WORDS.update({'inline':'内联','nonnegative':'非负','tooluse':'工具调用','interprete':'解释','interpreter':'解释器','author':'作者','read':'读取','write':'写入','creation':'创建','cachewrite':'缓存写入','passed':'已传递','failed':'失败','failure':'失败','chunks':'分片','chunk':'分片','for':'面向','by':'按','passthrough':'透传','redactedthinking':'脱敏思考','no':'无','noop':'空操作','clear':'清理','edit':'编辑','edits':'编辑列表','billing':'计费','header':'标头','anthropic':'Anthropic','legacy':'旧版','separated':'带分隔的','joined':'拼接的','present':'存在的','allowedcallers':'允许的调用方'})
m.WORDS.update({'openai':'OpenAI','chatcompletions':'Chat Completions','chunk':'分片','decode':'解码','encode':'编码','else':'其他','matching':'匹配','linkage':'关联','end':'结束'})
for method,protocol in [('tryExtractClaudeUsage','Claude'),('tryExtractOpenAIResponsesUsage','Responses'),('tryExtractOpenAIImagesUsage','Images'),('tryExtractOpenAIChatCompletionsUsage','Chat Completions')]:
 overrides['StreamingPassthroughUsageExtractor.java:'+method]='尝试从 '+protocol+' 流式事件数据解析统一用量；事件未携带有效用量时保留已有统计。'
m.WORDS.update({'blocks':'内容块列表','started':'启动状态','update':'更新','url':'URL','top':'顶层','level':'层级','context':'上下文','management':'管理','definition':'定义','if':'条件','failed':'失败','created':'创建状态','companion':'伴随','calling':'调用'})
for file,cfg in meta.items():
 if 'UsageExtractor' in file:overrides[file+':extract']=cfg[0]
overrides.update({
'ProtocolMessageConverter.java:convert':'依据转换需求将输入载荷转换为目标协议的请求或响应文本。',
'OpenAIResponsesRequestConverter.java:convert':'解析 Responses 请求 JSON，规范化客户端历史消息及缓存断点后重新序列化。',
'ProtocolContractRegistry.java:require':'依据协议类型查找已注册的可执行契约，缺失时抛出包含协议标识的异常。',
'GenericProtocolMessageConverter.java:responsesContextManagement':'将 Claude 上下文编辑转换为 Responses 压缩配置，忽略清除思考和工具的编辑，并拒绝不支持或有损的压缩选项。'
})
m.run(8,meta,overrides)
