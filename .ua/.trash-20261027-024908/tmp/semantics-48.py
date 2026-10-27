from importlib.machinery import SourceFileLoader
import pathlib
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={}
def sp(n,t,g):s[n]=(t,g.split())
sp('ProtocolResponseUsageMapper.java','以统一用量模型为中介，映射三种协议的输入、输出、总量及缓存读写计量字段。','协议转换 用量映射 缓存计量')
sp('ProtocolSupportConfiguration.java','注册共享 JSON 协议处理组件，使各转换器复用应用的对象映射器。','依赖注入 协议处理 配置')
sp('ResponsesProtocolConstants.java','集中定义 Responses 压缩与不透明状态的占位文本及压缩类型识别规则。','协议常量 上下文压缩 类型识别')
sp('ResponsesSseEmitter.java','封装 Responses SSE 的事件序号、写出刷新、基础响应、用量及输出文本汇总。','流式响应 事件发送 用量映射')
sp('ProtocolShapeKind.java','枚举协议结构定义的请求、响应与流式事件类型。','协议结构 类型枚举 契约')
sp('InFlightConcurrencyCalculator.java','将请求起止时刻展开为增减事件，计算每个半开时间桶内的最高并发数。','并发统计 时间聚合 扫描算法')
sp('TimeBucket.java','定义左闭右开的时间桶并按指定正时长拆分窗口，支持截止时刻过滤和末桶裁剪。','时间桶 时间窗口 聚合')
sp('JdbcTimestampSupport.java','在 JDBC 时间戳与领域 Instant 之间执行可空转换。','持久化 时间转换 工具函数')
sp('ApiKeyMaterialProtectionProperties.java','绑定密钥材料加密配置，解析历史轮换密钥并校验当前版本号为正。','密钥保护 配置绑定 密钥轮换')
sp('AuthenticationRequiredException.java','表示 HTTP 管理接口缺少已认证用户会话。','身份认证 接口异常 会话')
sp('ClientDisconnectDetector.java','遍历异常因果链和受抑制异常，识别下游客户端中断并避免将其误计为上游失败。','连接中断 异常分类 网关')
sp('GatewayModelListResponse.java','定义兼容模型列表响应，并对模型集合进行不可变复制。','模型列表 响应对象 不可变数据')
sp('GatewayModelResponse.java','定义网关公开的兼容模型描述，包含模型标识、创建时间和所有者字段。','模型描述 响应对象 协议兼容')
sp('GatewayProtocolErrorBodyBuilder.java','集中生成消息协议与补全协议兼容的错误 JSON，序列化失败时记录异常并生成固定安全响应。','错误响应 协议适配 序列化')
sp('GatewayProtocolRequest.java','声明网关协议请求适配契约，暴露正文、模型以及流式、工具和推理需求。','请求契约 协议适配 能力需求')
sp('GatewayRawResponse.java','封装供外部 SDK 使用的原始响应，校验 HTTP 状态并过滤透传头后生成框架响应。','原始响应 响应头过滤 协议适配')
sp('GatewayResponseHeaderFilter.java','统一过滤逐跳、长度和 Cookie 等响应头，并从上游头中提取内容类型。','响应头 安全过滤 内容类型')
sp('ResponsesImageBridgeFailure.java','保存图像桥接预期上游失败的原始响应，支持生成无效上游响应的网关错误。','图像桥接 失败传播 错误响应')
sp('ResponsesImageBridgePassthrough.java','表示规划器拒绝桥接请求并保留状态码，使调用方回退到普通 Responses 透传。','图像桥接 透传回退 控制信号')
sp('ResponsesImageResponseWriter.java','维护图像桥接 Responses 输出状态，写出消息、部分图像及完成事件，并汇总多个图像结果的用量。','图像桥接 事件状态 用量聚合')
sp('ResponsesImageSseAdapter.java','逐行解析图像上游 SSE，验证 JSON 与完成结果，将部分和最终图像交给 Responses 写出器。','图像桥接 流式解析 格式校验')
sp('application.yml','配置数据库和迁移、会话安全、上传大小、上游超时、协议开关、密钥保护及宿主机监控路径，并通过环境变量覆盖。','运行配置 环境变量 安全配置')
sp('claude-client-toolsets-20260801.json','定义客户端桌面与浏览器工具的参数模式、说明和启用状态，约定截图坐标及跨标签页操作语义。','工具定义 客户端执行 参数模式')
sp('UpstreamResponseMetadataTest.java','验证重试等待响应头的秒数及日期解析、最大等待裁剪，以及缺失或非法值处理。','测试 响应元数据 重试等待')
sp('ProtocolTypeTest.java','验证图像协议端点与别名解析、客户端可见性及默认生成路径。','测试 协议类型 图像接口')
d={
 'toChatUsage':'将统一令牌计量写为补全协议用量，并在存在缓存时补充缓存读写明细。','toResponsesUsage':'将统一令牌计量写为 Responses 用量，并仅在非零时附加缓存细分。','toClaudeUsage':'将统一用量写为消息协议输入输出和缓存读写令牌字段。','chatUsageFromClaude':'解析消息协议用量后生成补全协议用量。','responsesUsageFromClaude':'解析消息协议用量后生成 Responses 用量。','claudeUsageFromChat':'解析补全协议用量后生成消息协议用量。','responsesUsageFromChat':'解析补全协议用量后生成 Responses 用量。','chatUsageFromResponses':'解析 Responses 用量后生成补全协议用量。','claudeUsageFromResponses':'解析 Responses 用量后生成消息协议用量。',
 'writeEvent':'为事件补充单调序号，按 SSE 格式写出事件名和 JSON 并刷新输出。','baseResponse':'构造带标识、模型、状态和空输出列表的 Responses 响应骨架。','responsesUsage':'构建含输入输出、总量、缓存读取和缓存创建明细的用量节点。','collectOutputText':'遍历输出项内容，仅拼接非空的输出文本。','peakPerBucket':'为每个时间桶裁剪请求生存区间，排序并扫描开始结束事件得到桶内最高并发。','split':'按正时长顺序划分时间窗口，裁剪最后时间桶并过滤截止时间之后的桶。','timestamp':'将可空 Instant 转换为 JDBC 时间戳。','instant':'从结果集读取可空时间戳并转为 Instant。','previousEncryptionKeyList':'按逗号或分号分隔历史加密密钥，清理空白项后返回列表。',
 'isClientDisconnect':'建立按对象身份去重的集合，递归判断异常树是否包含下游断连。','walk':'遍历因果链、非受检 I/O 包装和受抑制异常，避免异常引用环并查找断连证据。','matches':'识别异步响应失效、容器客户端终止类及特定 I/O 断管或刷新失败消息。','buildClaudeErrorBody':'构建消息协议的嵌套错误对象，序列化失败时记录完整异常并返回固定错误 JSON。','buildOpenAIErrorBody':'构建补全协议的错误对象及空参数代码字段，序列化失败时返回固定错误 JSON。','toResponseEntity':'过滤禁止转发的头部，设置内容类型，并保持上游 HTTP 状态和原始正文。','copyHeaders':'清除无名或空列表响应头，对名称和值集合进行不可变复制。','body':'读取原始响应正文。','statusCode':'读取保留的上游 HTTP 状态码。','contentType':'读取原始响应内容类型。','headers':'读取不可变上游响应头。','extractContentType':'不区分大小写查找首个有效内容类型头，缺省时返回调用方给定类型。','invalidUpstream':'创建状态为 502 且具有无效上游响应代码的图像桥接异常。',
 'beginImage':'创建进行中的图像生成输出项，分配索引及标识并发送输出项添加事件。','partialImage':'在事件中附上图像序号与部分图像索引，发送 Base64 图像增量。','finishImage':'将图像输出项标记完成，发送图像完成及输出项完成事件并汇总用量。','appendItem':'追加并补全输出项标识，按消息或工具调用类型发送起始、内容及完成事件。','messageEvents':'为消息输出项逐一生成内容块添加、文本增量、文本完成和内容块完成事件。','addUsage':'接收桥接轮次用量并与已累计的响应计量合并。','sumUsage':'递归累加对象中的整型用量字段，保留嵌套明细结构。','ResponsesImageResponseWriter:finish':'创建带完整输出、聚合用量和拼接正文的完成响应并发送最终事件。','ResponsesImageSseAdapter:finish':'补派剩余事件，并在缺少有效最终图像或错误事件时拒绝不完整上游响应。','ResponsesImageSseAdapter:write':'以 UTF-8 逐行读取上游流并派发图像事件，最后校验流是否正常结束。','consumeLine':'按 SSE 空行边界派发事件，收集事件类型和多行数据字段。','dispatch':'解析当前事件 JSON，处理错误、部分图像和完成图像，拒绝无效事件或空图像结果。','fail':'在保留当前输出及可用用量的基础上生成失败响应并发送错误完成事件。',
 'test_returnsDelay_when_retryAfterIsDeltaSeconds':'验证秒数格式的重试等待头被转换为对应时长。','test_returnsRemainingDelay_when_retryAfterIsHttpDate':'验证 HTTP 日期格式的重试等待头按当前时间计算剩余时长。','test_clampsDelay_when_upstreamAdvertisesExcessiveRetryAfter':'验证超长重试等待被裁剪为一小时。','test_returnsEmpty_when_retryAfterIsAbsent':'验证缺少重试等待头时返回空值。','test_returnsEmpty_when_retryAfterIsUnparseable':'验证非法重试等待文本不会产生时长。','test_parsesOpenAIImages_when_externalValueIsEndpointPath':'验证图像生成接口路径解析为图像协议。','test_parsesOpenAIImages_when_externalValueUsesImagesAliases':'验证多个图像生成、编辑及变体别名映射到图像协议。','test_reportsClientFacing_when_protocolIsOpenAIImages':'验证图像协议允许作为客户端请求协议。','test_returnsImagesGenerationsPath_when_defaultEndpointPathIsRequested':'验证图像协议默认端点为图像生成路径。'
}
t,x=b.load(48);p={pathlib.Path(f['path']).name:f['path'] for f in t['files']};e=[('config:'+p['application.yml'],'class:'+p['ApiKeyMaterialProtectionProperties.java']+':ApiKeyMaterialProtectionProperties','configures')]
b.code(48,s,d,e)
