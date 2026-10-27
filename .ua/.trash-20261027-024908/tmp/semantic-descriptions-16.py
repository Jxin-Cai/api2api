import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'ModelGroupDailyUsageService.java':('按所有者及当前业务日查询模型组各模型的加权令牌用量，整理为模型组到模型用量的映射。',['日用量','应用服务','模型组']),
'InboundRequestContext.java':('保存客户端原始请求头和来源地址，提供大小写无关的头值查询、空上下文及不可变头复制。',['入口上下文','请求头','客户端地址']),
'InvokeGatewayCommand.java':('以不可变命令承载网关调用与追踪标识、密钥哈希、请求模型及协议、原始内容、入口上下文和流式工具推理需求。',['调用命令','协议需求','请求追踪']),
'UsageQueryApplicationService.java':('编排用户和管理员用量记录查询，验证权限与密钥归属，构建可见性过滤和分页参数，并补充用户、密钥及管理员可见的渠道名称。',['用量查询','权限校验','分页聚合']),
'QueryAdminUsageRecordsCommand.java':('承载管理员用量记录查询的操作者、用户、密钥、模型、渠道、协议、时间与分页筛选条件。',['查询命令','管理后台','用量筛选']),
'QueryMyUsageRecordsCommand.java':('承载当前用户用量查询的密钥、模型、协议、时间和分页条件，限制为本人数据范围。',['查询命令','用户门户','用量筛选']),
'PagedUsageRecordViews.java':('将带关联名称的用量记录视图和分页信息、筛选令牌合计组合为应用查询结果。',['分页结果','用量视图','汇总数据']),
'UsageRecordView.java':('为用量记录附加用户名、密钥名与可选渠道名，作为应用层展示视图。',['用量视图','关联名称','数据封装']),
'GatewayInvocationId.java':('将有效标识值封装为网关调用身份，避免与其他业务标识混用。',['调用标识','值对象','身份校验']),
'GatewayRequestId.java':('将非空且符合长度约束的追踪标识封装为网关请求身份。',['请求标识','值对象','输入校验']),
'ModelGroupModelUsage.java':('表示模型组内某个请求模型的加权令牌用量聚合结果。',['聚合结果','模型组','令牌用量']),
'PageRequestSpec.java':('验证页码与允许的页面大小，计算数据库查询偏移量。',['分页参数','参数校验','值对象']),
'PagedUsageRecords.java':('保存不可变用量记录页、页码、页面大小、总记录数和筛选令牌汇总，支持空页与总页数计算。',['分页结果','用量记录','统计汇总']),
'UsageDuration.java':('以非负毫秒数表示调用耗时，并支持从开始与结束时刻计算持续时间。',['调用耗时','时间计算','值对象']),
'UsageRecord.java':('表示调用用量事实，保留请求、用户、密钥、模型、协议、渠道、令牌明细、耗时与失败诊断。支持从终态调用创建记录及非终态调用创建额度预留，并校验各状态字段约束。',['用量事实','额度预留','状态约束']),
'UsageRecordFilter.java':('以用户角色限制用量查询可见范围，组合用户、密钥、模型、渠道、协议和时间条件，并提供用户门户专用过滤构造。',['查询过滤','数据隔离','角色权限']),
'UsageRecordId.java':('将有效标识值封装为用量记录身份，支持查询、更新和预留取消。',['用量标识','值对象','身份校验']),
'UsageRecordStatus.java':('枚举用量预留、成功与失败状态，区分进行中额度占用与调用终态。',['用量状态','枚举类型','额度预留']),
'UsageTimeRange.java':('定义起点包含、终点不包含的用量查询时间范围，验证边界并支持范围包含判断。',['时间范围','值对象','边界校验']),
'UsageRecordRepository.java':('定义用量事实与预留的持久化契约，覆盖保存、更新、取消、角色约束分页和原始或加权令牌汇总，要求额度统计纳入预留占用。',['仓储接口','用量统计','额度预留']),
'UsageTokenSqlFragments.java':('集中定义加权令牌与原始令牌合计的 SQL 表达式，支持添加表别名前缀，统一额度扣减和统计计算口径。',['查询片段','令牌权重','统计一致性'])}
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('ModelGroupDailyUsageService.java',{'loadTodayUsageByGroup':'按当前业务日统计所有者名下模型组的各模型用量，并构建双层映射。'})
add('UsageQueryApplicationService.java',{'queryMyUsageRecords':'校验用户门户权限及所选密钥归属，仅查询当前用户记录，并隐藏渠道名称。','queryAdminUsageRecords':'校验管理员权限，按用户、渠道及其他条件查询分页用量，并附带渠道名称。','toViews':'为查询记录补充用户和密钥名称，仅在管理员视图中补充渠道名称。','username':'查询用量记录关联用户的显示名称。','apiCredentialName':'查询用量记录关联密钥的名称。','providerChannelName':'查询用量记录关联渠道的名称。'})
add('UsageRecord.java',{'fromInvocation':'要求调用已终止，将最终候选、协议、令牌用量、耗时与错误诊断转换为完整用量事实。','reserve':'要求调用未终止且预留值非负，将保守预留量作为输入令牌写入 PENDING 记录，先占用额度。','validateStatusInvariant':'检查预留状态不含上游信息和错误，成功状态具有上游信息且无错误，失败状态具有诊断。','forViewer':'按查看者角色生成可见用量记录，对普通用户隐藏上游渠道等受限信息。','actualTokens':'按平台权重计算记录的额度计费令牌量。'})
add('UsageTokenSqlFragments.java',{'withPrefix':'为加权令牌 SQL 中的各列添加表别名前缀，空前缀直接使用公共表达式。','totalTokensWithPrefix':'为原始令牌合计生成带可选表别名前缀的空值安全 SQL。'})
add('PageRequestSpec.java',{'offset':'根据页码和页面大小计算查询偏移量。'})
add('UsageDuration.java',{'between':'校验调用起止时刻并计算非负毫秒耗时。'})
add('UsageRecordFilter.java',{'forUserPortal':'强制当前用户可见范围，禁止用户门户通过渠道筛选跨越可见性约束。','of':'构建带角色约束的多维用量过滤条件。'})
add('PagedUsageRecords.java',{'empty':'构建记录数为零且令牌合计已知为零的分页结果。','totalPages':'按总记录数和页面大小向上取整计算总页数。'})
add('PagedUsageRecordViews.java',{'totalPages':'根据总记录数和页面大小计算展示总页数。'})
add('InboundRequestContext.java',{'copyHeaders':'剔除无效项并复制不可变请求头映射，避免外部修改入口上下文。','headerValues':'大小写无关地读取指定请求头的全部值。','firstHeader':'读取指定请求头的首个值。','empty':'创建无请求头和客户端地址的入口上下文。'})
m.build(16,d,o)
