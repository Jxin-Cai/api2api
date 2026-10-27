import json, pathlib, math
ROOT=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api')
UA=ROOT/'.ua'
b=json.loads((UA/'tmp/batch-task-3.json').read_text())
d=json.loads((UA/'tmp/ua-file-extract-results-3.json').read_text())
assert d['scriptCompleted'] and not d['filesUnreadable']
# 所有语义来自提取结果与选择性阅读；不重新提取源码结构。
file_info=[
('定义用户角色可访问的业务范围，区分个人门户与管理后台。',['权限','领域模型','访问范围']),
('实现上游模型列表拉取端口，使用默认 models 路径、鉴权请求和有限 I/O 重试；兼容多种列表格式及 Bedrock 模型摘要，生成渠道模型支持项。',['上游适配','模型发现','HTTP','错误处理']),
('实现渠道领域仓储，将聚合与持久化对象互转并委托 Mapper 完成保存、查询、模型限流恢复和软删除。',['仓储','渠道管理','持久化']),
('通过 MapStruct 与接口默认方法转换渠道聚合和持久化对象，恢复协议映射、模型状态、限流时间及值对象。',['MapStruct','持久化','对象转换']),
('使用命名参数 JDBC 读写渠道及其协议映射和模型子表，在事务中替换子记录，并支持模型限流隔离、恢复与渠道软删除。',['JDBC','渠道管理','事务','限流']),
('定义渠道持久化 Mapper 契约，覆盖增改查、可路由渠道筛选、模型限流标记和恢复及软删除。',['持久化','接口','渠道管理']),
('承载渠道模型支持项的数据库字段，包括请求别名、上游模型与协议、优先级、来源、状态及限流时间。',['持久化对象','模型映射','限流']),
('承载渠道请求协议到上游协议的映射及创建更新时间，用于协议映射子表读写。',['持久化对象','协议映射','渠道管理']),
('承载渠道主表字段及协议映射、模型支持子集合，包含状态、软删除标记和时间戳。',['持久化对象','渠道管理','聚合存储']),
('转换协议转换定义与数据库记录，并从可执行程序注册表补充字段映射文档；未注册跨协议程序时显式生成不可用映射说明。',['对象转换','协议转换','持久化','可观测性']),
('提供渠道管理 HTTP 接口，将管理员请求转换为应用命令，覆盖渠道启停、模型预览拉取、模型批量维护和限流重置。',['HTTP','管理接口','渠道管理','命令转换']),
('将渠道 HTTP DTO 转换为领域值对象和应用命令，并组装响应；处理协议归一化、兼容字段、可选覆盖参数和密钥掩码。',['MapStruct','HTTP','对象转换','密钥掩码']),
('定义批量维护渠道模型的单项请求，携带可选标识、请求与上游模型、协议、优先级、首选标记和来源，并声明字段校验。',['DTO','请求校验','模型管理']),
('定义渠道模型批量维护请求，包含是否替换已有模型的标记与经过级联校验的非空模型列表。',['DTO','批量操作','请求校验']),
('定义新建渠道请求，要求名称、上游地址、密钥和支持协议，并允许指定模型路径、路由优先级及协议映射。',['DTO','渠道创建','请求校验']),
('定义已保存渠道的模型预览请求，可选覆盖地址、密钥、模型路径和协议配置，并要求有效的默认优先级。',['DTO','模型预览','参数覆盖']),
('定义未保存渠道的上游模型预览请求，校验地址、密钥和默认优先级，并接收上游协议及兼容协议配置。',['DTO','模型预览','请求校验']),
('定义渠道模型拉取请求，要求默认优先级不小于一。',['DTO','模型发现','请求校验']),
('定义渠道更新请求，校验名称、地址和支持协议；密钥可省略以保留原值，并允许配置模型路径和协议映射。',['DTO','渠道更新','请求校验']),
('定义单项渠道模型新增或更新请求，校验模型名称、上游协议、来源及正整数优先级，并接收首选标记。',['DTO','模型管理','请求校验']),
('定义渠道模型支持项响应，返回请求别名、上游模型和协议、优先级、状态、来源及毫秒级限流和审计时间。',['DTO','响应模型','限流']),
('定义协议映射请求，要求请求协议与上游协议均为非空字符串。',['DTO','协议映射','请求校验']),
('定义协议映射响应，返回请求协议与实际调用的上游协议。',['DTO','协议映射','响应模型']),
('封装渠道列表响应，将渠道详情集合纳入统一响应结构。',['DTO','渠道列表','响应模型']),
('定义渠道详情响应，返回连接配置、掩码密钥信息、协议映射、模型支持列表、状态和审计时间。',['DTO','渠道详情','响应模型','密钥掩码']),
('定义上游模型列表预览响应，封装尚未持久化的模型支持项集合。',['DTO','模型预览','响应模型']),
('使用 Java record 返回渠道模型限流重置操作恢复的记录数量。',['DTO','限流恢复','响应模型']),
('验证用户查询渠道配置模型选项时的读取与权限行为，以及管理员批量恢复模型限流时的仓储委托。',['测试','渠道管理','权限验证']),
('验证模型可用性查询保持只读、幂等，并随渠道启停、模型状态、协议映射及限流到期动态变化，且与路由判断一致。',['测试','模型可用性','限流','路由']),
('验证渠道评测默认选择已启用模型、拒绝未启用模型，并在探测返回分数后完成运行中评测。',['测试','渠道评测','状态转换']),
('验证路由策略对协议映射、原生协议限制、转换能力、候选重试及单模型限流隔离的处理。',['测试','路由策略','协议转换','故障转移']),
('通过本地 HTTP 服务验证网关适配器的原始响应、流式调用、端点选择、查询参数及 multipart 转发，并覆盖流式错误不重发与 Retry-After 传递。',['测试','HTTP','流式传输','网关']),
('通过本地 HTTP 服务验证模型发现的鉴权、列表格式兼容、默认路径选择和上游认证错误详情。',['测试','模型发现','HTTP','鉴权']),
('验证协议转换服务的同协议透传及请求规范化行为，覆盖 Responses 作者字段、助手文本和从 Messages 重放的推理内容。',['测试','协议转换','请求规范化'])
]
function_info={
1:{'ProviderModelFetchAdapter':'校验依赖并创建配置连接超时的 HTTP/1.1 客户端。','fetchModels':'校验渠道参数，通过默认 models 路径发送带鉴权的 GET 请求，解析模型并将超时、I/O、中断及非成功响应转换为业务错误。','sendWithRetry':'按配置次数重试模型列表请求的 I/O 失败，遇到超时立即终止。','providerModelsHttpException':'按认证失败、路径不存在和其他 HTTP 状态生成包含上游原因的业务异常。','extractUpstreamError':'从 JSON 错误消息字段中提取原因，非 JSON 时保留截断后的正文。','truncateError':'合并空白并将上游错误文本截断到 240 个字符。','toModelSupports':'识别普通模型列表或 Bedrock 摘要，去重普通模型标识并排序构建的支持项。','modelEntries':'兼容根数组以及 data、models 数组，并合并同时存在的两组条目。','modelId':'从文本或 id、slug、name、model 字段提取并规范化模型标识。','toBedrockModelSupports':'从 Bedrock 摘要提取模型，排除明确不支持 ON_DEMAND 的条目。','buildModelSupports':'为每个模型与去重后的上游协议组合创建 FETCHED 来源支持项，使用时钟生成标识并拒绝空列表。'},
2:{'save':'将渠道聚合转为持久化对象，根据标识查询结果选择插入或更新。','findById':'按渠道标识加载记录，并将可空对象转换为领域聚合 Optional。','findAll':'读取全部渠道记录并转换为领域聚合集合。','findEnabledForRouting':'读取启用的可路由渠道并恢复领域聚合。','markModelRateLimited':'校验限流参数，将指定渠道上游模型的限流时间与恢复时间委托给 Mapper。','restoreModelRateLimitsBefore':'校验当前时间与更新时间，恢复指定时间前已到期的模型限流。','restoreAllModelRateLimits':'按指定时间恢复全部未删除渠道的模型限流。','softDeleteById':'校验标识与删除时间，委托 Mapper 软删除渠道。'},
4:{'insert':'在事务中插入渠道主记录，再写入协议映射和模型支持子记录。','update':'在事务中更新渠道配置，并整体替换协议映射和模型子记录。','selectById':'按标识读取未删除渠道，并加载协议与模型子集合。','selectAll':'按创建时间倒序查询未删除渠道并加载所有子记录。','selectEnabledForRouting':'读取未删除且启用的渠道，仅加载启用的模型支持记录。','markModelRateLimited':'按渠道与上游模型批量将启用的支持项标记为 RATE_LIMITED，并记录隔离与恢复时间。','restoreModelRateLimitsBefore':'将恢复时间已到期的限流记录置为启用，并清空限流时间。','restoreAllModelRateLimits':'恢复未删除渠道下全部限流模型并清空隔离字段。','softDeleteById':'将未删除渠道标记为已删除和禁用，并更新时间。','withChildren':'加载渠道协议映射和按优先级排序的模型记录，可选择仅返回启用模型。','replaceProtocolMappings':'先删除渠道旧协议映射，再逐条插入当前映射。','replaceModelSupports':'先删除渠道旧模型支持，再逐条写入当前模型配置与状态。','params':'将渠道对象字段与时间戳绑定到命名 SQL 参数。','modelParams':'将模型别名、协议、限流状态和时间字段绑定到命名 SQL 参数。'},
9:{'toPO':'将协议转换定义、能力标记和映射摘要投影为持久化记录。','toDomain':'从数据库记录恢复转换定义，并从程序注册表补充请求与响应字段映射。','buildMappingDocument':'优先采用程序描述的字段映射，否则按同协议透传或显式不可用状态构建文档。','unavailableMappings':'构建带 UNSUPPORTED 元数据的有损占位说明，表达转换程序尚未注册。','toCapability':'根据持久化能力标记组装文本、工具调用、推理、用量、缓存和流事件支持集合。'},
10:{'listChannels':'解析操作者身份，查询渠道列表并转换为统一 HTTP 响应。','resetAllRateLimits':'解析管理员身份，调用批量限流恢复并返回恢复记录数。','createChannel':'校验新建请求、分配渠道标识，将命令交给应用服务并返回渠道详情。','updateChannel':'将路径标识与更新请求组合为应用命令，返回更新后的渠道。','enableChannel':'构造渠道状态命令并启用指定渠道。','previewProviderModels':'将临时连接配置转换为预览命令，返回上游模型列表。','previewSavedProviderModels':'使用渠道标识和可选连接覆盖参数预览已保存渠道的模型。','disableChannel':'构造渠道状态命令并禁用指定渠道。','deleteChannel':'组合操作者和渠道标识执行渠道删除，返回空成功响应。','fetchProviderModels':'构建模型拉取命令，调用应用服务替换渠道模型配置。','upsertChannelModel':'将渠道、模型支持标识和单项请求转换为新增或更新命令。','upsertChannelModels':'将批量模型请求转换为命令，维护渠道模型集合并返回渠道详情。','enableChannelModel':'将渠道与模型支持标识转换为状态命令，启用指定模型。','disableChannelModel':'构造模型状态命令，禁用指定渠道模型支持项。','removeChannelModel':'按请求模型名与上游协议构造移除命令并返回渠道详情。'},
27:{'test_lists_all_configured_channel_models_when_user_queries_provider_model_options':'验证个人门户用户可查询全部已配置模型选项，并校验 USER_PORTAL 权限。','test_restores_all_rate_limits_when_operator_is_admin':'验证管理员重置限流时检查后台权限并返回仓储恢复数量。'},
29:{'test_submits_enabled_models_when_model_list_is_empty':'验证未指定模型时提交已启用模型，保存运行状态并传递探测任务标识。','test_rejects_submit_when_requested_model_is_not_enabled':'验证请求未启用模型时抛出业务错误且不提交探测、不保存评测。','test_marks_in_flight_run_succeeded_when_probe_reports_score':'验证探测返回分数后将运行中的评测更新为成功并保存结果。','service':'创建注入仓储、探测端口与固定时钟的评测应用服务测试实例。','runningEvaluation':'创建已提交且已标记运行的评测样本。','channelWithEnabledModel':'构造包含已启用模型与同协议映射的渠道领域样本。'},
32:{'test_sendsStoredKey_when_fetchingModels':'验证模型拉取将保存的渠道密钥放入 Bearer 鉴权头。','test_fetchesModels_when_openAiListUsesDataAndId':'验证兼容 data/id 格式的模型列表、GET 请求及上游协议填充。','test_fetchesNewModels_when_hostAlreadyContainsV1AndListUsesModelsAliases':'验证地址含 v1 时仍正确拉取 models 别名格式中的模型。','test_requestsDefaultModelsPath_when_customModelsPathProvided':'验证传入自定义模型路径后仍请求默认 /v1/models 并携带鉴权头。','test_includesFailureReason_when_upstreamReturnsUnauthorized':'验证上游 401 被转为认证失败业务错误并包含失败原因。','adapter':'创建允许本地 HTTP 地址并注入固定时钟的模型拉取测试适配器。'},
33:{'test_forwardsBodyUnchanged_when_clientAndUpstreamProtocolsMatch':'验证同协议请求转换保留无需规范化的原始正文。','test_marksResultAsPassthrough_when_clientAndUpstreamProtocolsMatch':'验证同协议转换结果标记为透传。','test_removesAuthor_when_responsesRequestPassesThrough':'验证 Responses 同协议请求规范化时移除输入项 author 字段。','test_normalizesAssistantText_when_responsesRequestUsesSameProtocolRoute':'验证 Responses 同协议路由将助手文本规范化为目标输入结构。','test_normalizesReplayedReasoning_when_messagesConvertToResponses':'验证 Messages 请求转 Responses 时规范化重放推理内容。','definition':'构建具有完整转换能力的测试用协议转换定义。'}
}
ordered_tests={
28:['验证重复请求模型可用性仅执行仓储读取。','验证显示已到期限流模型时不修改状态和时间戳。','验证重复切换渠道状态时模型可用性结果保持幂等。','验证模型状态变化后的列表结果与路由可用性一致。','验证多个渠道支持同一请求别名时只返回一次。','验证关闭一个渠道后其他启用渠道仍能提供模型。','验证最后一个支持渠道禁用后模型从列表移除。','验证渠道重新启用后模型重新可用。','验证重新启用渠道不覆盖模型显式禁用状态。','验证没有渠道时返回空模型集合。','验证启用渠道没有模型时返回空集合。','验证查询间删除渠道后立即移除模型。','验证查询间移除最后一个支持项后模型不可用。','验证模型上游协议不在渠道映射中时排除该模型。','验证非启用模型支持项不进入可用列表。','验证模型限流到期后在查询结果中恢复可用。','验证缺少限流恢复时间的模型继续不可用。'],
30:['验证请求协议有映射时采用配置的上游协议和模型。','验证原生协议限定过滤需要协议转换的候选。','验证模型注册在多种协议下时仅采用映射指定的协议。','验证映射协议无法服务模型时采用模型实际协议。','验证可重试失败后切换到剩余路由候选。','验证缺少请求映射时可从模型支持推导上游协议。','验证 Messages 流式完整转换需求选择 Chat Completions 候选。','验证转换能力不满足需求时不推导路由候选。','验证缺少转换定义时不推导跨协议候选。','验证一个模型被限流后同渠道其他模型仍可路由。'],
31:['验证普通转发保留上游状态码、响应头和正文。','验证普通转发保留上游错误响应。','验证流式调用返回状态码、响应头和可读取正文。','验证 Messages 流式路由到 Bedrock 时使用 InvokeModel 流端点。','验证存在协议回退可能时模型不存在错误标记为可重试。','验证流式请求遇到上游限流只发送一次。','验证同协议转发保留客户端查询字符串。','验证 CountTokens 操作采用专用计数端点。','验证图像编辑将文件和文本字段编码为 multipart 并发往 edits 端点。','验证跨协议调用丢弃客户端查询字符串。','验证上游服务错误不会导致流式请求重发。','验证流式限流异常保留可解析的 Retry-After 信息。']
}
helpers={
'claudeChannel':'构造配置 Messages 协议的渠道测试样本。','model':'构造指定别名、上游模型和协议的模型支持样本。','definition':'构建包含转换能力、映射文档和实现状态的协议转换测试定义。','mapping':'构建测试使用的字段映射文档。','recordingServer':'启动记录调用次数的本地 HTTP 测试服务。','capturingServer':'启动捕获请求 URI、内容类型和正文的本地 HTTP 服务。','serverThatFailsWith':'启动返回指定错误状态和错误正文的本地 HTTP 服务。','rateLimitedServerWithRetryAfter':'启动携带 Retry-After 响应头的限流测试服务。','server':'启动提供指定状态码和正文的本地 HTTP 服务。','serverThatRateLimitsOnce':'启动首次限流、随后成功并统计请求次数的测试服务。','bedrockStreamingServer':'启动模拟 Bedrock 流式响应并记录调用次数的本地服务。','adapter':'创建注入测试端点、转换器和 HTTP 配置的网关适配器。','channel':'构建包含协议映射和模型支持的渠道测试样本。','candidate':'构建选定渠道和协议转换路线的路由候选。','modelSupport':'构建用于网关路由测试的模型支持项。'}
for i,texts in ordered_tests.items():
 tests=[f for f in d['results'][i]['functions'] if f['name'].startswith('test_')]
 assert len(tests)==len(texts),(i,len(tests),len(texts))
 function_info[i]={f['name']:text for f,text in zip(tests,texts)}
 function_info[i].update({x['name']:helpers[x['name']] for x in d['results'][i]['functions'] if not x['name'].startswith('test_') and x['name'] in helpers})
