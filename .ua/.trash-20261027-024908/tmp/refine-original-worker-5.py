import json,pathlib,collections
u=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua')
# 以下为人工核对后的语义注释，仅装配已提取节点的描述。
common={
'system':'校验管理员访问权限后读取系统资源监控快照。','health':'汇总采样健康信息，输出资源读取范围及异常提示。','ensureProtocolRateCoverage':'检查并补齐所有受支持协议的请求速率项，保证结果协议覆盖完整。','requireSupported':'校验统计粒度属于允许集合，不支持时拒绝查询。','isDaily':'判断统计粒度是否为按日聚合。','contains':'判断给定时刻是否位于起点包含、终点不包含的统计区间内。','durationMinutes':'将时间窗口持续时长换算为分钟。','durationMinutesExact':'以高精度计算窗口分钟数，供请求速率归一化。',
'getRank':'返回渠道在慢渠道统计中的名次。','getProviderChannelId':'返回统计项关联的上游渠道标识。','getProviderChannelName':'返回统计项关联的上游渠道名称。','getMaxDurationMillis':'返回该渠道请求的最大总耗时毫秒数。','getAvgDurationMillis':'返回该渠道请求的平均总耗时毫秒数。','getMaxFirstTokenMillis':'返回该渠道请求的最大首令牌延迟。','getAvgFirstTokenMillis':'返回该渠道请求的平均首令牌延迟。','getRequestCount':'返回统计窗口内的请求数量。','getBucketStart':'返回统计时间桶的起始时刻。','getBucketEnd':'返回统计时间桶的结束时刻。','getPeakConcurrency':'返回当前时间桶的峰值并发数。','getCredentialId':'返回并发统计所对应的密钥标识。','getCredentialName':'返回并发统计所对应的密钥名称。',
'zero':'创建令牌值为零的统计对象。','todayActualTokens':'返回今日按计费权重计算的令牌量。','todayTotalTokens':'返回今日各类原始令牌合计。','monthActualTokens':'返回月度窗口内按计费权重计算的令牌量。','monthTotalTokens':'返回月度窗口内各类原始令牌合计。','todayTokens':'返回今日完整的令牌明细及已知状态。','monthTokens':'返回月度窗口完整的令牌明细及已知状态。','getTodayTokens':'返回今日令牌明细的兼容访问视图。','getMonthTokens':'返回月度令牌明细的兼容访问视图。','getTodayActualTokens':'返回今日加权令牌量的展示值。','getTodayTotalTokens':'返回今日原始令牌合计的展示值。','getMonthActualTokens':'返回月度加权令牌量的展示值。','getMonthTotalTokens':'返回月度原始令牌合计的展示值。','calculate':'依据请求数量与窗口分钟数计算每分钟请求速率。','toMillions':'将原始令牌量除以一百万，转换为百万令牌展示单位。','getName':'返回用量分布项的维度名称。','getValue':'返回用量分布项用于展示的统计值。','actualTokens':'按统一平台权重计算输入、输出和缓存令牌的计费量。','getActualTokens':'返回该令牌明细的加权计费用量。','isUsageKnown':'判断调用用量是否由协议响应提供且已知。','sumUserTotalTokens':'汇总指定用户在统计窗口内的原始令牌用量。','sumPlatformTotalTokens':'汇总平台在统计窗口内的原始令牌用量。','findModelDistribution':'按模型聚合请求数或令牌量，返回指定时间范围内的模型用量分布。','findChannelDistribution':'按渠道聚合请求数或令牌量，返回指定时间范围内的渠道用量分布。','countUsageRecords':'按用户及时间条件统计调用记录数量。','success':'创建携带成功码、消息和可选数据的统一业务响应。','failure':'创建携带错误码与错误消息的统一失败响应。',
'invoke':'执行同步网关调用并从完整调用结果中提取上游响应。','withoutProviderResponse':'创建无上游响应的网关结果，用于路由或调用在转发前终止的情况。','hasProviderResponse':'判断当前调用结果是否包含上游响应。','opened':'将已打开的上游输入流、候选路由及用量标识包装为流式调用上下文。','failed':'创建流未打开时的失败调用上下文，保留领域失败结果。','requiresProtocolConversion':'比较入口与上游协议，判断响应流是否需要转换。','availableOn':'判断当前协议操作是否适用于指定协议。','requiresNativeProtocol':'判断操作是否只允许同协议原生路由。','acceptsMultipartForm':'判断操作是否接受多部分表单请求。','successful':'根据 HTTP 状态码判断上游响应是否成功。','close':'关闭持有的上游响应输入流并释放资源。','empty':'创建不包含上游响应头的空元数据。','present':'判断字段路径是否能读取到非缺失值。','withRequestConversion':'返回加入请求方向转换结果的新转换轨迹。','withResponseConversion':'返回加入响应方向转换结果的新转换轨迹。','withRequestConversionRecorded':'返回已记录请求转换的新转换轨迹。','withResponseConversionRecorded':'返回已记录响应转换的新转换轨迹。','passthrough':'判断本次转换是否为同协议透传。','isPassthrough':'判断请求和响应转换轨迹是否保持透传。','markAuthenticated':'将调用从初始状态迁移为已认证状态。','isTerminal':'判断调用是否已经成功或失败而进入终态。','finalCandidate':'返回调用最终使用的路由候选。','failures':'返回调用累计的候选失败记录。','markFirstToken':'记录首个响应令牌的到达时刻，用于首令牌延迟统计。','isStreaming':'返回请求或调用结果的流式标志。','withoutFailures':'创建不附带候选失败历史的调用错误。','normalizeMessage':'清理错误消息并限制长度，避免无界诊断内容进入领域对象。','route':'将路由计划附加到已认证调用，驱动其进入可尝试候选状态。','matches':'判断当前转换类型是否与指定转换类别一致。','toProtocolPayload':'将兼容转换载荷还原为统一协议载荷。','protocol':'返回当前载荷所属的协议。','body':'返回转换载荷或结果中的原始内容。','streaming':'返回协议载荷的流式标记。','withAnthropicBetaFeatures':'返回带去重实验特性列表的新转换需求，保留现有路由和能力约束。','forRoute':'返回关联当前候选路由上下文的新转换需求。','toProtocolConversionRequest':'将领域转换需求映射为统一协议转换请求。','sourceProtocol':'返回转换前的源协议。','targetProtocol':'返回转换后的目标协议。','usage':'返回协议转换过程中提取的统一令牌用量。',
'rehydrate':'根据已保存的身份、字段集合和审计时间恢复协议元数据。','presentAndNotEmpty':'判断字段路径存在且对应值不为空。','readText':'按契约字段路径读取字符串值。','readBoolean':'按契约字段路径读取布尔值。','readLong':'按契约字段路径读取长整数值。','findAll':'返回从契约注册表投影出的全部协议元数据。','findById':'根据协议元数据标识查询内存索引。','findByProtocolType':'根据协议类型查询对应元数据。','listProtocolMetadata':'解析当前操作者，验证权限后查询并转换协议元数据列表。','getProtocolMetadataDetail':'解析协议参数和当前操作者，查询并转换协议元数据详情。','toUsageListResponse':'将带用量和限流状态的密钥视图列表转换为接口列表响应。','toRevealCommand':'绑定当前用户和目标密钥身份，构造明文密钥揭示命令。',
'ofHeaders':'复制请求头并构造不可变入口请求上下文。','clientIp':'返回入口请求的客户端地址。','hasRawQuery':'判断入口请求是否带有可保留的原始查询参数。','builder':'创建不可变网关调用命令的构建器。','isToolCallingRequired':'返回解析出的工具调用转换需求。','isReasoningRequired':'返回解析出的推理转换需求。','isEmpty':'判断当前用量分页是否没有记录。','totalTokens':'返回输入、输出及缓存明细的原始令牌合计。','isPending':'判断用量记录是否仍为进行中的额度预留。','isSuccessful':'判断用量记录是否表示成功调用。','isFailed':'判断用量记录是否表示失败调用。','ofNullable':'将非空存储值包装为密码哈希，空值保留为空以兼容历史账户。','isAdmin':'判断用户角色是否为管理员。','isRegularUser':'判断用户角色是否为普通用户。','hasPasswordHash':'判断账户是否已配置密码哈希。','grants':'判断用户角色是否授予指定访问范围。','validate':'校验用户名格式、内容与长度，拒绝不合法输入。','selectByUsername':'按未删除用户名执行账户查询。',
'sumTotalTokensByApiCredential':'统计指定密钥的原始令牌合计。','sumActualTokensByApiCredential':'按平台权重统计指定密钥的计费用量，包含有效预留。','sumActualTokensByModelGroupAndModel':'按模型组与请求模型统计指定窗口内的加权令牌量。','sumTokens':'按领域过滤条件汇总令牌明细，并转换为统一业务用量。','toPO':'将领域用量事实转换为数据库字段，保留身份、路由、状态、令牌与耗时。','toQueryPO':'将领域过滤条件和分页参数映射为数据库查询对象。','toPage':'将数据库查询行及统计元信息映射为带角色约束的领域分页。','toTokenBreakdown':'将数据库令牌聚合结果转换为业务令牌明细与已知状态。','updateById':'按用量标识更新预留记录为最终事实。','deleteById':'软删除指定用量预留以取消占用。','selectByRequestId':'按请求追踪标识查询未删除用量记录。','countByFilter':'使用与明细查询一致的过滤条件统计记录总数。','nextInvocationId':'生成独立的 UUID 网关调用标识。','requestId':'保留有效客户端请求标识，缺失或不合法时生成 UUID 请求标识。','nextUsageRecordId':'生成独立 UUID 用量记录标识。','queryMyUsageRecords':'绑定当前会话用户，转换门户查询命令并返回受本人权限限制的用量分页。','queryAdminUsageRecords':'绑定当前操作者，转换后台筛选命令并返回管理员可见的用量分页。','toQueryMyCommand':'将门户请求参数及当前用户转换为本人用量查询命令。','toQueryAdminCommand':'将后台筛选参数及当前操作者转换为管理员用量查询命令。','getUser':'验证当前管理员并读取指定用户的账户详情。','getDefinitionByDirection':'按源目标协议方向查询转换定义详情。','enableConversion':'验证管理员权限后启用指定转换定义，并保存更新状态。','disableConversion':'验证管理员权限后禁用指定转换定义，并保存更新状态。','selectBySourceAndTarget':'按源目标协议对查询转换配置记录。'
}
specific={
'ProtocolRequestRate.java:zero':'为指定协议创建请求数量和每分钟速率均为零的统计项。',
'CredentialTokenTrendPoint.java:zero':'为指定密钥与时间桶创建零令牌趋势点。','ProtocolTokenTrendPoint.java:zero':'为指定协议与时间桶创建零令牌趋势点。',
'UpstreamResponseMetadata.java:present':'包装已收到的上游响应头并标记元数据存在。',
'GatewayInvocationResult.java:success':'由最终候选、上游协议模型与令牌用量构建通过状态校验的成功结果。',
'GatewayInvocationResult.java:failed':'由错误类型、消息与候选失败历史创建失败终态结果。',
'ConversionKind.java:from':'源目标协议相同时确定透传类别，否则确定跨协议转换类别。',
'ConversionPayload.java:from':'将基础协议载荷包装为兼容转换载荷。','ConversionResult.java:from':'将统一协议转换结果包装为兼容结果模型。',
'ApiCredentialHttpConverter.java:toListResponse':'将密钥领域列表逐项映射并包装为密钥列表响应。','ModelGroupHttpConverter.java:toListResponse':'将模型组视图列表逐项转换为包含有效模型和日限额状态的响应。',
'JdbcUserAccountMapper.java:selectById':'按用户标识查询未软删除账户，并映射账户字段与审计时间。','JdbcUserAccountMapper.java:selectAll':'按创建时间读取全部未软删除账户。','JdbcUserAccountMapper.java:params':'将账户持久化字段及审计时间转换为具名 SQL 参数。',
'JdbcUsageRecordMapper.java:selectById':'按用量标识查询未软删除的调用记录。',
'JdbcProtocolConversionDefinitionMapper.java:selectById':'按转换标识读取未软删除的协议转换配置。','JdbcProtocolConversionDefinitionMapper.java:selectAll':'按创建时间和标识顺序读取全部未软删除协议转换配置。',
'ProtocolMetadataHttpConverterTest.java:metadata':'构造带多字段分组的协议元数据测试样本。','ProtocolMetadataHttpConverterTest.java:field':'构造具有指定分组和排序的协议字段定义测试样本。',
'ModelGroupTest.java:groupWithLimits':'构建带模型白名单及模型日限额的模型组测试数据。',
'ResponsesImageRoutingTest.java:setUp':'组装图像桥接路由、上游调用和用量仓储测试依赖。',
'UsageRecordPersistenceConverterTest.java:po':'构造不同调用生命周期、令牌明细和路由信息的持久化测试样本。',
'UsageRecordHttpConverterTest.java:pageWith':'将用量视图和令牌汇总包装为分页测试数据。','UsageRecordHttpConverterTest.java:successfulRecord':'构造带用户、密钥和路由信息的成功用量记录。',
'BootstrapAdminInitializerTest.java:adminWithPasswordHash':'构造带指定密码哈希的管理员账户，供初始化幂等性测试。'
}
# 测试名称精确对应各自的预期行为和触发条件。
tests={
'test_detectsReasoningCapability_when_claudeHasOnlyMessageEffort':'请求仅包含消息级推理强度时，验证仍识别为需要推理转换。',
'test_acceptsStateReferenceWithoutInput_when_responsesUsesPreviousResponse':'请求仅引用前一响应而没有新输入时，验证契约接受状态续接。',
'test_detectsToolCapability_when_responsesHistoryContainsToolResultWithoutTools':'历史含工具结果但无工具定义时，验证解析仍识别工具能力需求。',
'test_detectsReasoningCapability_when_responsesHistoryContainsEncryptedReasoning':'历史含加密推理项时，验证解析识别推理能力需求。',
'test_acceptsTypedHistoryUnion_when_responsesUsesCurrentItems':'历史输入采用带类型的多种条目时，验证契约接受当前协议联合结构。',
'test_keepsNativeContractUnchanged_when_clientContainsCompatibilityExtension':'客户端携带兼容扩展时，验证不改变原生协议契约字段。',
'test_registry_contains_executable_shapes_when_five_protocols_are_registered':'注册五类协议时，验证各协议具有可执行请求、响应或流事件形状。',
'test_parseRequest_reads_model_and_stream_through_contract_fields_when_claude_body_is_valid':'请求体合法时，验证通过契约字段正确读取模型和流式标记。',
'test_parseGatewayRequest_extractsSignalsInOnePass_when_claude_historyContainsMixedContent':'历史消息含混合内容块时，验证快速解析一次识别模型、流式、工具与推理信号。',
'test_parseRequest_reads_model_and_stream_when_images_body_requests_streaming':'图像请求包含流式参数时，验证正确解析模型及流式标记。',
'test_parseRequest_accepts_string_message_content_when_openai_responses_uses_text_shorthand':'Responses 消息使用字符串内容简写时，验证契约允许并正确解析。',
'test_metadata_projects_the_same_field_refs_when_registry_is_the_source':'以契约注册表生成元数据时，验证投影字段与可执行字段引用一致。',
'test_reports_official_api_versions_when_five_protocols_are_registered':'注册五类协议时，验证各协议元数据报告对应官方规范版本。',
'test_uses_official_flattened_tool_paths_when_responses_contract_is_registered':'注册 Responses 契约时，验证工具字段采用官方扁平路径。',
'test_exposes_latest_official_fields_when_sdk_schema_snapshots_are_registered':'注册当前 SDK 结构快照时，验证协议包含最新官方字段。',
'test_loadsAllContractFields_when_claudeMessagesMetadataIsRequested':'查询 Messages 元数据时，验证投影包含全部契约字段。',
'test_groups_content_block_fields_when_claude_messages_metadata_is_loaded':'加载消息协议元数据时，验证内容块字段归入独立分组。',
'test_exposes_claude_code_deferred_tool_field_when_latest_metadata_is_loaded':'加载最新协议元数据时，验证提供延迟工具相关字段。',
'test_orders_content_block_after_message_when_metadata_contains_content_block_fields':'详情包含内容块字段时，验证内容块分组排列在消息分组之后。',
'test_preserves_all_fields_when_mapping_metadata_detail_response':'将协议元数据转为详情响应时，验证所有字段均被保留。',
'test_returns_credential_whitelist_when_api_key_is_active':'密钥启用且可用时，验证查询返回该密钥模型白名单。',
'test_soft_deletes_credential_when_owner_requests_deletion':'密钥所有者请求删除时，验证执行软删除并保留审计记录。',
'test_returns_actual_and_total_tokens_when_listing_credentials':'查询密钥列表时，验证同时返回加权计费用量和原始令牌合计。',
'test_marks_model_rate_limited_when_group_daily_limit_is_reached':'模型组日限额耗尽时，验证密钥视图将相应模型标记为受限。',
'test_intersects_permissions_when_provider_also_supports_models_outside_group':'渠道支持白名单外模型时，验证模型组有效模型仍取权限交集。',
'test_returns_empty_support_when_group_whitelist_is_empty':'模型组白名单为空时，验证有效支持模型为空。',
'test_updates_effective_group_support_when_provider_availability_changes':'渠道可用模型发生变化时，验证模型组有效支持同步变化。',
'test_preserves_saved_configuration_when_provider_support_disappears':'渠道不再支持配置模型时，验证模型组原始白名单和限额配置仍被保留。',
'test_serializes_no_effective_models_when_group_has_no_provider_support':'模型组没有可用渠道支持时，验证响应的有效模型集合为空。',
'test_keeps_daily_limit_status_when_provider_is_unavailable':'渠道不可用时，验证模型组日限额及受限状态仍正常展示。',
'test_rejects_deletion_when_group_has_bound_credentials':'模型组仍绑定密钥时，验证删除操作被拒绝。',
'test_updates_whitelist_when_group_configuration_changes':'更新模型组配置时，验证模型白名单随之更新。',
'test_rejects_request_when_model_daily_limit_is_reached':'模型当天用量达到限额时，验证后续请求被拒绝。',
'test_reports_rate_limited_models_when_consumption_reaches_limits':'各模型消费达到对应限额时，验证返回正确的受限模型集合。',
'test_preservesStoredHashFormat_when_hashingKnownKey':'对固定密钥计算哈希时，验证结果保持现有持久化哈希格式兼容。',
'test_matchesIssuedKeyHash_when_gatewayAuthenticatesGeneratedKey':'网关认证新签发密钥时，验证认证哈希与签发存储哈希一致。',
'test_generates_sk_prefixed_uuid_when_creating_api_key_material':'生成密钥材料时，验证采用带 sk 前缀的 UUID 格式。',
'test_generates_unique_keys_when_creating_multiple_api_key_materials':'多次生成密钥材料时，验证各明文密钥互不重复。',
'test_initializes_usage_metrics_to_zero_when_mapping_new_credential':'映射新建密钥时，验证累计与当日原始及加权用量均初始化为零。',
'test_returns_only_supported_whitelisted_models_when_api_key_is_valid':'密钥有效时，验证模型列表仅包含白名单与渠道支持的交集。',
'test_returns_empty_list_when_no_provider_supports_whitelisted_models':'无渠道支持白名单模型时，验证模型列表为空。',
'test_returns_empty_list_when_group_forbids_all_models':'模型组不允许任何模型时，验证模型列表为空。',
'test_refreshes_model_list_when_provider_support_changes':'渠道支持变化时，验证下一次模型列表查询使用最新可用模型。',
'test_rejects_model_listing_when_credential_authentication_fails':'密钥认证失败时，验证模型列表请求被拒绝。',
'test_uses_native_images_route_when_only_images_conversion_is_registered':'仅注册图像转换时，验证 Responses 图像桥接选择原生图像路由。',
'test_fails_over_to_second_image_channel_when_first_returns_503':'首个图像渠道返回 503 时，验证请求切换到第二候选渠道。',
'test_cancels_reservation_when_image_stream_is_truncated':'图像流被截断时，验证取消额度预留而不记录完整成功用量。',
'test_records_stream_usage_when_images_send_completion_data_without_event_header':'图像完成数据缺少事件头时，验证仍提取并结算流式用量。',
'test_preservesPersistedFields_when_rehydratingEachLifecycleState':'恢复各生命周期状态的用量记录时，验证持久化身份、路由和状态字段完整保留。',
'test_preservesFirstTokenLatency_when_latencyIsAbsentOrMeasured':'首令牌延迟缺失或已测量时，验证转换分别保留空值或实际测量值。',
 'toPOPersistsDomainDuration':'将领域用量记录转为数据库对象时，验证使用领域耗时而非重新计算。',
'test_recalculatesTotalTokens_when_persistedTotalIsInconsistent':'历史保存的总令牌量与明细不一致时，验证根据规范化明细重新计算合计。',
'preservesExistingAdminPasswordHash':'管理员已有密码哈希时，验证启动初始化不重置该凭据。',
'initializesMissingAdminPasswordHashOnce':'历史管理员缺少密码哈希时，验证仅补全一次并保持后续启动幂等。'
}
for i in [5,6,14,15,16,17,18,19,20]:
 overrides={};missing=[]
 for p in sorted((u/'intermediate').glob(f'batch-{i}-part-*.json')):
  for n in json.loads(p.read_text())['nodes']:
   if '对应的数据或状态' not in n['summary']:continue
   key=pathlib.Path(n['filePath']).name+':'+n['name'];s=specific.get(key) or tests.get(n['name']) or common.get(n['name'])
   if s:overrides[key]=s
   else:missing.append(key)
 assert not missing,(i,missing)
 (u/f'tmp/semantic-refinements-{i}.json').write_text(json.dumps(overrides,ensure_ascii=False,indent=2)+'\n')
 print(i,len(overrides))
