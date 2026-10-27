import runpy
x=runpy.run_path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-build-4.py');globals().update({k:v for k,v in x.items() if not k.startswith('__')})
old=file_info
file_info=[
('实现请求模型改写端口，为普通 JSON 请求替换根级 model 字段；Bedrock 使用 URI 选择模型，保持正文原样，并明确拒绝空正文或非对象 JSON。',['协议适配','模型映射','JSON']),old[1],old[2],
('将评测记录和 Cron 调度在领域模型与持久化对象之间互转，处理评分报告、模型名称列表 JSON、可空值及调度时区。',['对象转换','渠道评测','JSON','评测调度']),old[5],old[6],old[7],old[8],old[13],old[12],old[14],old[15],old[16],old[17],old[18],old[19],old[20],old[21],old[22],old[23],old[24],
('以 record 封装不可变排序记录集合，提供排序字段与方向解析、空排序判断及顺序追加。',['排序','不可变数据','参数解析']),
('验证评测领域模型接受探测任务后进入运行状态，并禁止成功结束后再次转为失败。',['测试','渠道评测','状态转换']),
('验证已从渠道删除的模型仍可用于构建历史用量记录，保留审计上下文。',['测试','用量记录','历史审计']),
('通过本地 HTTP 服务验证评测探测适配器的异步提交、历史结果回退和字段解析，并覆盖缺少任务标识及非法完成时间。',['测试','渠道评测','HTTP','兼容性']),
('验证 Spring Cron 适配器计算下次小时触发时间，并将非法表达式转换为业务错误。',['测试','Cron','评测调度']),
('验证 Messages 输出的消息标识生成与保留规则，覆盖流式多输出项、无标识输入及非流式转换的唯一性。',['测试','消息标识','协议转换']),
('验证 Messages 与 Responses 的客户端原生工具兼容性，覆盖命令契约、工具搜索与延迟加载、多模态结果、强制工具选择及流式参数。',['测试','协议转换','工具调用','延迟加载']),
old[28],old[29],old[30],old[31],old[32],old[33]]
assert len(file_info)==len(d['results'])
oldfn=function_info
function_info={0:{'rewriteModel':'校验协议、模型及正文，为普通 JSON 对象替换 model 字段；Bedrock 请求正文透传，JSON 错误转换为协议异常。'},
1:{'save':'转换评测记录并按标识是否已存在选择插入或更新。','findById':'按标识读取并恢复可选评测记录。','findByChannel':'按渠道与查询条件读取评测列表，单独统计总数并组装领域分页结果。'},
2:{'save':'转换调度并根据渠道对应调度是否存在选择更新或插入。','findByProviderChannel':'按渠道标识查询并恢复可选调度。','findDue':'读取指定时刻已到期的调度并恢复领域模型。','deleteByProviderChannel':'删除指定渠道的评测调度。'},
3:{'toPO':'将评测或调度领域对象转换为对应数据库对象，处理评分报告或模型列表序列化。','toDomain':'从评测或调度数据库对象恢复领域状态、值对象、时间和 JSON 内容。','writeJson':'将 JSON 节点序列化为文本，异常时保留原因并抛出转换错误。','readJson':'将评分报告文本解析为 JSON，空值返回空对象，格式错误明确失败。','writeModelNames':'将领域模型名称集合序列化为字符串数组 JSON。','readModelNames':'解析模型名称 JSON 并恢复模型名称值对象集合。'},
6:oldfn[7],7:oldfn[8],8:oldfn[13],9:{'isClientDisconnect':'遍历异常原因链，检查容器断连异常与连接重置等常见 I/O 消息。'},
10:{'submit':'解析操作者并将可空评测请求转换为提交命令，返回本次创建的评测。','list':'把渠道历史查询参数转换为命令并返回分页评测响应。','schedule':'读取指定渠道的当前调度并转换为响应。','updateSchedule':'校验调度请求并调用应用服务保存 Cron、时区和模型配置。'},
19:oldfn[23],20:{'parse':'解析单个或多个排序字符串，支持逗号合并以及分号分隔多个字段。','parseExpression':'按分号分割排序表达式并逐段追加排序记录。','appendRecord':'校验字段与方向令牌数量，解析方向并追加排序记录。'},
21:{'SortSpec':'将传入排序列表复制为不可变集合，空值转为空列表。','append':'保留原有排序顺序并返回追加新记录后的 SortSpec。'},
22:{'test_marks_running_when_probe_accepts_pending_run':'验证待处理评测接受探测后记录任务标识与开始时间并转为运行中。','test_rejects_terminal_transition_when_already_succeeded':'验证评测成功结束后禁止再标记失败。'},
23:{'test_creates_usage_record_when_channel_model_was_removed':'验证渠道模型已被移除时仍可创建保留历史上下文的用量记录。'},
24:{'test_submits_async_run_when_probe_service_returns_run_id':'验证异步提交携带明文密钥、模型和协议配置，并返回上游任务标识。','test_reads_completed_history_when_live_run_is_gone':'验证实时任务已消失时回退历史查询并恢复完成的评分结果。','test_fails_submit_when_probe_service_omits_run_id':'验证探测服务缺少任务标识时抛出明确业务错误。','test_omitsCompletionTime_when_probeTimestampIsInvalid':'验证无法解析的探测完成时间被省略而不影响结果读取。','startServer':'启动评测探测测试使用的本地 HTTP 服务。','adapter':'创建配置本地探测服务及密钥材料解析的评测适配器。','historyJson':'构造包含评测分数与探测项的历史响应 JSON。','baseUrl':'根据本地服务监听端口构建测试 URL。','channel':'创建带测试地址、密钥和模型配置的渠道样本。'},
25:{'test_computes_next_hour_when_expression_is_hourly':'验证整点小时 Cron 在给定 UTC 时间之后计算下一小时触发点。','test_rejects_expression_when_cron_is_not_parseable':'验证不可解析 Cron 表达式返回 EVALUATION_CRON_INVALID。'},
26:{'test_generatesDistinctMessageIds_when_convertingSeparateResponsesStreams':'验证多个独立 Responses 流转换后各自具有唯一 msg_ 标识。','test_emitsOneMessageIdentity_when_responsesStreamContainsMultipleOutputItems':'验证同一 Responses 流包含多个输出项时只发布一个消息身份。','test_generatesDistinctMessageIds_when_chatStreamsHaveNoUsableId':'验证缺少有效标识的独立 Chat 流各自生成唯一消息标识。','test_preservesOneUpstreamMessageId_when_chatStreamContainsMultipleChunks':'验证同一 Chat 流多个分片保留一个有效上游标识。','test_generatesDistinctMessageIds_when_nonStreamingResponsesHaveNoUsableId':'验证非流式响应缺少有效标识时每次生成唯一消息标识。','test_preservesUpstreamMessageId_when_nonStreamingResponseHasUsableId':'验证非流式响应提供有效上游标识时直接保留。','convert':'执行指定协议的流式转换并收集用于身份检查的输出。','messageIds':'从 SSE 消息开始事件提取消息标识集合。'},
31:oldfn[31],32:oldfn[32]}
# 实际 Mapper 名称与 SQL 参数帮助方法。
function_info[6].update({'selectByProviderChannel':'按渠道及可选模型和状态筛选分页记录，使用白名单列构建排序。','countByProviderChannel':'按列表相同的渠道、模型及状态条件统计评测数量。','filters':'为渠道标识及可选筛选条件组装 SQL WHERE 子句。','parameters':'绑定评测字段和时间值到命名 SQL 参数。','map':'从结果集读取评测标识、状态、报告和时间字段。'})
function_info[7].update({'parameters':'将调度配置与触发时间绑定为命名 SQL 参数。','map':'从结果集读取渠道调度配置并构建持久化对象。'})
# 第27批内文件的原生工具兼容性回归项。
test_texts[27]=['验证缺省提供模式的原生工具仍公开可执行命令结构。','验证 Bash 重置会话无需 command 且支持 restart 字段。','验证不同编辑器版本提供对应的撤销命令支持。','验证 memory 重命名工具保留旧路径与新路径契约。','验证编辑器描述保留最大字符数和视图范围限制。','验证严格模式无法表达命令变体时保留可选原生字段。','验证原生客户端工具保留程序化调用者支持。','验证搜索结果转换后保留正文和来源 URL。','验证浏览器工具结果保留标签页及状态变化信息。','验证失败工具结果即使正文类似成功也添加失败标记。','验证多模态失败结果同时保留图像和错误标记。','验证成功 JSON 工具结果保持原始文本以供程序解析。','验证发现工具引用后只激活被发现函数。','验证错误结果中的工具引用不会触发加载。','验证先前使用过的工具即使引用离开历史仍保持加载。','验证不支持工具搜索的目标立即加载函数与 MCP 工具。','验证强制选择延迟工具时激活该函数。','验证按名称选择托管工具时使用实际托管类型。','验证自定义 web_search 名称不被误识别为托管工具。','验证旧目标无法执行强制工具搜索时明确拒绝。','验证未确认契约的原生工具版本被拒绝。','验证非流式原生调用与结果在协议间正确往返。','验证流式分片参数恢复为可执行的原生工具调用。']
# 修正更短的测试函数与实际辅助函数名。
extra_helpers={'mcpRequest':'构造 MCP 服务器、工具限制和加载策略的请求样本。','stream':'将事件文本送入真实流式适配器并收集输出。','screenshotHistory':'构造包含屏幕截图工具调用的历史消息。','deferredComputerRequest':'构造启用延迟加载的计算机工具集请求。','convertResponse':'使用实际转换程序将测试响应转换为目标协议。','convertRequest':'使用实际转换程序转换测试请求。','dataEvents':'解析下游 SSE JSON 数据事件并过滤结束标记。','adapter':'创建配置实际程序注册表与用量收集器的测试流式适配器。','context':'构造源协议、目标协议和模型的流式上下文。','bedrockFrame':'将事件正文与头部编码为 Bedrock 二进制事件流测试帧。'}
for i,texts in test_texts.items():
 fs=[z for z in d['results'][i].get('functions',[]) if z['name'].startswith('test_')];assert len(fs)==len(texts),(i,len(fs),len(texts));function_info.setdefault(i,{}).update({z['name']:t for z,t in zip(fs,texts)})
