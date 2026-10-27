import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'UsageRecordRepositoryImpl.java':('实现用量事实保存、预留更新与取消、按角色筛选分页和令牌汇总，衔接领域过滤、持久化映射与原始或加权用量口径。',['用量仓储','分页查询','额度统计']),
'UsageRecordPersistenceConverter.java':('映射用量领域模型与持久化对象，兼容历史记录缺失状态、协议、时间和错误字段，并规范化令牌明细。',['持久化转换','历史兼容','令牌规范化']),
'JdbcUsageRecordMapper.java':('使用具名参数 SQL 保存、更新和取消用量记录，执行软删除过滤分页、明细汇总与模型组维度加权用量聚合。',['数据访问','用量查询','参数化查询']),
'ModelGroupModelUsagePO.java':('保存按模型组与请求模型分组的加权令牌聚合结果。',['聚合对象','模型组','令牌用量']),
'UsageRecordMapper.java':('定义用量记录写入、预留更新取消、筛选查询、计数及原始和加权令牌汇总的数据访问接口。',['数据访问接口','额度预留','用量统计']),
'UsageRecordQueryPO.java':('承载用户、密钥、模型、渠道、协议、时间及分页条件，供用量 SQL 构建使用。',['查询对象','多维筛选','分页参数']),
'UsageTokenSummaryPO.java':('保存输入、输出、缓存创建、缓存读取和总令牌及用量已知标记的数据库聚合结果。',['聚合对象','令牌明细','数据库映射']),
'GatewayIdentifierHelper.java':('校验可复用的请求标识并在缺失或无效时生成 UUID，同时生成独立调用标识，保留客户端追踪语义。',['请求追踪','标识生成','格式校验']),
'UsageRecordController.java':('提供用户和管理员用量分页 HTTP 查询，将请求及当前会话用户转换为查询命令，并按视图权限输出结果。',['用量接口','用户门户','管理后台']),
'UsageRecordHttpConverter.java':('转换用量筛选请求与应用命令，映射分页及令牌汇总响应，对普通用户隐藏渠道信息并限制页面大小。',['接口转换','数据隔离','分页校验']),
'AdminUsageRecordQueryRequest.java':('定义管理员用量筛选请求，支持用户、密钥、模型、渠道、协议、时间及分页参数。',['请求模型','管理后台','用量筛选']),
'MyUsageRecordQueryRequest.java':('定义用户本人用量筛选请求，支持密钥、模型、协议、时间及分页参数。',['请求模型','用户门户','用量筛选']),
'UsageRecordPageResponse.java':('返回用量记录分页、总记录数和总页数，附带筛选后的原始及加权令牌合计和管理员视图标志。',['响应模型','分页结果','令牌汇总']),
'UsageRecordResponse.java':('输出调用身份、关联名称、协议模型、令牌明细、耗时、来源地址和错误诊断，渠道字段受管理员视图限制。',['响应模型','用量详情','调用诊断']),
'GatewayIdentifierHelperTest.java':('验证有效请求标识保留、缺失或无效标识的 UUID 回退以及调用标识的独立生成。',['请求追踪测试','标识校验','行为验证']),
'UsageRecordHttpConverterTest.java':('验证用量转换中允许的页面大小、协议别名兼容及原始与加权令牌汇总。',['转换测试','分页校验','令牌汇总']),
'UsageRecordTest.java':('验证调用终态到用量事实的转换、耗时计算及用户视图隐藏渠道、管理员视图保留渠道的规则。',['领域测试','用量事实','数据隔离']),
'JdbcUsageRecordMapperTest.java':('验证 SQL 用量统计采用原始明细和统一权重，覆盖预留更新取消、状态兼容、模型组聚合及当日密钥用量。',['数据访问测试','额度预留','统计口径']),
'UsageRecordPersistenceConverterTest.java':('验证用量持久化转换兼容历史缓存令牌口径和网关协议别名，保留上游路由身份并补全失败记录。',['转换测试','历史兼容','协议别名'])}
d.update({
'UsageRecordPO.java':('定义用量记录表字段，持久化调用身份、路由、状态、令牌明细、时间、来源与诊断及软删除标记。',['持久化对象','用量事实','审计字段']),
'GatewayRequestMapper.java':('将契约解析结果、密钥头、追踪标识和入口上下文转换为网关调用命令，按操作能力控制流式标记。',['请求映射','网关命令','操作能力']),
'QueryAdminUsageRecordsRequest.java':d['AdminUsageRecordQueryRequest.java'],
'QueryMyUsageRecordsRequest.java':d['MyUsageRecordQueryRequest.java'],
'DashboardResponseConverterTest.java':('验证仪表盘最近调用字段映射，以及门户仪表盘原始和加权令牌汇总的输出。',['仪表盘测试','用量转换','令牌汇总']),
'GatewayRequestMapperTest.java':('验证请求操作不支持流式时，即使协议解析结果要求流式也会在调用命令中关闭。',['请求映射测试','流式约束','操作能力'])})
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('UsageRecordRepositoryImpl.java',{'save':'将用量记录转换为持久化对象并插入数据库。','update':'按原用量标识更新预留记录为最终调用事实。','cancelReservation':'取消指定预留记录以释放额度占用。','findById':'查询用量记录并按管理员可见性恢复完整领域模型。','findByRequestId':'按请求追踪标识查询用量记录列表并应用查看者权限。','query':'将领域过滤和分页转换为查询对象，读取记录、总数及令牌合计，并按角色隐藏受限字段。','toQuery':'将领域过滤值对象展开为持久化查询条件。','toTokenBreakdown':'将数据库令牌汇总转换为统一用量明细。','sumActualTokensByOwnerGroupedByModel':'查询所有者名下模型组的模型用量聚合，并转换为领域结果。'})
add('UsageRecordPersistenceConverter.java',{'toDomain':'根据持久化记录恢复用量事实，补全历史字段，规范化令牌计数并恢复失败诊断。','parseStatus':'兼容无效或缺失状态，按错误字段推断终态，并将缺少上游路由信息的成功记录降为失败。','parseErrorType':'解析历史错误枚举，无效或空值时使用通用上游失败类型。','safeStartedAt':'按开始、创建、修改时间优先级恢复起点，均缺失时回退到纪元。','safeEndedAt':'从结束时间或记录耗时恢复终点，缺失或早于起点时归一到起点。','normalizedTokenBreakdown':'统一非负令牌明细及缓存口径，恢复与当前领域约束兼容的用量对象。'})
add('JdbcUsageRecordMapper.java',{'insert':'以具名参数插入调用身份、路由、令牌、时间及诊断字段。','update':'根据记录标识更新调用最终路由、令牌明细、状态和错误诊断。','cancelReservation':'标记指定预留已取消，避免继续占用令牌额度。','selectByFilter':'组合参数化过滤条件，按开始时间和标识倒序分页查询。','sumTotalTokensByApiCredential':'统计密钥未删除记录的总令牌合计。','sumActualTokensByApiCredential':'按公共权重表达式统计密钥实际计费令牌量，纳入未删除预留。','sumActualTokensByModelGroupAndModel':'关联密钥所属模型组，按请求模型和半开时间窗口聚合加权令牌。','sumActualTokensByOwnerGroupedByModel':'关联所有者密钥，按模型组与请求模型聚合时间窗口内加权用量。','sumTokensByFilter':'按筛选条件汇总各类令牌及用量是否已知标记。','whereClause':'以具名参数拼接用户、密钥、渠道、模型、协议及半开时间范围过滤，并统一排除软删除。','params':'将用量持久化对象展开为数据库具名参数及审计字段。'})
add('UsageRecordHttpConverter.java',{'toMyQueryCommand':'绑定当前用户，将门户筛选条件与默认时间范围、分页参数转换为查询命令。','toAdminQueryCommand':'绑定操作人，将管理员多维筛选及分页参数转换为查询命令。','toPageResponse':'映射记录视图、分页元信息和原始及加权令牌汇总，标明视图权限。','toRecordResponse':'映射调用用量明细及关联名称，按管理员视图保留或隐藏渠道字段。','defaultSize':'接受默认页面大小，拒绝不属于 50、100、200 的请求值。'})
add('GatewayIdentifierHelper.java',{'newInvocationId':'为每次网关调用生成独立 UUID 调用标识。','resolveRequestId':'保留经校验的客户端请求标识，否则生成新的 UUID 追踪标识。','sanitizeRequestId':'规范化并校验请求标识长度与允许字符，拒绝不可安全复用的值。'})
a='backend/src/main/java/com/api2api/'
o['_edges']=[('class:'+a+'infr/repository/usage/UsageRecordRepositoryImpl.java:UsageRecordRepositoryImpl','class:'+a+'domain/usage/repository/UsageRecordRepository.java:UsageRecordRepository','implements',0.9),('class:'+a+'infr/repository/usage/mapper/JdbcUsageRecordMapper.java:JdbcUsageRecordMapper','class:'+a+'infr/repository/usage/mapper/UsageRecordMapper.java:UsageRecordMapper','implements',0.9)]
add('GatewayRequestMapper.java',{'toCommand':'解析密钥哈希及调用和请求标识，将协议事实、入口信息与操作能力封装为统一调用命令；不支持流式的操作强制关闭流式。'})
add('DashboardResponseConverterTest.java',{'test_mapsUsageRecordFields_when_renderingRecentCall':'构造包含身份、协议、用量、来源和耗时的记录，验证最近调用响应完整映射对应字段。','test_maps_actual_and_total_token_sums_when_rendering_front_dashboard':'验证门户仪表盘同时输出原始令牌合计与加权令牌合计。'})
add('GatewayRequestMapperTest.java',{'test_disablesStreaming_when_operationDoesNotSupportIt':'协议请求标记为流式但操作不支持流式时，验证生成的网关命令将流式标记设为假。'})
m.build(18,d,o)