nodes=[];edges=[];ids=set()
def edge(s,t,kind,w):
 if s!=t and not any(e['source']==s and e['target']==t and e['type']==kind for e in edges): edges.append(dict(source=s,target=t,type=kind,direction='forward',weight=w))
def add(n):
 assert n['id'] not in ids,n['id']; ids.add(n['id']);nodes.append(n)
for i,f in enumerate(d['results']):
 p=f['path'];fid='file:'+p;summary,tags=file_info[i]
 complexity='complex' if f['nonEmptyLines']>200 else 'moderate' if f['nonEmptyLines']>=50 else 'simple'
 add(dict(id=fid,type='file',name=pathlib.Path(p).name,filePath=p,summary=summary,tags=tags,complexity=complexity))
 exported={x['name'] for x in f.get('exports',[])}
 for c in f['classes']:
  if c['endLine']-c['startLine']+1<20 and len(c['methods'])<2 and c['name'] not in exported:continue
  cid='class:'+p+':'+c['name'];add(dict(id=cid,type='class',name=c['name'],filePath=p,lineRange=[c['startLine'],c['endLine']],summary=summary,tags=tags,complexity=complexity));edge(fid,cid,'contains',1.0)
  if c['name'] in exported:edge(fid,cid,'exports',.8)
 for fn in f.get('functions',[]):
  if fn['endLine']-fn['startLine']+1<10 and fn['name'] not in exported:continue
  nid='function:'+p+':'+fn['name']
  if nid in ids:continue
  text=function_info.get(i,{}).get(fn['name'])
  assert text, (p,fn['name'])
  ln=fn['endLine']-fn['startLine']+1
  ft=['测试','行为验证','回归测试'] if fn['name'].startswith('test_') else (['测试','样本构建','辅助方法'] if '/test/' in p else tags)
  add(dict(id=nid,type='function',name=fn['name'],filePath=p,lineRange=[fn['startLine'],fn['endLine']],summary=text,tags=ft,complexity='moderate' if ln>=40 else 'simple'))
  edge(fid,nid,'contains',1.0)
  if fn['name'] in exported:edge(fid,nid,'exports',.8)
 for t in b['batchImportData'][p]:
  # 输入数组完整保留，不做路径解析或聚合。
  edges.append(dict(source=fid,target='file:'+t,type='imports',direction='forward',weight=.7))