for i,f in enumerate(d['results']):
 for z in f.get('functions',[]):
  if z['name'] in extra_helpers:function_info.setdefault(i,{}).setdefault(z['name'],extra_helpers[z['name']])
# 按实际路径校准持久化对象、HTTP 文件与回归样本。
file_info[8]=('保存渠道评测的数据库字段，包括模型、任务状态、评分、模型识别、探测计数、Token 用量、报告摘要及审计时间。',['持久化对象','渠道评测','数据模型'])
file_info[9]=('保存渠道评测调度的 Cron、时区、模型列表、启用标记以及上次和下次触发时间。',['持久化对象','评测调度','数据模型'])
file_info[10]=('承载渠道评测聚合统计查询结果，包括总数、已评分与失败数以及平均、最低和最高评分。',['持久化对象','评分统计','渠道评测'])
file_info[11]=old[13]
file_info[12]=old[14]
file_info[13]=('将评测 HTTP 请求转换为应用命令，并组装评测、评分统计及调度响应；解析 limit/offset、排序字段和状态，处理 Cron 时区的 UTC 缺省与错误。',['MapStruct','对象转换','评测调度','分页'])
file_info[14]=old[16]
file_info[15]=old[17]
file_info[16]=('封装评测历史记录、评分数量与均值极值汇总，以及 totalElements、limit 和 offset 分页字段。',['DTO','渠道评测','评分统计','分页'])
file_info[17]=old[19]
file_info[18]=old[20]
file_info[19]=old[21]
file_info[20]=('定义渠道评测历史查询参数，支持模型、状态、时间范围、排序及 limit/offset；校验 limit 为一至五百、offset 非负。',['DTO','渠道评测','查询参数','分页'])
file_info[21]=('将网关业务、协议及参数异常映射为 Messages 或 OpenAI 兼容错误体，区分认证、权限和限流状态；额度错误可携带 Retry-After，客户端断连时停止写入响应。',['异常处理','网关','协议适配','限流'])
function_info[11]=oldfn[13]
function_info[12]={'submit':'解析操作者身份，将可空模型请求转换为评测提交命令并返回创建结果。','list':'将历史筛选、排序与分页参数转换为命令并返回评测历史和评分汇总。','schedule':'读取指定渠道的评测调度并转换为可空响应。','updateSchedule':'将 Cron、时区、模型和启用配置转换为应用命令并保存调度。'}
function_info[13]={'toModelNames':'过滤空模型名称并转换为领域模型名称值对象。','toZoneId':'对空时区使用 UTC，对非空时区去除空白并解析，将 DateTimeException 转换为参数错误。'}
function_info[21]={'handleGatewayProtocol':'按协议异常的状态和类型生成对应协议错误体。','handleTokenQuota':'将额度异常映射为 HTTP 429，存在重置时间时生成最少一秒的 Retry-After。','handleBusiness':'将业务错误码映射为 HTTP 状态与协议错误类型并返回 JSON。','handleBadRequest':'根据参数异常消息选择状态与错误类型并生成协议错误体。','handleUnexpected':'识别客户端断连并停止响应，否则记录完整堆栈并返回通用 HTTP 500 错误。','protocolOf':'根据请求路径识别 Messages、Responses、Chat 或图像协议，缺省为 Chat。','mapBusinessCode':'将认证、权限、限额和内部计量错误码映射为 HTTP 状态及协议错误类型。','mapBadRequestMessage':'从参数错误消息中识别权限和限额业务码，其他情况使用请求错误。','normalizeCode':'去除错误码首尾空白及冒号后附加内容，并统一为大写。','buildErrorBody':'为 Messages 生成其原生错误对象，否则使用 OpenAI 兼容错误结构。','mapErrorTypeForOpenAI':'保留限流和服务器错误类型，其他错误统一映射为无效请求。'}
# 下方仅消费已提取结构，未执行任何临时结构提取。
supplement={
0:{'JsonGatewayPayloadModelMappingAdapter':'注入 Jackson ObjectMapper 以执行协议请求模型改写。'},
1:{'findHistory':'按渠道标识和历史查询条件恢复评测领域记录集合。','countHistory':'委托 Mapper 统计符合筛选条件的历史评测数量。','summarize':'将持久化评分统计转换为领域评测汇总。','findInFlight':'读取仍处于待处理或运行状态的评测记录并恢复领域对象。','deleteByProviderChannelId':'删除指定渠道关联的评测记录。'},
2:{'findById':'按调度标识查询并恢复可选领域调度。','findByProviderChannelId':'按渠道标识加载并恢复评测调度。','deleteByProviderChannelId':'删除指定渠道的评测调度记录。'},
6:{'selectHistory':'构建历史过滤、白名单排序及 limit/offset 分页 SQL 并查询评测记录。','countHistory':'使用与历史列表相同的过滤条件统计记录总量。','summarize':'查询评测总数、评分和失败数量及评分均值与极值。','selectInFlight':'按数量限制查询尚未结束的评测任务。','deleteByProviderChannelId':'删除指定渠道下的全部评测记录。','whereClause':'组装渠道、模型、状态及时间范围过滤条件，并使用命名参数绑定值。'},
11:{'handleBadRequest':'将非法参数、不可读请求体及不支持请求方法等异常统一转换为参数错误响应。'},
12:{'history':'校验历史查询参数并调用应用服务获取分页评测历史与汇总。','loadSchedule':'读取指定渠道的评测调度并转换为响应。','upsertSchedule':'构建 Cron 调度命令，调用应用服务保存并返回当前调度。'},
22:{'pending':'创建指定标识和时间的待处理渠道评测样本。'},
23:{'usageRecord':'创建包含路由、模型、Token 和计费字段的历史用量记录样本。'},
26:{'responsesStream':'构建指定响应标识与输出文本的 Responses SSE 样本。','streamMessageIds':'解析下游 SSE 并提取所有消息开始事件的标识。','responseConverter':'配置实际转换程序注册表以测试非流式响应消息标识。'},
30:{'writeFrame':'将事件头与正文写入 Bedrock 二进制帧，并写入帧长度及校验字段。','writeStringHeader':'按 Bedrock event-stream 格式编码字符串头字段。'}}
for i,m in supplement.items():function_info.setdefault(i,{}).update(m)
nodes=[];edges=[];ids=set();warnings=[]
def edge(s,t,kind,w):
 if s!=t and not any(e['source']==s and e['target']==t and e['type']==kind for e in edges):edges.append(dict(source=s,target=t,type=kind,direction='forward',weight=w))
