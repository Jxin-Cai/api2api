from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={}
def spec(name,summary,tags):s[name]=(summary,tags.split())
spec('config.json','配置知识图谱分析时包含测试文件。','知识图谱 分析配置 测试覆盖')
spec('.env.example','提供数据库、初始管理员、端口及上游和流式超时等环境变量模板。','环境配置 部署参数 超时配置')
spec('pom.xml','定义 Java 21 与 Spring Boot 后端的 Maven 依赖、测试框架、编译和打包插件。','构建配置 依赖管理 后端')
spec('Api2ApiApplication.java','启动 Spring Boot 后端，扫描配置属性、启用定时任务并提供 UTC 时钟。','应用入口 依赖注入 定时任务')
spec('BootstrapAdminProperties.java','以配置属性记录承载初始化管理员的用户名与密码。','管理员初始化 配置绑定 数据记录')
spec('GatewayInvocationState.java','定义网关调用从创建、认证和路由到转换、转发及成功失败的生命周期状态。','网关调用 状态枚举 生命周期')
spec('MappingMetadata.java','以映射文档的别名值对象暴露方向、标题、说明和字段映射，并支持不可变追加。','协议转换 映射元数据 值对象')
spec('ProtocolConversionId.java','以正整数封装协议转换标识，提供领域定义标识互转和值相等语义。','协议转换 标识校验 值对象')
spec('ProtocolMetadataException.java','表示协议元数据处理失败的领域异常，支持保留原始异常原因。','协议元数据 领域异常 错误传播')
spec('EvaluationProbeProperties.java','绑定外部渠道评测服务的端点、超时、批次、轮询及调度参数，并验证地址和正数约束。','渠道评测 配置绑定 参数校验')
spec('ProbeIdentity.java','按信任顺序从多个身份估计器提取模型家族、具体模型和置信度，并保留报告中的家族不符标记。','渠道评测 身份识别 置信度')
spec('ProbeReportCompactor.java','压缩评测报告中的身份、签名和探针结果，截断原因与错误文本以限制持久化数据量。','渠道评测 报告压缩 数据持久化')
spec('ProbeVerdict.java','优先依据探针致命错误及加权探针得分判断通过或失败，证据不足时再解析报告及高置信度模型建议。','渠道评测 结论判定 结果映射')
spec('OutboundUriGuard.java','校验出站 URI 的协议与主机，阻止非 HTTP 地址和云实例元数据服务目标。','出站安全 地址校验 元数据保护')
spec('StreamingIdleTimeoutInputStream.java','包装上游输入流，区分首字节与空闲超时，超时后关闭底层流并将后续读取失败转换为超时异常。','流式读取 空闲超时 资源关闭')
spec('UpstreamRequestBody.java','封装上游请求的内容类型和字节数据，支持以 UTF-8 JSON 创建正文。','上游请求 正文封装 数据记录')
spec('UpstreamUrlResolver.java','校验渠道基础地址并拼接上游操作路径，必要时将本机主机名替换为容器可访问地址。','上游地址 路径拼接 容器兼容')
spec('HostResourceReader.java','从 Linux proc 文件读取宿主机 CPU 累计计数、物理内存和负载，并由相邻快照计算 CPU 使用率。读取独立于容器资源限制，缺失或非法计数会抛出异常。','主机监控 资源读取 系统指标')
spec('BedrockClaudeMessagesResponseSanitizer.java','移除 Bedrock 响应内指定的提供方扩展字段，避免扩展信息透传到兼容消息响应。','响应清理 平台兼容 字段过滤')
spec('BedrockClaudeToolType.java','枚举 Bedrock 可接受的消息工具版本及其所需测试特性，按线协议类型名解析工具。','工具类型 平台兼容 版本枚举')
spec('BedrockEventStreamReader.java','从二进制事件流中读取帧、校验长度并解码多类型头部及正文，供 Bedrock 流式响应解码使用。','事件流 二进制解码 帧解析')
spec('BedrockStructuredOutputCompatibility.java','把结构化输出约束转换为强制工具调用，规范化模式与历史内容，并解包兼容工具结果返回目标 JSON。','结构化输出 工具兼容 请求规范化')
spec('ClaudeMessageIds.java','为生成的消息创建带标准前缀的随机唯一标识，并保留已有的有效标识。','消息标识 唯一性 兼容工具')
spec('GptModelVersion.java','从模型名称解析主次版本并忽略变体后缀，用可比较版本控制协议功能开关。','模型版本 能力判断 版本比较')
spec('ProtocolConversionProperties.java','绑定推理模型名称识别规则和 Responses 显式缓存断点功能开关。','协议转换 配置绑定 能力开关')
d={
'main':'以 Spring Boot 启动网关后端应用。','clock':'提供使用 UTC 时区的系统时钟 Bean。','isTerminal':'判断网关调用是否已成功或失败。',
'MappingMetadata:append':'返回追加指定字段映射后的新元数据值对象，保留原实例不变。','toDocument':'返回当前元数据封装的映射文档。','direction':'读取映射方向。','title':'读取映射文档标题。','summary':'读取映射文档说明。','fieldMappings':'读取不可变字段映射列表。','toDefinitionId':'将兼容标识转换为领域转换定义标识。','value':'读取经过校验的正整数标识值。',
'runEndpoint':'拼接评测提交端点或指定运行标识的轮询端点。','historyEndpoint':'拼接指定运行的历史报告端点。','requireHttpUrl':'要求非空 HTTP 或 HTTPS 基础地址，并移除末尾斜杠。','requirePath':'要求非空且以斜杠开头的接口路径，并移除末尾斜杠。','requirePositive':'验证时长或批次等配置值严格为正。',
'ProbeIdentity:read':'按签名预测、估计器建议及声明模型的优先级提取实际模型和置信度，并保留家族不符标记。','readEstimator':'从估计报告读取声明模型与最高候选，兼容不同置信度字段及字段缺省情况。','confidence':'优先采用模型置信度，其次回退到家族置信度。',
'compact':'仅保留报告结论、身份、压缩后的签名和探针列表，避免持久化冗余大字段。','compactIdentity':'保留被测和实际模型及置信度等身份诊断字段。','compactSignature':'保留签名判定和关键预测结果，并截断诊断文本。','compactProbes':'逐项保留探针标识、状态和必要数值，按配置截断原因与错误说明。',
'verify':'要求 URI 使用 HTTP 或 HTTPS 并具有主机，阻止命中元数据服务的主机和地址。','isBlockedHost':'识别元数据主机名、链路本地地址及协议允许范围外的目标。',
'StreamingIdleTimeoutInputStream:read':'检查读取前超时状态并读取底层流，成功后更新空闲计时，失败时保留超时语义。','StreamingIdleTimeoutInputStream:close':'原子标记关闭，取消超时任务并关闭底层输入流。','afterRead':'在读取到数据后重新设置空闲计时，在流结束或关闭时清理超时任务。','expire':'仅在定时版本仍有效且流未关闭时标记超时，并关闭底层输入流以中断阻塞读取。',
'json':'将 JSON 文本编码为 UTF-8 字节并附上 JSON 内容类型。','content':'返回正文数据的防御性副本，避免调用方修改记录内部字节。','UpstreamUrlResolver:resolve':'验证上游地址，按配置替换本机访问主机，再拼接规范化的操作路径。',
'readCpu':'通过扩展操作系统管理接口读取系统和进程 CPU 使用率，并换算为百分比。','readMemory':'读取物理内存总量与空闲量，推导内存使用量及使用百分比。','readLoad':'读取系统平均负载及可用处理器数量，生成负载指标。',
'removeProviderExtensions':'在协议规定的响应位置移除提供方扩展字段，保留正常消息内容。','readEvent':'读取完整帧并验证长度边界，从帧中拆出头部和消息正文。','parseHeaders':'依次解析头部名称及类型化值，构造事件头映射。','readHeaderValue':'按类型标识解码布尔值、整数、字节、文本、时间戳及标识，拒绝未知类型。',
'normalizeRequest':'校验并移除结构化输出格式，把其模式改写为网关兼容工具并强制选择该工具。','isEnforcementRetry':'检查历史消息中是否存在结构化输出工具调用及对应的强制执行重试标记。','forceStructuredOutputTool':'设置结构化输出工具选择，移除冲突的思考与推理配置。','removeClearThinkingEdits':'移除与结构化输出续接不兼容的思考清理编辑，并在列表为空时清理上下文管理字段。','normalizeConversationHistory':'将历史兼容工具调用和结果改写为目标协议能够接受的对话内容。','unwrapResponse':'从兼容结构化输出工具调用中提取输入对象，将其序列化为普通响应文本。','normalizeInputSchema':'校验结构化模式对象并补全缺省对象类型，拒绝与对象约束冲突的类型。',
'parse':'以受限数字正则解析模型名称中的 GPT 主次版本，未匹配时返回空值。','compareTo':'先比较主版本再比较次版本，提供稳定的模型版本顺序。',
'create':'使用随机 UUID 生成带消息前缀的唯一标识。'
}
t,x=b.load(47);paths={__import__('pathlib').Path(f['path']).name:f['path'] for f in t['files']};e=[]
for src in ['pom.xml','.env.example']:e.append(('config:'+paths[src],'file:'+paths['Api2ApiApplication.java'],'configures'))
e.append(('config:'+paths['.env.example'],'class:'+paths['EvaluationProbeProperties.java']+':EvaluationProbeProperties','configures'))
d.update({'ProbeIdentity:read':'按冻结估计器、签名估计器、快照身份及附加估计器的信任顺序读取模型身份，单独保留家族不符标记。','readEstimator':'读取家族及模型第一候选和置信度，为模型特征数量不足或疑似同版本干扰的结果放弃模型精度，兼容旧身份字段。','confidence':'若有具体模型则采用模型置信度，否则使用家族置信度。','readCpu':'从 proc 统计文件解析 CPU 首行，累加各状态计数并把空闲与 I/O 等待合并为闲置计数。','readMemory':'从 proc 内存信息解析总量与可用量，缺少可用量时使用空闲、缓冲和缓存估算，返回字节指标。','readLoad':'从 proc 平均负载文件读取一分钟负载，并拒绝空文件。','isBlockedHost':'禁止完整回环网段、私有 IPv4、链路本地地址，以及 IPv6 回环、唯一本地和链路本地地址，保留指定本机例外。'})
s['OutboundUriGuard.java']=('校验出站 URI 的 HTTP 协议、凭据和主机，解析目标地址并阻止回环、私有、链路本地和无具体地址的目标；显式允许指定的本机开发地址。',['出站安全','地址校验','内网防护'])
s['OutboundUriGuard.java']=('校验出站 URI 的 HTTP 协议与主机，默认阻止回环、链路本地、任意本地、元数据服务和无法解析的目标；普通私有地址仍允许，配置开关可允许受限主机。',['出站安全','地址校验','本机防护'])
d.update({'verify':'要求 URI 使用 HTTP 或 HTTPS 且有主机，除非配置允许不安全主机，否则拒绝受限目标。','isBlockedHost':'检查 localhost 及其子域，并解析地址以拒绝回环、链路本地、任意本地和元数据目标；解析失败同样拒绝。','readMemory':'解析以 kB 为单位的总内存和可用内存并换算为字节，缺失或非法计数抛出 I/O 异常。','readLoad':'读取一、五及十五分钟负载，校验至少三个有限非负值。'})
b.code(47,s,d,e)