# 接口及端口实现关系：来自显式 implements。
for i,target in [(1,'backend/src/main/java/com/api2api/application/channel/ProviderModelFetchPort.java'),(2,'backend/src/main/java/com/api2api/domain/channel/repository/ProviderChannelRepository.java'),(4,d['results'][5]['path'])]:
 p=d['results'][i]['path'];edge('class:'+p+':'+pathlib.Path(p).stem,'class:'+target+':'+pathlib.Path(target).stem,'implements',.9)
# Controller 对应用服务的显式调用，目标符号由 neighborMap 核验。
p=d['results'][10]['path'];service='backend/src/main/java/com/api2api/application/channel/ProviderChannelApplicationService.java'
for fn in d['results'][10]['functions']:
 name=fn['name']; target={'previewSavedProviderModels':'previewProviderModels','fetchProviderModels':'fetchAndReplaceModels'}.get(name,name)
 assert any(n['path']==service and target in n['symbols'] for n in b['neighborMap'][p])
 edge('function:'+p+':'+name,'function:'+service+':'+target,'calls',.8)
# 明确的跨文件领域工厂与注册表调用。
for i,src,target,symbol in [
(1,'buildModelSupports','backend/src/main/java/com/api2api/domain/channel/model/ChannelModelSupport.java','create'),
(9,'toDomain','backend/src/main/java/com/api2api/domain/protocol/model/ProtocolConversionDefinition.java','rehydrate'),
(9,'toDomain','backend/src/main/java/com/api2api/infr/protocol/ProtocolConversionProgramRegistry.java','describeRequestMappings'),
(9,'toDomain','backend/src/main/java/com/api2api/infr/protocol/ProtocolConversionProgramRegistry.java','describeResponseMappings')]:
 assert any(n['path']==target and symbol in n['symbols'] for n in b['neighborMap'][d['results'][i]['path']]),(i,symbol)
 edge('function:'+d['results'][i]['path']+':'+src,'function:'+target+':'+symbol,'calls',.8)