def add(n):
 assert n['id'] not in ids,n['id'];ids.add(n['id']);nodes.append(n)
for i,f in enumerate(d['results']):
 p=f['path'];fid='file:'+p;summary,tags=file_info[i];complexity='complex' if f['nonEmptyLines']>200 else 'moderate' if f['nonEmptyLines']>=50 else 'simple'
 add(dict(id=fid,type='file',name=pathlib.Path(p).name,filePath=p,summary=summary,tags=tags,complexity=complexity));exports={z['name'] for z in f.get('exports',[])}
 for c in f.get('classes',[]):
  if c['endLine']-c['startLine']+1<20 and len(c['methods'])<2 and c['name'] not in exports:continue
  cid='class:'+p+':'+c['name'];cs=summary
  if c['name']=='Direction':cs='定义升序和降序方向，并校验外部排序方向字符串。'
  add(dict(id=cid,type='class',name=c['name'],filePath=p,lineRange=[c['startLine'],c['endLine']],summary=cs,tags=tags,complexity=complexity));edge(fid,cid,'contains',1.0)
  if c['name'] in exports:edge(fid,cid,'exports',.8)
 for z in f.get('functions',[]):
  ln=z['endLine']-z['startLine']+1
  if ln<10 and z['name'] not in exports:continue
  nid='function:'+p+':'+z['name']
  if nid in ids:continue
  text=function_info.get(i,{}).get(z['name']);assert text,(i,p,z['name'])
  ft=['测试','行为验证','回归测试'] if z['name'].startswith('test_') else ['测试','样本构建','辅助方法'] if '/test/' in p else tags
  add(dict(id=nid,type='function',name=z['name'],filePath=p,lineRange=[z['startLine'],z['endLine']],summary=text,tags=ft,complexity='complex' if ln>200 else 'moderate' if ln>=40 else 'simple'));edge(fid,nid,'contains',1.0)
  if z['name'] in exports:edge(fid,nid,'exports',.8)
 for t in b['batchImportData'][p]:edges.append(dict(source=fid,target='file:'+t,type='imports',direction='forward',weight=.7))
