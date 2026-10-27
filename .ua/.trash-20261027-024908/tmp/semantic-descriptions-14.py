import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'ProtocolMetadataApplicationService.java':('校验管理员访问权限后提供协议元数据列表、协议类型查询和标识查询，并将缺失记录转换为业务错误。',['应用服务','协议元数据','权限校验']),
'ProtocolContractViolationException.java':('表示协议请求、响应或字段不满足契约结构时的异常，保留底层解析原因。',['协议契约','校验异常','错误传播']),
'FieldSection.java':('枚举协议字段的基础、消息、内容块、工具、推理、流式及用量等分组和展示标签。',['字段分组','枚举类型','协议元数据']),
'FieldType.java':('枚举协议字段的结构类型，供契约字段校验和元数据展示共同使用。',['字段类型','枚举类型','结构校验']),
'ProtocolFieldDefinition.java':'定义协议字段身份、名称、路径、数据类型、必填性、分组、输入输出方向、说明、用途和排序信息。',
'ProtocolFieldDefinitionId.java':'以非空标识值封装协议字段定义身份，提供受控创建和数值访问。',
'ProtocolMetadata.java':'封装协议名称、规范版本、默认端点及字段定义，支持字段总数、方向筛选和分组查询。',
'ProtocolMetadataId.java':'以非空标识值封装协议元数据身份，供仓储与管理接口定位协议记录。',
'UsageDirection.java':('区分协议字段用于输入、输出或双向交互。',['使用方向','枚举类型','协议字段']),
'ProtocolMetadataRepository.java':('定义协议元数据的列表、标识及协议类型查询契约。',['仓储接口','协议元数据','查询契约']),
'ProtocolContract.java':('将协议元信息、可执行字段与请求、响应及流事件形状结合，校验 JSON 并解析模型、流式与工具推理需求。提供避免逐字段遍历的网关请求快速解析路径。',['协议契约','请求解析','结构校验']),
'ProtocolContractDefinitions.java':('集中创建五类协议的契约定义、官方版本及请求响应字段，作为可执行结构与管理元数据的共同来源。',['协议定义','字段清单','契约注册']),
'ProtocolFieldRef.java':('定义可执行协议字段引用，支持点路径、数组展开、值筛选、流事件嵌套回退、字段写入和类型必填校验。',['字段引用','路径解析','类型校验']),
'ProtocolShape.java':('根据请求、响应或流事件种类校验 JSON 对象及其字段，并按字段路径获取必需的契约引用。',['协议形状','字段校验','结构解析']),
'ProtocolMetadataRepositoryImpl.java':('从协议契约注册表投影协议及字段元数据，生成稳定标识和排序，并建立按协议类型与标识查询的内存索引。',['仓储实现','契约投影','内存索引']),
'AdminProtocolMetadataController.java':('提供管理员协议元数据列表和详情 HTTP 接口，解析当前操作者并将领域模型转换为统一响应。',['管理接口','协议元数据','协议适配']),
'ProtocolMetadataHttpConverter.java':('将协议及字段元数据转换为列表和详情响应，保持固定字段分组顺序与组内排序，并输出输入输出字段数量。',['响应转换','字段分组','协议元数据']),
'ProtocolFieldDefinitionItemResponse.java':('定义管理接口中的协议字段详情，包括路径、类型、必填性、方向、描述与排序。',['响应模型','协议字段','数据传输']),
'ProtocolFieldSectionResponse.java':('定义协议字段分组响应，包含分组标签、字段数量和字段详情列表。',['响应模型','字段分组','数据传输']),
'ProtocolMetadataDetailResponse.java':('定义协议元数据详情响应，包含版本、默认端点、字段分组、输入输出计数与时间戳。',['响应模型','协议详情','数据传输']),
'ProtocolMetadataListItemResponse.java':('定义协议列表条目，包含协议标识、版本、默认端点及字段数量摘要。',['响应模型','协议列表','数据传输']),
'ProtocolMetadataListResponse.java':('包装协议元数据列表，作为管理员列表接口的数据结构。',['响应模型','列表封装','数据传输']),
'ProtocolContractRegistryTest.java':('验证协议注册、官方字段与版本、请求解析、工具和推理信号识别，以及契约字段和元数据投影的一致性。',['协议契约','回归测试','行为验证']),
'ProtocolMetadataRepositoryImplTest.java':('验证仓储从协议契约投影完整字段，保持内容块分组并包含延迟工具等最新字段。',['仓储测试','元数据投影','回归测试']),
'ProtocolMetadataHttpConverterTest.java':('验证协议元数据详情转换保留全部字段，并将内容块分组安排在消息分组之后。',['转换测试','字段分组','回归测试'])}
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('ProtocolContract.java',{'parseRequest':'按完整请求形状验证 JSON，并提取模型、流式与工具推理能力信号。','parseGatewayRequest':'解析 JSON 后直接提取网关路由事实，避免对大型历史记录逐个元数据字段遍历。','requestFacts':'从模型、流式字段和消息内容中计算路由所需的工具调用与推理能力。','parseRequestNode':'读取 JSON 并执行请求形状的字段类型和必填校验。','parseResponse':'读取 JSON 并按响应形状校验字段结构。','buildResponse':'验证响应符合协议契约后返回原始响应内容。','readObject':'拒绝空白载荷并将 JSON 解析失败转换为协议契约异常。','contentSignals':'一次遍历消息或响应输入项及内容块，识别工具调用、工具结果、思考和加密推理信号。'})
add('ProtocolFieldRef.java',{'readAll':'解析字段路径并展开数组，支持等值筛选和流事件中的嵌套响应容器回退。','read':'读取字段路径的首个匹配值，未找到时返回缺失节点。','write':'按规范化路径向协议 JSON 对象写入字段值。','validate':'校验字段必填约束及实际 JSON 值是否满足声明类型。','matchesType':'根据字段类型判断 JSON 节点是否满足字符串、数值、数组或对象等结构要求。','normalizedPath':'按请求、响应及流事件形状归一化字段路径，处理协议包装结构。'})
add('ProtocolMetadata.java',{'fieldsBySection':'按字段所属区域分组，供协议元数据详情展示。','inputFields':'筛选用于输入或双向交互的协议字段。','outputFields':'筛选用于输出或双向交互的协议字段。','fieldCount':'返回当前协议定义的字段总数。'})
add('ProtocolMetadataHttpConverter.java',{'toListResponse':'将协议元数据集合转换为管理员列表响应。','toListItemResponse':'映射协议版本、端点及字段计数到协议列表条目。','toDetailResponse':'按固定分组顺序转换协议详情，并输出方向计数和毫秒时间戳。','toSectionResponse':'按字段排序号转换组内字段列表及分组标签。','toFieldResponse':'完整映射协议字段的路径、类型、约束、用途和展示顺序。'})
add('ProtocolContractDefinitions.java',{'create':'建立五类协议的请求、响应与流事件字段清单及官方版本信息，返回统一契约集合。'})
add('ProtocolMetadataApplicationService.java',{'listAll':'验证操作者具备后台访问权限后查询所有协议元数据。','getByProtocolType':'验证后台权限并按协议类型查找元数据，缺失时抛出业务异常。','getById':'验证后台权限并按标识查找协议元数据，缺失时抛出业务异常。'})
add('ProtocolShape.java',{'parse':'确认载荷为 JSON 对象，并依次验证形状字段的必填性和类型。','requireField':'按路径查找形状字段，不存在时拒绝无效字段引用。'})
a='backend/src/main/java/com/api2api/'
o['_edges']=[('class:'+a+'infr/repository/protocolmetadata/ProtocolMetadataRepositoryImpl.java:ProtocolMetadataRepositoryImpl','class:'+a+'domain/protocolmetadata/repository/ProtocolMetadataRepository.java:ProtocolMetadataRepository','implements',0.9)]
m.build(14,d,o)