# 同包生产类在 Java 中不需要 import；源码显式创建并调用对应类。
for i,target in [(27,service),(28,'backend/src/main/java/com/api2api/application/channel/ProviderModelAvailabilityService.java'),(29,'backend/src/main/java/com/api2api/application/evaluation/ChannelEvaluationApplicationService.java'),(30,'backend/src/main/java/com/api2api/domain/routing/service/DefaultRoutingPolicyService.java'),(31,'backend/src/main/java/com/api2api/infr/client/provider/ProviderGatewayCallAdapter.java'),(32,d['results'][1]['path']),(33,'backend/src/main/java/com/api2api/infr/protocol/DefaultProtocolConversionServiceAdapter.java')]:
 assert (ROOT/target).is_file()
 production='file:'+target; test='file:'+d['results'][i]['path']
 # 跨批生产节点不属于本片源节点，使用协议允许的反向测试关系交由合并器规范化。
 edge(production if production in ids else test,test if production in ids else production,'tested_by',.5)
assert sum(e['type']=='imports' for e in edges)==sum(len(b['batchImportData'][f['path']]) for f in b['files'])
assert len([n for n in nodes if n['type']=='file'])==len(b['files'])
# 按原始批次独立分片；所有跨片引用在本批节点全集中验证。
parts=math.ceil(max(len(nodes)/60,len(edges)/120));paths=sorted(f['path'] for f in b['files']);size=math.ceil(len(paths)/parts)
allowed_files=set(paths)
for a in b['batchImportData'].values():allowed_files.update(a)
allowed_symbols=set()
for a in b['neighborMap'].values():
 for n in a:
  allowed_files.add(n['path'])
  for s in n['symbols']:
   allowed_symbols.update(['function:'+n['path']+':'+s,'class:'+n['path']+':'+s])