# 从该文件的邻居表匹配显式调用或实现，不猜测未公布的目标符号。
def neighbor_symbol(i,name,kind='function',suffix=None):
 options=[n for n in b['neighborMap'].get(d['results'][i]['path'],[]) if name in n['symbols'] and (suffix is None or n['path'].endswith(suffix))]
 return kind+':'+options[0]['path']+':'+name if len(options)==1 else None
for i,name in [(0,'GatewayPayloadModelMappingPort'),(1,'ChannelEvaluationRepository'),(2,'ChannelEvaluationScheduleRepository'),(6,'ChannelEvaluationMapper'),(7,'ChannelEvaluationScheduleMapper')]:
 target=neighbor_symbol(i,name,'class')
 # 同批接口由已生成节点直接定位。
 if not target:target=next((n['id'] for n in nodes if n['type']=='class' and n['name']==name),None)
 if target:edge('class:'+d['results'][i]['path']+':'+pathlib.Path(d['results'][i]['path']).stem,target,'implements',.9)
for i,src,method in [(12,'submit','submit'),(12,'history','history'),(12,'loadSchedule','loadSchedule'),(12,'upsertSchedule','upsertSchedule')]:
 target=neighbor_symbol(i,method,suffix='ChannelEvaluationApplicationService.java')
 if target:edge('function:'+d['results'][i]['path']+':'+src,target,'calls',.8)
