import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'ModelGroupController.java':('提供当前用户模型组的列表、创建、更新与删除接口，解析会话用户并通过转换器构造应用命令。',['模型组接口','用户门户','请求适配']),
'ApiCredentialHttpConverter.java':('转换密钥管理请求、领域值对象与应用命令，使用 MapStruct 映射密钥详情及用量视图，并封装创建和明文揭示响应。',['密钥管理','对象转换','接口适配']),
'ModelGroupHttpConverter.java':('将模型组请求转换为创建更新命令，并将模型白名单、有效模型、日限额、实际用量及限流状态转换为响应。',['模型组','对象转换','额度展示']),
'ApiCredentialListResponse.java':('包装用户密钥详情列表，供密钥管理列表接口输出。',['响应模型','密钥列表','数据传输']),
'ApiCredentialResponse.java':('定义密钥管理响应，包含预览、模型组与白名单、额度、累计及当日用量、限流模型、状态和审计时间。',['响应模型','密钥详情','额度展示']),
'ChangeModelGroupRequest.java':('承载密钥切换模型组的目标模型组标识。',['请求模型','模型组','参数校验']),
'ChangeTokenLimitRequest.java':('承载密钥额度修改请求的令牌上限，并提供请求参数约束。',['请求模型','额度管理','参数校验']),
'CreateApiCredentialRequest.java':('承载新建密钥的名称、模型组与令牌额度参数。',['请求模型','密钥创建','参数校验']),
'CreateApiCredentialResponse.java':('返回新建密钥详情与本次生成的明文密钥。',['响应模型','密钥创建','密钥材料']),
'ModelGroupListResponse.java':('包装模型组响应列表。',['响应模型','模型组列表','数据传输']),
'ModelGroupRequest.java':('承载模型组名称、模型白名单与各模型每日令牌限额配置。',['请求模型','模型组','日限额']),
'ModelGroupResponse.java':('输出模型组配置、实际可用模型、日限额及用量、受限模型和额度统计时区。',['响应模型','模型组','日限额']),
'RenameApiCredentialRequest.java':('承载密钥重命名请求及名称参数约束。',['请求模型','密钥命名','参数校验']),
'RevealApiCredentialSecretResponse.java':('返回指定密钥的标识、预览和经授权揭示的明文材料。',['响应模型','密钥揭示','密钥材料']),
'GatewayApiKeyHashHelper.java':('从 Authorization 或 x-api-key 提取网关密钥，优先选择 Bearer 凭据并调用统一哈希算法。',['网关认证','密钥提取','哈希计算']),
'GatewayProtocolController.java':('提供模型列表及 Messages、Responses、Chat Completions 和图像操作的兼容入口。通过契约解析、图像桥接和统一命令组织同步或流式网关调用。',['网关入口','多协议适配','流式响应']),
'ApiCredentialApplicationServiceTest.java':('验证密钥白名单、所有者删除、累计与当日用量展示，以及模型组日限额对应的限流状态。',['密钥服务测试','用量统计','行为验证']),
'ModelGroupApplicationServiceTest.java':('验证模型组权限与渠道支持的交集、动态可用性、配置保留、日限额展示及绑定密钥时的删除约束。',['模型组测试','权限交集','行为验证']),
'ModelGroupTest.java':('验证模型组白名单更新、模型每日额度校验、无限额模型放行及受限模型报告。',['领域测试','模型组','日限额']),
'ApiKeyMaterialHelperTest.java':('验证生成密钥格式与唯一性，并确认签发和网关认证沿用一致的存储哈希格式。',['密钥材料测试','哈希兼容','行为验证']),
'ApiCredentialHttpConverterTest.java':('验证新密钥响应转换时累计及当日用量指标初始化为零。',['转换测试','密钥用量','默认值']),
'GatewayApiKeyHashHelperTest.java':('验证网关密钥头选择、Bearer 优先级及缺少凭据时的拒绝行为。',['认证测试','请求头','行为验证']),
'GatewayProtocolControllerTest.java':('验证模型列表是密钥白名单与当前渠道支持的交集，覆盖空集、可用性变化和认证失败。',['网关测试','模型列表','权限交集']),
'ResponsesImageRoutingTest.java':('验证 Responses 图像桥接走原生图像路由、失败重试、权限与额度预检、原始图像用量结算和截断流预留取消。',['图像桥接测试','故障转移','用量结算'])}
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('GatewayProtocolController.java',{'listModels':'认证密钥后，将其模型白名单与当前渠道支持模型求交集并排序，返回兼容模型列表。','claudeMessages':'将 Messages 请求适配为统一网关调用，保留请求头、密钥和请求标识。','claudeCountTokens':'将令牌计数端点适配为相应协议操作并交由统一调用链处理。','openaiResponses':'先尝试 Responses 图像桥接，未被桥接处理时交给统一 Responses 调用链，兼容带和不带版本前缀的端点。','openaiChatCompletions':'将 Chat Completions 请求交给统一协议调用链。','openaiImageGenerations':'将图像生成请求适配为原生图像协议操作。','openaiImageEdits':'读取图像编辑表单并封装为图像协议操作。','openaiImageVariations':'读取图像变体表单并封装为图像协议操作。','invokeProtocol':'按可执行契约提取请求事实，生成应用命令并选择同步响应或流式响应路径。','stream':'开启上游流，失败时映射错误响应，成功时创建流式响应体。','logAcceptedRequest':'记录已接受请求的标识、协议、模型和流式状态。','multipartEnvelope':'编码多部分表单为网关可传递的统一载荷。'})
add('GatewayApiKeyHashHelper.java',{'hashGatewayApiKey':'提取受支持请求头中的网关密钥并计算统一密钥哈希。','hashBearerToken':'通过统一提取逻辑计算 Authorization Bearer 密钥哈希。','extractGatewayApiKey':'优先使用 Bearer 密钥，否则使用非空 x-api-key，请求缺少密钥时拒绝。','extractBearerTokenOrNull':'大小写无关地识别 Bearer 前缀并提取非空密钥，对无效头格式抛出参数错误。'})
add('ApiCredentialHttpConverter.java',{'toCreateCommand':'将名称、模型组和额度请求转换为创建密钥应用命令。','toRenameCommand':'将当前用户、密钥标识和新名称转换为密钥重命名命令。','toChangeModelGroupCommand':'构造密钥切换模型组的应用命令。','toChangeTokenLimitCommand':'将请求额度转换为令牌上限值对象并构造修改命令。','toChangeStatusCommand':'构造密钥启用或禁用所需的状态变更命令。','toResponse':'将密钥或密钥用量视图映射为响应，包含额度、累计及当日用量和受限模型。','toRevealResponse':'将授权揭示的密钥材料映射为明文揭示响应。','toCreateResponse':'将新密钥领域数据和生成的明文材料组合为创建响应。','toModelWhitelist':'将模型名称列表去重并转换为模型白名单值对象。'})
add('ModelGroupHttpConverter.java',{'toCreateCommand':'将模型组名称、白名单及日限额转换为创建命令，并绑定所有者与模型组标识。','toUpdateCommand':'将模型组配置转换为带所有者约束的更新命令。','toResponse':'映射模型组配置与有效模型，补齐日用量并输出受限模型及统计时区。','toDailyLimits':'过滤空值与非正日限额，将模型名称和限额映射转换为领域对象。'})
add('ModelGroupController.java',{'listMyGroups':'解析当前用户并查询其模型组视图，转换为统一列表响应。','createGroup':'生成模型组标识，将请求转换为创建命令并返回新模型组视图。','updateGroup':'解析模型组路径标识和当前用户，转换并执行更新命令。','deleteGroup':'构造带当前用户身份的模型组删除命令并返回成功响应。'})
m.build(15,d,o)
