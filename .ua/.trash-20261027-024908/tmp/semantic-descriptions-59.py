import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'RouteFailureType.java':('按 HTTP 状态与上游错误体区分授权失败、令牌限流、并发限流、容量过载、超时和渠道不可用，供故障转移与模型隔离选择不同策略。',['失败分类','路由策略','限流识别']),
'BearerTokenProviderCallStrategy.java':('实现 Bearer 凭据上游的同步与流式 HTTP 调用，按操作编码 JSON 或表单并透传安全头与查询参数。流式预检只对未输出前的显式容量拒绝或未建立连接的传输错误执行受限重试，避免重放已运行的生成。',['上游调用','流式预检','安全重试'])}
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('RouteFailureType.java',{'shouldFailoverToNextCandidate':'根据失败类别判断是否应切换到下一候选渠道。','fromHttpStatus':'将常见状态码映射为默认路由失败类别。','fromHttpResponse':'结合状态码和错误体，先识别显式限流、容量拒绝与并发限制，再按 HTTP 状态分类。','isTokenRateLimit':'识别令牌速率或预算类限制，供模型隔离策略区别处理。'})
add('BearerTokenProviderCallStrategy.java',{'supports':'作为默认 Bearer 调用策略接受所有协议类型，由更具体策略优先拦截特殊协议。','forward':'构建上游请求并同步读取响应，将超时、连接错误与中断转换为带重试语义的网关异常。','openStream':'在配置次数内重试可安全重放的流式连接或显式过载失败，其余异常直接向路由层传播。','openStreamOnce':'发送流式请求，失败状态读取错误体并保留响应头，成功时为输入流加上超时和协议预检。','shouldRetryStream':'仅允许无状态码连接失败或输出前明确过载重试，遵守次数限制且不绕过上游 Retry-After。','prepareStreamingBody':'为输入流施加首字节与空闲超时，仅对 Responses SSE 执行首事件预检；收到响应头后的读取失败标为不可重放并关闭流。','waitBeforeStreamRetry':'按尝试次数计算指数退避并记录重试，中断时恢复线程中断标志并终止调用。','buildRequest':'校验渠道及模型可用性，解析上游路径与安全地址，选择请求编码和受控请求头后构建 POST 请求。','encodeBody':'JSON 操作直接编码；多部分表单操作解码统一载荷并重新生成真实 multipart 请求体。','appendQuery':'将原始查询串原样追加到上游地址，保留已有查询参数。','resolveUpstreamPath':'校验协议操作可用性，为 Bedrock 按流式与模型替换路径模板，其余协议读取配置路径。','toStatusFailure':'按状态及错误体分类失败，授权错误不可重试，普通上游错误仅在模型不可用时可重试，并保留退避元数据。','readTimeout':'在流式首字节超时与同步读取超时之间选择适用配置。','withStreamingTimeout':'为上游输入流包装首字节及空闲读取超时控制。','readErrorBody':'读取上游失败响应体，I/O 失败时记录异常堆栈并返回空诊断内容。','closeQuietly':'关闭上游响应流，关闭失败时记录完整异常。','elapsedSince':'计算从请求开始到当前时刻的耗时毫秒数。'})
a='backend/src/main/java/com/api2api/infr/client/provider/'
# 同包 ProviderCallStrategy 未包含于此批导入/邻接表，暂不生成无法被输入上下文验证的实现边。
m.build(59,d,o)