for i in range(22,34):
 f=d['results'][i];stem=pathlib.Path(f['path']).stem
 exact=f['path'].replace('/src/test/','/src/main/').replace('Test.java','.java')
 targets=[]
 if (ROOT/exact).is_file():targets.append(exact)
 # 转换集成测试使用的真实适配器由已解析 imports 提供。
 if 26<=i<=30:
  targets.extend(t for t in b['batchImportData'][f['path']] if t.endswith(('DefaultProtocolConversionServiceAdapter.java','ProtocolConversionProgramRegistry.java','UnifiedStreamingAdapter.java')))
 for t in set(targets):
  production='file:'+t;test='file:'+f['path'];edge(production if production in ids else test,test if production in ids else production,'tested_by',.5)
assert sum(e['type']=='imports' for e in edges)==sum(len(b['batchImportData'][f['path']]) for f in b['files'])
known_files={f['path'] for f in b['files']}
for a in b['batchImportData'].values():known_files.update(a)
known_symbols=set()
for a in b['neighborMap'].values():
 for n in a:
  known_files.add(n['path'])
  for s in n['symbols']:known_symbols.update(['function:'+n['path']+':'+s,'class:'+n['path']+':'+s])
for e in edges:
 if e['type']=='tested_by':known_files.update(e[k][5:] for k in ['source','target'])
for e in edges:
 for k in ['source','target']:
  ref=e[k];assert ref in ids or ref in known_symbols or (ref.startswith('file:') and ref[5:] in known_files),e
parts=math.ceil(max(len(nodes)/60,len(edges)/120));paths=sorted(f['path'] for f in b['files']);size=math.ceil(len(paths)/parts);written=[]
for k,start in enumerate(range(0,len(paths),size),1):
 group=set(paths[start:start+size]);ns=[n for n in nodes if n['filePath'] in group];local={n['id'] for n in ns};es=[e for e in edges if e['source'] in local]
 p=UA/'intermediate'/(f'batch-{BATCH}-part-{k}.json' if parts>1 else f'batch-{BATCH}.json');p.write_text(json.dumps(dict(nodes=ns,edges=es),ensure_ascii=False,indent=2)+'\n');json.loads(p.read_text());written.append(str(p))
assert sum(len(json.loads(pathlib.Path(p).read_text())['edges']) for p in written)==len(edges)
print(json.dumps(dict(batch=BATCH,files=len(paths),nodes=len(nodes),edges=len(edges),imports=sum(e['type']=='imports' for e in edges),parts=len(written),paths=written,skipped=d['filesSkipped']),ensure_ascii=False))
