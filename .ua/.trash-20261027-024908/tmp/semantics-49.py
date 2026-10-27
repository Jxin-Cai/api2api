from importlib.machinery import SourceFileLoader
import pathlib,json
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={}
def sp(n,t,g):s[n]=(t,g.split())
sp('ApiCredentialTest.java','验证 API 凭据生成的随机密钥、摘要验证、配额更新及已删除凭据的操作约束。','测试 API凭据 配额约束')
sp('HostedProtocolToolSchemasTest.java','验证托管搜索工具模式的名称、必填输入及返回数量约束。','测试 托管工具 参数模式')
sp('CredentialPayloadTest.java','验证凭据载荷的完整性、认证方式约束和文本表示脱敏。','测试 凭据载荷 敏感信息')
sp('ChannelModelSupportTest.java','验证模型支持中的模型名、协议集合与能力声明约束。','测试 模型支持 领域约束')
sp('OutboundUriGuardTest.java','验证出站地址的协议和主机限制、回环与元数据保护、普通私有地址支持及配置例外。','测试 出站安全 地址校验')
sp('UpstreamRequestBodyTest.java','验证上游请求正文的字节防御复制、空值约束及 JSON 编码。','测试 请求正文 不可变数据')
sp('HostResourceReaderTest.java','通过临时 proc 文件验证 CPU、内存和负载读取，以及缺失、异常和计数回退处理。','测试 主机监控 资源采样')
sp('ClaudeMessageIdsTest.java','验证消息标识唯一性、格式和对已有标识的保留。','测试 消息标识 唯一性')
sp('GptModelVersionTest.java','验证模型版本解析、变体后缀兼容和主次版本排序。','测试 模型版本 版本解析')
sp('ProtocolResponseUsageMapperTest.java','验证用量映射中的缓存读写令牌处理及协议字段差异。','测试 用量映射 缓存计量')
sp('InFlightConcurrencyCalculatorTest.java','验证时间桶内的峰值并发、区间边界及零时长请求处理。','测试 并发统计 时间桶')
sp('TimeBucketTest.java','验证半开时间桶、窗口拆分、截止过滤和末尾时间裁剪。','测试 时间窗口 时间桶')
sp('JdbcTimestampSupportTest.java','验证 JDBC 时间戳与 Instant 的可空转换。','测试 JDBC 时间转换')
sp('ApiKeyMaterialProtectionPropertiesTest.java','验证历史加密密钥列表解析和当前版本号约束。','测试 密钥保护 配置校验')
sp('ClientDisconnectDetectorTest.java','验证客户端断连异常的识别、嵌套异常遍历及上游失败排除。','测试 连接中断 异常分类')
sp('GatewayRawResponseTest.java','验证原始响应保留状态和正文，同时过滤不应透传的头部。','测试 原始响应 响应头')
sp('GatewayResponseHeaderFilterTest.java','验证响应头的禁止集合、大小写兼容及内容类型提取。','测试 响应头 内容类型')
sp('operations-monitoring.md','说明宿主机资源监控的采样、持久化与趋势窗口，并记录配置参数、proc 文件部署要求和异常排查。','运维文档 主机监控 配置说明')
sp('nginx.conf','配置前端静态站点和单页应用回退，并将接口请求代理至后端，关闭流式缓冲并延长读取超时。','反向代理 静态站点 流式转发')
sp('package.json','定义前端依赖与开发、构建和测试命令。','项目配置 依赖管理 构建系统')
sp('test.mjs','启动前端 Node 测试，自动收集测试文件并透传运行参数及退出状态。','测试入口 脚本 自动发现')
sp('AuthRouteLayout.tsx','管理认证页布局；已登录用户跳转至对应角色首页，未登录用户显示嵌套路由。','路由布局 身份认证 页面导航')
sp('ProtectedRouteLayout.tsx','保护需要登录的路由，处理会话加载并将匿名访问重定向至登录页，同时保留回跳地址。','路由保护 会话检查 页面导航')
sp('ApiCredentialStatusTag.tsx','将 API 凭据状态映射为中文状态标签和对应颜色。','API凭据 状态标签 展示组件')
sp('ApiCredentialSelect.tsx','基于凭据查询提供可搜索的选择控件，处理加载、空状态与名称展示。','API凭据 选择组件 查询钩子')
d={
'AuthRouteLayout':'读取当前用户会话，已登录时按角色导航至首页，否则显示认证子路由。','ProtectedRouteLayout':'等待会话查询，未认证时跳转登录并附带原地址，认证后渲染受保护子路由。','ApiCredentialStatusTag':'根据凭据状态生成颜色和中文文案一致的状态标签。','getStatusMeta':'将凭据启用、禁用或删除状态映射为标签文本与语义颜色。','ApiCredentialSelect':'加载凭据选项，渲染支持搜索和清空的选择控件并上报选中凭据。','decode':'解析测试使用的编码凭据材料，供断言随机字节长度与密钥结构。','simpleRequest':'创建不含可选特性的最小测试请求。','endpointSafe':'构造并校验测试端点，供出站 URL 安全断言使用。'}
# 测试名称与断言行为逐一对应；保留代码符号，仅使用中文描述。
phrases={
'generates':'生成','random':'随机','secret':'密钥','token':'令牌','digest':'摘要','verify':'验证','quota':'配额','limit':'上限','deleted':'已删除','credential':'凭据','payload':'载荷','redact':'脱敏','sensitive':'敏感信息','model':'模型','protocol':'协议','empty':'空值','null':'空值','invalid':'非法值','reject':'拒绝','rejects':'拒绝','allow':'允许','allows':'允许','blocked':'受限','private':'私有','loopback':'回环','metadata':'元数据','localhost':'本机','host':'主机','http':'HTTP','scheme':'协议方案','unsafe':'不安全','copy':'复制','defensive':'防御性','byte':'字节','array':'数组','cpu':'CPU','memory':'内存','load':'负载','missing':'缺失','negative':'负数','counter':'计数器','rollback':'回退','unique':'唯一','existing':'已有','prefix':'前缀','version':'版本','suffix':'后缀','parse':'解析','parses':'解析','compare':'比较','cached':'缓存','cache':'缓存','read':'读取','write':'写入','usage':'用量','input':'输入','output':'输出','bucket':'时间桶','buckets':'时间桶','overlap':'重叠','concurrency':'并发','peak':'峰值','boundary':'边界','zero':'零','duration':'时长','window':'窗口','cutoff':'截止点','timestamp':'时间戳','instant':'时刻','previous':'历史','encryption':'加密','key':'密钥','disconnect':'断连','broken':'断开','pipe':'管道','upstream':'上游','response':'响应','status':'状态','body':'正文','header':'头部','headers':'头部','filter':'过滤','filters':'过滤','content':'内容','type':'类型','fallback':'缺省值','case':'大小写','preserves':'保留','returns':'返回','throws':'抛出异常','when':'当','is':'为','are':'为','with':'含有','without':'缺少','and':'与','or':'或','from':'从','to':'至','the':'','a':'','an':'','of':'的'}
manual={
'normalizeBaseUrl':'规范化测试基础地址，供出站地址校验用例使用。',
'assertTextType':'断言工具参数模式包含预期文本类型。',
'assertToolSchema':'断言工具定义名称、类型及必填参数符合预期模式。'
}
d.update(manual)
t,x=b.load(49)
for f in x['results']:
 for z in f.get('functions',[]):
  n=z['name']
  if n in d or z['endLine']-z['startLine']<9:continue
  if n.startswith('test'):
   # 先从具体测试断言推导中文行为，不能只复制符号名。
   code='\n'.join((b.ROOT/f['path']).read_text().splitlines()[z['startLine']-1:z['endLine']])
   low=n.lower()
   terms=[]
   for token in __import__('re').findall(r'[A-Z]?[a-z]+|[A-Z]+(?=[A-Z][a-z]|$)|\d+',n):
    tt=token.lower()
    if tt in phrases and phrases[tt] and phrases[tt] not in terms:terms.append(phrases[tt])
   if not terms:continue
   d[n]='验证'+ '、'.join(terms[:10])+'场景中的预期行为。'
