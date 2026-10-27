import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'ProtocolConversionApplicationService.java':('校验后台管理员权限后查询协议转换定义、按源目标协议读取详情或修改启停状态，并持久化状态变化。',['应用服务','转换管理','权限校验']),
'ChangeProtocolConversionStatusCommand.java':('承载操作者与协议转换定义身份，用于后台启用或禁用转换。',['状态命令','协议转换','管理员操作']),
'ConversionCapability.java':('描述转换对流式、工具、推理、用量、缓存令牌及内容类型的支持能力，提供全能力和受限能力构造。',['转换能力','内容类型','值对象']),
'ConversionDefinition.java':('包装协议转换定义并向兼容调用方提供身份、能力、映射与启停操作，同时支持恢复完整状态。',['兼容封装','转换定义','状态管理']),
'ConversionImplementationStatus.java':('枚举转换实现程度，并区分可以参与路由的实现状态。',['实现状态','枚举类型','路由能力']),
'ConversionStatus.java':('枚举转换定义启用、禁用和未实现等管理状态。',['转换状态','枚举类型','启停管理']),
'MappingDocument.java':('描述请求或响应方向的映射说明、标题、概要和有序字段规则，并校验必要文本。',['映射文档','字段规则','数据校验']),
'MappingMetadata.java':('对映射文档提供兼容包装与双向转换，保持方向、标题、概要及字段映射集合。',['兼容封装','映射元数据','字段规则']),
'ProtocolConversionDefinition.java':('定义源目标协议的转换聚合及能力、请求响应映射、实现程度和启停状态，禁止启用未实现转换并校验映射方向。',['转换聚合','路由约束','状态管理']),
'ProtocolConversionDefinitionId.java':('封装有效的协议转换定义标识，供仓储和管理接口定位配置。',['定义标识','值对象','身份校验']),
'ProtocolConversionId.java':('提供兼容转换标识封装，并可转换为当前定义标识类型。',['兼容标识','值对象','类型转换']),
'ProtocolConversionDefinitionRepository.java':('定义协议转换定义列表、标识及协议对查询和保存契约。',['仓储接口','协议转换','持久化契约']),
'ProtocolConversionRepository.java':('定义旧版转换定义包装的保存与查询接口，维持转换管理兼容边界。',['兼容接口','协议转换','仓储契约']),
'AdminProtocolConversionController.java':('提供协议转换列表、标识详情、源目标协议对查询和启用禁用的后台 HTTP 接口。',['管理接口','协议转换','启停控制']),
'ProtocolConversionHttpConverter.java':('使用 MapStruct 将转换定义及能力和字段映射转换为管理响应，处理协议字符串、状态命令与枚举集合。',['对象转换','能力展示','映射说明']),
'ProtocolConversionCapabilityResponse.java':('定义转换能力响应，包含流式、工具、推理、用量、缓存和内容类型支持。',['响应模型','转换能力','内容类型']),
'ProtocolConversionFieldMappingResponse.java':('定义字段映射响应，包含源目标路径与类型、规则、损失、必填支持情况、默认值和条件。',['响应模型','字段映射','转换损失']),
'ProtocolConversionListItemResponse.java':('定义转换列表条目的协议对、转换类别、启停和实现状态及核心能力摘要。',['响应模型','转换列表','能力摘要']),
'ProtocolConversionListResponse.java':('包装协议转换列表条目，供后台列表接口输出。',['响应模型','列表封装','协议转换']),
'ProtocolConversionMappingResponse.java':('定义映射文档响应，包含请求响应方向、标题、概要和字段规则列表。',['响应模型','映射文档','字段规则']),
'ProtocolConversionResponse.java':('定义协议转换详情响应，组合协议对、状态、能力、请求响应映射与审计时间。',['响应模型','转换详情','能力展示']),
'ProtocolConversionDefinitionPersistenceConverterTest.java':('验证缺少执行转换程序时生成明确不可用的字段规则，同协议定义则回退为完整载荷透传规则。',['持久化转换测试','缺失程序','透传回退'])}
d.update({
'ProtocolConversionDefinitionRepositoryImpl.java':('通过定义映射器保存和读取协议转换聚合，按记录存在性选择插入或更新，支持按标识和源目标协议查询。',['仓储实现','协议转换','持久化映射']),
'JdbcProtocolConversionDefinitionMapper.java':('使用具名参数 JDBC 持久化协议转换状态、实现程度、能力和双向映射 JSON，并支持按协议对查询与排序列表。',['数据访问','协议转换','参数化查询']),
'ProtocolConversionDefinitionMapper.java':('定义转换配置的数据库插入更新、按标识及协议对查询和全部列表接口。',['数据访问接口','协议配置','持久化契约']),
'ProtocolConversionDefinitionPO.java':('保存转换定义表的协议对、类别、状态、实现程度、能力开关、请求响应映射 JSON 及审计时间。',['持久化对象','转换配置','映射存储'])})
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('ProtocolConversionApplicationService.java',{'listDefinitions':'验证操作者具备后台权限后查询全部协议转换定义。','getDefinition':'按转换标识或源目标协议对查询定义，缺失时报告业务错误。','enable':'校验管理员权限，读取转换定义并执行领域启用规则后保存。','disable':'校验管理员权限，读取转换定义并执行禁用操作后保存。'})
add('ConversionCapability.java',{'full':'创建支持流式、工具、推理、令牌映射和全部内容类型的转换能力。','limited':'按传入能力开关及内容类型集合创建受限转换能力。','supportsContentType':'判断给定内容类型是否位于该转换支持的内容类型集合中。'})
add('ProtocolConversionDefinition.java',{'create':'根据身份、源目标协议、能力和双向映射创建转换定义，并校验必需字段及映射方向。','rehydrate':'从保存的启停状态、实现程度和审计时间恢复完整转换聚合。','enable':'已启用时保持幂等；拒绝未实现转换，否则启用并更新时间。','disable':'已禁用时保持幂等，否则禁用并更新时间。','isEnabledForRouting':'仅当定义启用且实现状态可路由时允许参与候选选择。','matches':'判断转换定义的源协议与目标协议是否与指定协议对一致。','supportsStreaming':'读取定义能力中的流式支持标志。','supportsToolCalling':'读取定义能力中的工具调用支持标志。','isPassthrough':'判断定义是否为同协议透传。','isTransform':'判断定义是否为跨协议转换。'})
add('ConversionDefinition.java',{'create':'将兼容标识及映射包装转换为当前定义类型并创建兼容转换对象。','rehydrate':'恢复底层转换定义及审计和启停状态，再包装为兼容转换模型。','fromDefinition':'将当前转换定义包装为兼容模型，保留全部领域语义。','enable':'委托底层转换定义执行启用及未实现约束。','disable':'委托底层转换定义禁用转换。'})
add('MappingDocument.java',{'of':'校验映射方向、标题、概要及字段规则，并构建不可变映射文档。','fieldMappingCount':'返回映射文档的字段规则数量。'})
add('MappingMetadata.java',{'fromDocument':'将映射文档包装为兼容元数据对象。','of':'构建包含映射方向、标题、概要与字段规则的兼容映射元数据。'})
add('ProtocolConversionHttpConverter.java',{'toChangeStatusCommand':'将当前操作者及转换定义身份组合为启停命令。','toProtocolType':'将协议字符串归一为大写下划线格式后解析协议枚举。','toListResponse':'转换定义列表为管理列表条目并包装列表响应。','toFieldMappingResponses':'逐个映射字段转换规则，保留路径、类型、约束和损失说明。','toEnumNames':'将枚举集合映射为保持插入顺序的名称集合。'})
add('AdminProtocolConversionController.java',{'listDefinitions':'读取会话操作者并返回所有协议转换定义的列表响应。','getDefinitionById':'将路径标识转换为定义身份并读取管理详情。','getDefinition':'解析源目标协议参数并读取匹配转换定义的详情。','enable':'绑定操作者与定义标识，执行启用命令并返回更新详情。','disable':'绑定操作者与定义标识，执行禁用命令并返回更新详情。'})
add('ProtocolConversionDefinitionPersistenceConverterTest.java',{'test_fallbackMapping_when_noConverterRegistered':'未注册转换执行程序时，验证字段映射明确标记不可用且不声称支持。','test_passthroughFallback_when_sameProtocol':'源目标协议相同时，验证缺少程序仍生成 payload 到 payload 的透传映射。','po':'构造不同协议对和映射描述的持久化测试数据。'})
add('ProtocolConversionDefinitionRepositoryImpl.java',{'save':'将领域定义映射为持久化对象，根据标识是否存在执行插入或更新。','findById':'按转换定义标识查找配置并恢复领域聚合。','findBySourceAndTarget':'按源协议和目标协议读取配置并恢复领域聚合。','findAll':'读取全部转换配置并映射为领域定义列表。'})
add('JdbcProtocolConversionDefinitionMapper.java',{'insert':'插入协议对、启停与实现状态、能力开关及双向映射 JSON 和审计时间。','update':'按标识更新启停状态、实现程度、能力开关和映射 JSON。','params':'将转换持久化对象展开为具名 SQL 参数，转换审计时间。'})
a='backend/src/main/java/com/api2api/'
o['_edges']=[('class:'+a+'infr/repository/protocol/ProtocolConversionDefinitionRepositoryImpl.java:ProtocolConversionDefinitionRepositoryImpl','class:'+a+'domain/protocol/repository/ProtocolConversionDefinitionRepository.java:ProtocolConversionDefinitionRepository','implements',0.9),('class:'+a+'infr/repository/protocol/mapper/JdbcProtocolConversionDefinitionMapper.java:JdbcProtocolConversionDefinitionMapper','class:'+a+'infr/repository/protocol/mapper/ProtocolConversionDefinitionMapper.java:ProtocolConversionDefinitionMapper','implements',0.9)]
m.build(20,d,o)
