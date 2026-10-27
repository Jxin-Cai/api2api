import sys
sys.path.insert(0,'/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp')
from importlib.machinery import SourceFileLoader
b=SourceFileLoader('builder','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
specs={}
def spec(name,summary,tags):specs[name+'.java']=(summary,tags.split())
spec('DefaultProtocolConversionService','从已启用且满足能力要求的协议定义中选择转换路线，提供领域层的请求和响应转换默认实现。',['协议转换','领域服务','路线选择'] if False else '协议转换 领域服务 路线选择')
spec('ProtocolConversionService','定义协议路线解析与双向载荷转换契约，并以默认适配方法兼容转换需求和结果值对象。','协议转换 服务契约 领域接口')
spec('ExecutableProtocolContract','定义无框架依赖的协议契约执行端口，将原始请求解析为网关领域数据并生成协议响应。','协议契约 防腐层 领域接口')
spec('ParsedGatewayRequest','承载协议契约解析得到的原始请求、模型及流式、工具调用和推理能力需求。','请求解析 领域模型 不可变数据')
spec('FailoverAction','枚举故障切换可执行的继续尝试和停止动作。','故障切换 领域枚举 路由')
spec('FailoverDecision','封装故障切换动作、下一候选路线、累计失败及原因，并校验动作与候选路线的一致性。','故障切换 领域模型 决策')
spec('RouteAttempt','记录一次路由尝试的候选、序号、起止时间和失败结果，提供成功或失败后的状态转换。','路由 尝试记录 领域模型')
spec('RouteCandidate','封装渠道、请求与上游模型、协议、优先级及转换路线，确保候选与协议转换定义一致。','候选路线 领域模型 模型映射')
spec('RouteFailure','保存渠道失败类型、HTTP 状态、可重试性及原因，并支持使用失败类型的默认重试策略。','路由失败 错误诊断 领域模型')
spec('RouteFailureType','定义路由失败分类及默认重试属性，并根据异常消息或 HTTP 状态识别模型不可用等失败。','失败分类 重试策略 领域枚举')
spec('RoutePlan','保存路由请求、排序后的候选集合和生成时间，提供候选访问及可用性判断。','路由计划 领域模型 候选集合')
spec('RoutingRequest','封装客户端协议、请求模型与协议转换能力需求，作为路由策略输入。','路由请求 领域模型 能力需求')
spec('DefaultRoutingPolicyService','依据渠道启用状态、模型支持和协议转换能力生成路线，按偏好与优先级排序并轮询同级候选。失败后跳过已尝试路线，按可重试性决定切换或终止。','路由策略 负载均衡 故障切换')
spec('RoutingPolicyService','声明生成路由计划及依据尝试记录和最新失败选择后续动作的领域服务契约。','路由策略 领域接口 故障切换')
spec('UsageErrorDiagnostic','从调用错误和最后一次路由失败提炼错误类型、消息与上游 HTTP 状态，供用量记录持久化诊断。','用量记录 错误诊断 领域模型')
spec('BearerTokenProviderCallStrategy','使用 Bearer 凭据执行上游同步及流式 HTTP 请求，处理多部分表单、超时、状态分类和首包过载重试。','上游调用 身份认证 流式重试')
spec('ProviderCallStrategy','声明按协议选择的上游调用策略，区分普通转发和流式连接打开操作。','上游调用 策略接口 流式响应')
spec('ProviderGatewayCallAdapter','实现应用层上游调用端口，从已注册策略中按上游协议选择普通或流式转发实现。','端口适配 策略分派 上游调用')
spec('ProviderHttpClientProperties','集中绑定上游 HTTP 连接、读取与流超时、重试次数、协议路径及请求头过滤配置。','配置绑定 超时配置 上游路径')
spec('ResponsesStreamPreflight','在向客户端交付 Responses 流之前检查首个有效 SSE 事件，识别明确过载错误并保留已读取的正常流前缀。','流式预检 过载检测 事件解析')
spec('UpstreamFailureClassifier','将上游 HTTP 状态映射为路由失败类型，并将错误响应压缩为长度受限的诊断消息。','上游错误 失败分类 错误诊断')
spec('UpstreamHttpHeaderPolicy','根据协议构建认证与内容类型请求头，过滤禁止透传及受保护头，并处理客户端协议头转发。','请求头 策略 认证')
spec('UpstreamStreamOverloadedException','表示流式响应首包包含明确过载信息的上游异常，使连接建立阶段可执行限定重试。','上游异常 流式过载 重试信号')
spec('DomainServiceConfiguration','通过 Spring Bean 配置装配路由、网关调用和仪表盘分析领域服务。','依赖注入 领域服务 配置')
spec('AbstractProtocolMessageConverter','提供协议转换器的公共骨架，统一处理转换能力、JSON 与 SSE 分支、用量提取和转换程序缓存。','协议转换 转换骨架 流式处理')
spec('BedrockClaudeMessagesProtocolMessageConverter','在 Claude Messages 与 Bedrock 消息协议之间转换请求与响应，清理平台差异字段并合并兼容性所需的测试特性。','协议转换 平台兼容 请求规范化')
spec('BedrockClaudeMessagesRequestValidator','校验 Bedrock 消息请求的内容来源、工具模式及输入示例，并收集高级工具功能所需的测试特性。','请求校验 工具模式 平台兼容')
spec('BedrockWebSearchToolCompatibility','将托管网页搜索工具转换为 Bedrock 可接受的自定义工具，并把调用次数、域名和地理位置约束写入工具说明。','网页搜索 工具兼容 参数校验')
spec('ChatCompletionsToResponsesStreamingConverter','把 Chat Completions 的 SSE 增量转换为 Responses 事件序列，维护推理、消息及工具调用状态并汇总用量。','流式转换 事件状态机 用量映射')
spec('ClaudeClientToolBridge','将客户端执行的终端、文本编辑和记忆工具转换为通用自定义工具模式，同时保留工具执行语义。','客户端工具 工具桥接 模式转换')
spec('ClaudeConversationContextOptimizer','按上下文管理策略清理历史思考和工具结果，保护最新工具续接所需信息，并检测重复成功工具调用以阻止循环。','上下文管理 工具循环 思考保留')
spec('ClaudeDeferredToolBridge','根据消息中的工具引用和具名选择激活延迟工具，并在自定义与托管工具之间转换具名选择。','延迟工具 工具选择 协议兼容')
D={}
def desc(names,text):
 for n in names.split('|'):D[n]=text
for n,t in {
'resolve':'按源与目标协议、启用状态和能力要求筛选可用转换定义，缺少可用路线时抛出领域异常。',
'convertRequest':'解析请求转换路线，区分透传与转换结果并保留请求载荷。',
'convertResponse':'解析响应到客户端协议的转换路线，构造结果并附带当前可确定的用量信息。',
'normalizeReason':'规范化失败或决策原因文本，对空白原因采用领域默认值。',
'normalizeMessage':'规范化诊断消息文本，避免空白错误说明进入用量记录。',
'fromExceptionMessage':'检查异常消息中的模型不可用特征，推导对应的路由失败类型。',
'fromHttpStatus':'依据上游 HTTP 状态码确定路由失败类别及其默认重试语义。',
'buildRoutePlan':'遍历启用渠道，组合模型支持与协议转换路线，按偏好和优先级生成有序候选计划。',
'decideNext':'汇总失败记录，遇到不可重试错误时停止，否则选择尚未尝试的下一条候选路线。',
'appendCandidatesForChannel':'结合渠道的模型支持和可用协议转换路线，追加包含模型映射及双重优先级的候选。',
'candidateUpstreamProtocols':'优先采用能够服务目标模型的渠道协议映射，否则收集模型声明的上游协议。',
'resolveRoute':'查找已启用且满足当前流式、工具和推理需求的协议转换定义，返回可选转换路线。',
'prioritizeCandidates':'优先排列偏好候选及最高路由优先级候选，再依次加入低优先级和非偏好后备路线。',
'rotateForLoadBalance':'按请求协议、模型、偏好和优先级维护原子计数，轮换同级路线的起始位置。',
'evictOldestCounters':'当负载均衡计数表过大时遍历移除部分键，将表规模降至目标值。',
'collectFailures':'合并历史尝试中的失败与最新失败，去重后返回不可变列表。',
'collectAttemptedCandidates':'从历史尝试中提取已使用的候选，避免故障切换重复选择。',
'forward':'构建并发送上游 HTTP 请求，将响应状态、头和正文封装为应用层响应并分类传输错误。',
'openStream':'在连接建立阶段循环打开上游流，仅对符合条件的传输或明确过载错误执行退避重试。',
'openStreamOnce':'发送单次流式请求，检查状态与首包并封装可关闭的上游响应流。',
'shouldRetryStream':'结合错误类型、重试次数和配置判断流连接建立失败是否允许再次尝试。',
'prepareStreamingBody':'为上游响应流应用首字节、空闲超时及 Responses 过载预检。',
'waitBeforeStreamRetry':'根据重试退避和上游重试提示等待，遇到线程中断时恢复中断状态并终止重试。',
'buildRequest':'读取渠道端点和凭据，解析操作路径及请求头，为普通正文或多部分表单构造 HTTP 请求。',
'resolveUpstreamPath':'根据协议和操作选择上游路径，并替换 Bedrock 模型路径中的占位内容。',
'BearerTokenProviderCallStrategy:toStatusFailure':'把上游非成功状态及响应正文转换为具有状态、耗时和响应元数据的网关异常。',
'readErrorBody':'读取流式错误正文，读取失败时返回可用的诊断说明。',
'closeQuietly':'关闭上游输入流并对关闭阶段的预期 I/O 异常记录诊断信息。',
'ProviderGatewayCallAdapter:forward':'根据候选上游协议查找支持的调用策略，并委派普通请求转发。',
'ProviderGatewayCallAdapter:openStream':'根据候选上游协议选择调用策略，并委派流式响应打开操作。',
'defaultPathFor':'根据协议枚举返回默认上游端点路径。',
'upstreamPathFor':'校验操作是否适用于目标协议，再返回对应的消息、令牌计数或图像处理路径。',
'normalizeHeaderNames':'清理空白请求头名称并转换为小写集合，便于不区分大小写的过滤。',
'inspect':'读取并缓存 SSE 前导行，对首个有效事件执行过载检查，正常时将缓存与剩余流重新拼接。',
'classify':'解析 SSE 事件 JSON，区分前导、普通数据和明确上游过载错误。',
'isOverloaded':'检查错误的结构化代码与类型是否匹配过载集合，避免依据任意错误消息误判。',
'compactErrorMessage':'将上游错误正文合并为空白规范化且长度受限的 HTTP 诊断消息。',
'buildHeaders':'设置协议所需的认证、内容类型和流式协商头，再合并允许透传的客户端请求头。',
'addForwardedHeaders':'过滤禁止头、保留头及空值，按协议规则加入可以透传的客户端请求头。',
'convert':'校验载荷源协议与转换需求，分派 JSON 或 SSE 转换并生成统一用量结果。',
'conversionProgram':'延迟构造并缓存字段映射驱动的协议转换程序。',
'mapStopToFinishReason':'将消息协议停止原因映射为补全协议结束原因，并拒绝缺少停止原因的非法输入。',
'mapFinishToStopReason':'把补全协议结束原因映射为消息协议停止原因，未提供原因时使用默认结束语义。',
'firstTextFromClaudeContent':'从消息内容字符串或内容块数组提取文本并拼接。',
'claudeMessagesToOpenAiInput':'遍历消息并提取角色和文本，构建目标协议输入数组。',
'openAiMessagesToClaudeMessages':'把输入消息转换为消息协议格式，并处理角色及内容字段差异。',
'extractOpenAiContentText':'从字符串或多内容块数组中提取可用文本。',
'firstOutputText':'遍历响应输出项，汇总各内容数组中的文本。',
'convertRequestJson':'复制消息请求并清理模型、流式与不受支持字段，规范化工具并合并 Bedrock 测试特性。',
'mergeAnthropicBetaFeatures':'合并请求已有、入站协商及兼容性校验要求的测试特性，去重后写入目标请求。',
'addContextManagementBetaFeatures':'扫描上下文编辑策略，为思考清理、工具清理等能力加入所需测试特性。',
'validateContentValue':'仅遍历协议定义的内容数组，验证图像、文档及嵌套工具结果内容，避免误解工具输入 JSON。',
'validateBase64Source':'验证图像或文档内容的来源对象及 Base64 来源类型。',
'validateTools':'校验工具数组、类型、延迟加载和输入示例，拒绝不受支持组合并收集所需测试特性。',
'validateCustomToolSchema':'要求自定义工具输入模式为对象模式，并验证必要字段结构。',
'validateInputExamples':'校验工具输入示例的数组类型、数量上限及示例对象结构。',
'optionalBoolean':'读取可选布尔字段，对缺省值返回假并对非布尔值抛出协议校验异常。',
'normalizeRequest':'遍历请求工具数组，将匹配的托管网页搜索工具替换为兼容的自定义工具。',
'BedrockWebSearchToolCompatibility:toCustomTool':'验证网页搜索工具字段并把次数、域名和位置限制编码为自定义工具的描述及输入模式。',
'validateKnownFields':'检查网页搜索工具字段是否在受支持集合内，发现未知字段时拒绝转换。',
'validateAllowedCallers':'校验网页搜索允许调用者列表的类型及支持值。',
'appendDomainConstraint':'检查允许域名和禁止域名是否互斥，并将有效域名约束追加到工具说明。',
'domainList':'校验域名数组和每项文本，生成供工具说明使用的域名列表。',
'appendLocationConstraint':'校验用户地理位置对象及字段，把位置约束追加到工具描述。',
'ChatCompletionsToResponsesStreamingConverter:transform':'逐行解析上游 SSE，转换内容增量和结束信号，写出 Responses 事件并返回聚合用量。',
'handleChunk':'更新响应标识、模型及用量，分派推理、正文、拒绝和工具调用增量。',
'ensureCreated':'仅发送一次响应创建事件，初始化空输出和进行中状态。',
'emitReasoningDelta':'打开推理输出项，维护索引及标识并发送推理摘要增量。',
'closeReasoning':'补发推理摘要完成及输出项完成事件，将累计推理内容加入完整响应。',
'emitMessageDelta':'关闭前一推理项，按正文或拒绝类型切换消息内容并发送文本增量。',
'ensureMessage':'创建带索引和唯一标识的助手消息项，发送内容块起始事件。',
'closeMessage':'结束当前正文或拒绝内容，发送完成事件并保存完整消息输出项。',
'emitToolCallDeltas':'按工具索引累积调用标识、名称和参数，延迟宣告并逐步发送函数参数增量。',
'announceTool':'为缺失的工具调用标识生成值，宣告进行中的函数调用输出项。',
'closeTools':'按输出索引结束工具调用，补齐尚未宣告的工具项并保存参数及完整输出。',
'finalizeStream':'结束推理、消息和工具输出，附上用量并按完成原因发送最终 Responses 事件。',
'updateUsage':'从补全用量及缓存明细提取输入、输出和缓存令牌，更新转换状态。',
'writeOutputItemEvent':'组装带输出索引和对象标识的输出项事件，交由 SSE 写出逻辑发送。',
'messagePart':'按是否拒绝构造拒绝内容块或正文内容块。',
'toolItem':'由工具状态生成包含调用标识、名称、参数和状态的函数调用输出项。',
'ClaudeClientToolBridge:toCustomTool':'识别终端、文本编辑或记忆工具版本，生成保留其执行契约的自定义工具定义。',
'editorSchema':'根据编辑工具版本构造命令、路径、文本及行范围等参数的 JSON 模式。',
'description':'根据客户端工具类型及配置生成保留会话、编辑或记忆操作语义的工具说明。',
'retainThinkingForLatestToolContinuation':'定位最新工具结果续接所依赖的助手消息，仅保留这些受保护轮次的思考内容。',
'protectedAssistantIndexes':'在末尾为纯工具结果消息时定位需要保留思考的关联助手轮次。',
'containsOnlyToolResults':'检查内容数组是否非空且仅包含普通或托管工具结果。',
'optimize':'清理压缩历史，按上下文编辑指令及网关安全策略处理思考与工具内容。',
'protectAgainstRepeatedToolCalls':'检测成功工具调用的连续重复，达到提醒阈值时追加提示，超出安全阈值时终止循环。',
'hasRepeatedToolCallWarning':'扫描消息内容块，判断是否已插入重复工具调用警告。',
'sanitizeCompactionHistory':'启用压缩编辑时清理历史压缩相关内容，保留可以安全传递的消息。',
'hasCompactionEdit':'检查上下文管理编辑列表中是否存在压缩策略。',
'applyDefaultToolResultSafetyPolicy':'估算消息令牌量，超过阈值时应用默认历史工具结果清理策略。',
'clearThinking':'校验思考保留策略并按需删除受保护范围之外的历史思考块。',
'clearToolUses':'根据令牌或工具交互阈值、保留数及排除工具清理旧工具调用和结果。',
'collectToolInteractions':'按工具调用标识关联调用块与结果块，生成可清理的工具交互集合。',
'detectRepeatedSuccessfulToolCall':'关联成功工具调用和用户新指令，分别检查完全相同调用及相似意图的连续重复。',
'repeatedExactToolCallTail':'从最新成功调用向前统计工具名称与参数完全相同的连续重复次数。',
'repeatedToolIntentTail':'按助手工具意图统计连续重复的成功调用，忽略过短或无有效意图的情况。',
'assistantToolIntent':'拼接助手文本内容以提取工具调用前的意图描述。',
'appendRepeatedToolCallWarning':'在最近用户内容中追加防止重复成功工具调用的网关提示。',
'containsNewUserInstruction':'判断用户内容是否包含工具结果以外的新指令。',
'containsThinking':'判断消息内容数组中是否包含思考类型的块。',
'stripThinking':'移除消息的思考块，并在清理后维护合法的消息内容。',
'activateReferencedTools':'扫描消息工具引用及具名选择，激活被引用的延迟工具或命名空间。',
'namedChoice':'匹配已声明的自定义工具或托管工具，生成目标协议的具名工具选择。',
'hostedType':'按工具名称及原工具定义推导托管工具的目标类型。'
}.items():D[n]=t
D.update({'withDefaultRetryable':'根据失败类型的默认可重试属性构造路由失败记录。','toStatusFailure':'把上游 HTTP 状态和错误正文封装为带响应元数据的路由失败异常。'})
b.code(7,specs,D)