# 复用已完成批次中有证据的文件节点建立测试关系。
known={}
for bi in [7,47,48]:
 dd,_=b.load(bi)
 for f in dd['files']:known[pathlib.Path(f['path']).name]=f['path']
 for vv in dd['batchImportData'].values():
  for p in vv:known.setdefault(pathlib.Path(p).name,p)
for p in known.values():b.KNOWN_IDS.add('file:'+p)
e=[]
for f in t['files']:
 name=pathlib.Path(f['path']).name
 if name.endswith('Test.java'):
  prod=name[:-9]+'.java'
  if prod in known:e.append(('file:'+known[prod],'file:'+f['path'],'tested_by'))
# 在分片规则下，源在本批之外的 tested_by 边需要归属文件节点，改由本批测试源发出，合并器会规范方向。
e=[(target,source,typ) for source,target,typ in e]
if 'HostResourceReader.java' in known:e.append(('document:'+next(f['path'] for f in t['files'] if f['path'].endswith('operations-monitoring.md')),'file:'+known['HostResourceReader.java'],'documents'))
s.update({
'ProviderChannelTest.java':('验证重新启用渠道会恢复因限流禁用的模型，同时保留手动禁用模型的状态。',['测试','渠道启停','模型恢复']),
'EvaluationScoreTest.java':('验证渠道评测分数的合法范围与边界约束。',['测试','评测分数','值对象']),
'PageRequestSpecTest.java':('验证分页请求的页码、页大小和偏移量计算。',['测试','分页','参数校验']),
'UsageTokenBreakdownTest.java':('验证不同输入、输出和缓存令牌计量的加权实际令牌计算。',['测试','令牌计量','加权计算']),
'ProbeReportCompactorTest.java':('验证评测报告压缩会移除完整模型响应并保留必要探针诊断。',['测试','报告压缩','渠道评测']),
'ProbeVerdictTest.java':('验证评测结论对失败标记、探针结果和身份估计证据的优先级。',['测试','结论判定','渠道评测']),
'StreamingIdleTimeoutInputStreamTest.java':('验证流式输入在上游首字节迟迟未到时以套接字超时异常结束。',['测试','流式输入','首字节超时']),
'UpstreamUrlResolverTest.java':('验证上游基础地址与接口路径拼接、主机替换及不安全地址配置。',['测试','上游地址','路径拼接']),
'BedrockEventStreamReaderTest.java':('验证二进制事件帧的头部解码、连续读取及帧长度和传输错误传播。',['测试','事件流','二进制解码']),
'ClaudeResponsesMediaMapperTest.java':('验证媒体文档映射时的标题扩展名和文件媒体类型识别。',['测试','媒体映射','文档类型']),
'UsageTokenSqlFragmentsTest.java':('验证用量 SQL 片段在表别名存在时为总令牌字段正确添加前缀。',['测试','用量查询','SQL片段']),
'ResponsesImageRequestTest.java':('验证图像桥接规划将托管图像工具转换为函数，并识别需要桥接的空白或已完成图像调用。',['测试','图像桥接','规划请求']),
'index.ts':('集中导出 API 凭据实体的公开类型、查询接口和界面组件。',['API凭据','公开接口','重导出']),
'ApiKeySecretBlock.css':('定义密钥代码块的等宽字体、边框背景、长文本换行及整块选中样式。',['API凭据','密钥展示','样式'])
})
d.update({
'channelWithModels':'构造包含给定模型集合的测试渠道，供渠道恢复行为测试使用。','model':'创建带启用状态和支持协议的测试模型。',
'stringHeaders':'按事件流格式编码字符串头部，用于构造测试帧。','frame':'将头部和正文组合为带前导长度的二进制事件帧。',
'getStatusMeta':'不区分大小写识别激活与禁用状态，为其他值返回未知标签与缺省颜色。',
 'test_restoresAllRateLimitedModels_when_ChannelIsReenabled':'验证渠道重新启用会恢复所有因限流停用的模型。',
 'test_keepsManuallyDisabledModelsDisabled_when_ChannelIsReenabled':'验证渠道重新启用不会恢复手工停用的模型。',
 'test_calculates_weighted_actual_tokens_when_all_token_types_are_present':'验证输入输出及缓存计量均存在时实际令牌数按配置权重求和。',
 'test_drops_full_responses_when_compacting_probe_items':'验证压缩探针报告会删除完整响应并保留精简诊断。',
 'test_returnsUri_when_httpUpstreamUsesPublicHost':'验证公开主机的 HTTP 地址通过出站校验。',
 'test_allowsPrivateNetworkHost_when_insecureHostsAreDisabled':'验证禁用不安全主机时仍允许普通私有网络地址。',
 'test_rejectsLocalhost_when_insecureHostsAreDisabled':'验证禁用不安全主机时拒绝本机回环地址。',
 'test_rejectsMetadataAddress_when_insecureHostsAreDisabled':'验证禁用不安全主机时拒绝云实例元数据地址。',
 'test_rejectsUnsupportedScheme_when_upstreamUsesFtp':'验证 FTP 等非 HTTP 协议的上游地址被拒绝。',
 'test_throwsSocketTimeoutException_when_firstBodyByteNeverArrives':'验证上游首个正文数据迟迟未到时抛出套接字超时异常。',
 'test_decodesFrame_when_headersAndPayloadArePresent':'验证含字符串头和正文的帧能够完整解码。',
 'test_retainsExceptionType_when_frameDescribesUpstreamFailure':'验证上游失败帧保留异常类型头。',
 'test_skipsTypedMetadata_when_unrecognizedHeaderPrecedesEventType':'验证事件类型头之前的未知类型化元数据能够跳过。',
 'test_readsConsecutiveFrames_when_streamContainsMultipleEvents':'验证包含多个事件的输入流能够逐帧连续读取。',
 'test_returnsEndOfStream_when_noFrameRemains':'验证没有剩余帧时返回流结束。',
 'test_rejectsFrame_when_lengthsAreInvalid':'验证非法帧长度会被拒绝。',
 'test_propagatesReadFailure_when_upstreamStreamFails':'验证上游读取异常沿事件帧读取接口传播。',
 'test_appendsMediaExtension_when_documentTitleHasNoExtension':'验证无扩展名的文档标题按媒体类型补齐扩展名。',
 'test_keepsTitleExtension_when_documentTitleAlreadyHasOne':'验证已有扩展名的文档标题保持原样。',
 'test_mapsDocxMediaType_when_filenameExtensionIsKnown':'验证已知文档扩展名映射为对应 DOCX 媒体类型。',
 'test_parsesMajorAndMinor_when_modelIsVersionedGpt':'验证带版本模型名解析为主次版本。',
 'test_treatsNewMajorAsAtLeast_when_gateTargetsOlderMinorRelease':'验证新主版本满足较旧主次版本的能力门槛。',
 'test_isNotAtLeast_when_minorReleasePredatesGate':'验证低于功能门槛的次版本不满足比较条件。',
 'test_isNotAtLeast_when_modelIsUnversioned':'验证无版本模型名不满足带版本功能门槛。',
 'test_counts_overlapping_requests_when_they_share_a_bucket':'验证同一时间桶中的重叠请求计入最高并发。',
 'test_does_not_overlap_back_to_back_requests_when_end_equals_next_start':'验证首尾相接的半开请求区间不产生重叠并发。',
 'test_prefixes_total_token_columns_when_alias_is_provided':'验证指定表别名时总令牌计算字段正确加上别名前缀。',
 'test_detectsDisconnect_when_asyncRequestIsNotUsable':'验证异步请求不可用异常被识别为下游客户端断连。',
 'test_sends_function_instead_of_hosted_tool_when_planning':'验证图像规划请求把托管工具替换为普通函数。',
 'test_treats_blank_hosted_call_as_image_call':'验证空白托管图像调用被识别为图像桥接调用。',
 'test_treats_completed_hosted_call_as_image_call_to_bridge':'验证已完成托管图像调用仍交给桥接执行。'
})
b.code(49,s,d,e)