# tested_by 的同包生产节点已由磁盘检查及源码使用确认。
allowed_files.update(e[end][5:] for e in edges if e['type']=='tested_by' for end in ['source','target'])
for e in edges:
 for end in ['source','target']:
  ref=e[end]
  assert ref in ids or ref in allowed_symbols or (ref.startswith('file:') and ref[5:] in allowed_files),e
out=UA/'intermediate';out.mkdir(exist_ok=True)
written=[]
for k,start in enumerate(range(0,len(paths),size),1):
 group=set(paths[start:start+size]); ns=[n for n in nodes if n['filePath'] in group]; local={n['id'] for n in ns}
 es=[e for e in edges if e['source'] in local]
 # 测试关系源可能在其他批次，随目标测试所属片保存。
 es.extend(e for e in edges if e['source'] not in ids and e['target'] in local)
 p=out/(f'batch-3-part-{k}.json' if parts>1 else 'batch-3.json');p.write_text(json.dumps(dict(nodes=ns,edges=es),ensure_ascii=False,indent=2)+'\n');json.loads(p.read_text());written.append(str(p))
assert sum(len(json.loads(pathlib.Path(p).read_text())['edges']) for p in written)==len(edges)
print(json.dumps(dict(batch=3,files=len(paths),nodes=len(nodes),edges=len(edges),parts=len(written),paths=written,imports=sum(e['type']=='imports' for e in edges),skipped=d['filesSkipped']),ensure_ascii=False))
