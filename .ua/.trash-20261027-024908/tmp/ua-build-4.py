import json,pathlib,math
ROOT=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api'); UA=ROOT/'.ua'; BATCH=4
b=json.loads((UA/f'tmp/batch-task-{BATCH}.json').read_text());d=json.loads((UA/f'tmp/ua-file-extract-results-{BATCH}.json').read_text())
assert d['scriptCompleted'] and not d['filesUnreadable']
file_info=[
('统一命名参数 JDBC 仓储的 Instant 与 Timestamp 转换，保留空值语义。',['JDBC','时间转换','工具函数']),
('实现渠道评测记录仓储，将领域模型与持久化对象互转，支持单条与批量保存及按渠道分页查询。',['仓储','渠道评测','持久化']),
('实现渠道评测调度仓储，支持按渠道查找调度、增改保存、查询到期任务及删除渠道调度。',['仓储','评测调度','持久化']),
('将评测模型与持久化对象互转，处理任务状态、分数报告 JSON、时间戳及运行时长；无效报告 JSON 转为显式转换错误。',['对象转换','渠道评测','JSON']),
('将评测调度与持久化对象互转，序列化模型名称列表并恢复 Cron、时区、触发时间与启用状态。',['对象转换','评测调度','JSON']),
('定义评测记录 Mapper 契约，覆盖插入、更新、按标识查询及按渠道筛选、排序和统计。',['接口','渠道评测','持久化']),
('定义评测调度 Mapper 契约，覆盖按渠道查询、到期调度查询、保存更新及按渠道删除。',['接口','评测调度','持久化']),
('通过命名参数 JDBC 读写评测记录，构建模型与状态筛选条件，采用白名单排序并执行分页及总数查询。',['JDBC','渠道评测','分页','排序']),
('通过命名参数 JDBC 读写渠道评测调度，按触发时间筛选和排序到期任务，并支持渠道级查询与删除。',['JDBC','评测调度','定时任务']),
('定义评测记录持久化对象，保存渠道、模型、触发方式、任务状态、评分报告、运行时长和审计时间。',['持久化对象','渠道评测','数据模型']),
('定义评测调度持久化对象，保存 Cron、时区、模型列表、启用状态及上次和下次触发时间。',['持久化对象','评测调度','数据模型']),
('生成 Jackson JDK8 类型支持模块，使 Optional 等类型可被统一 JSON 配置处理。',['配置','Jackson','序列化']),
('识别客户端主动断开的 I/O 异常，遍历原因链并检测常见连接重置、管道断开和容器断连异常。',['异常检测','流式传输','网络']),
('为普通 HTTP 接口统一映射认证、业务、参数绑定与校验异常；记录未预期异常的完整堆栈并返回通用服务器错误。',['异常处理','HTTP','参数校验']),
('提供管理员渠道评测接口，提交评测、查询历史、读取及更新 Cron 调度，并将协议请求组装为应用命令。',['HTTP','管理接口','渠道评测']),
('使用 MapStruct 组装评测请求命令与响应，处理 Cron 时区、分页排序、状态转换、分数 JSON 和审计时间。',['MapStruct','对象转换','评测调度','参数校验']),
('定义手动提交评测的可选模型列表请求，缺省模型选择由应用用例处理。',['DTO','渠道评测','请求模型']),
('定义评测调度更新请求，校验 Cron 非空并接收可选时区、模型集合及默认启用标记。',['DTO','评测调度','请求校验']),
('定义评测历史分页响应，返回记录列表、当前页、页大小和总记录数。',['DTO','渠道评测','分页']),
('定义单次评测响应，包含状态、触发方式、模型、评分报告、失败原因、运行时长及毫秒级时间。',['DTO','渠道评测','响应模型']),
('定义评测调度响应，返回标识、Cron、时区、模型集合、启用标记及触发和审计时间。',['DTO','评测调度','响应模型']),
('定义批量提交评测后的响应，封装本次创建的评测记录集合。',['DTO','渠道评测','批量操作']),
('定义评测历史查询参数，支持模型名、状态、受限页大小和排序字符串，默认第一页每页二十条。',['DTO','分页','查询参数']),
('将网关入口异常转换为 Messages 或 OpenAI 兼容错误结构，映射业务状态码并在流式客户端断开时停止生成响应。',['异常处理','网关','协议适配','流式传输']),
('解析分页排序字符串，兼容列表与数组输入、逗号合并和分号分隔，并校验排序字段与方向。',['参数解析','分页','排序','校验']),
('以不可变 record 封装分页结果并复制记录集合，校验页号、页大小和总数。',['分页','不可变数据','数据模型']),
('以不可变 record 定义页号与页大小，校验范围并计算 JDBC 分页偏移。',['分页','不可变数据','参数校验']),
('以不可变 record 表达排序字段和方向，缺省方向为升序，支持附加记录及判断空排序。',['排序','不可变数据','数据模型']),
('验证 Messages 与 Responses 桥接的推理重放、轮次阶段、MCP 配置与延迟加载、文本去重及拒绝结束原因。',['测试','协议转换','推理状态','MCP']),
('验证工具集桥接保持命名空间和成员身份，覆盖浏览器与计算机工具成员、延迟加载、参数缓冲及重复调用保护。',['测试','协议转换','工具集','命名空间']),
('覆盖统一流式转换适配器的跨协议 SSE 与 Bedrock 事件流行为，验证工具调用、推理签名、缓存用量、终止状态和截断流错误。',['测试','流式传输','协议转换','回归测试']),
('验证 AES-GCM API 密钥材料可用当前、旧版本和历史密钥解密，兼容已存明文，并在无匹配密钥时明确失败。',['测试','AES-GCM','密钥轮换','兼容性']),
('验证评测 HTTP 转换器对 Cron 时区的 UTC 缺省、空白清理和非法时区错误转换。',['测试','时区','对象转换']),
('验证网关异常处理按 Messages 或 OpenAI 格式返回认证、权限、限流和参数错误，并在流式断连时不返回错误体。',['测试','异常处理','协议适配'])
]
function_info={
0:{'timestamp':'将可空 Instant 转换为可空 JDBC Timestamp。','instant':'从 ResultSet 时间列读取可空 Timestamp 并转换为 Instant。'},
1:{'save':'将评测转为数据库对象，按标识是否存在选择插入或更新。','saveAll':'逐条保存传入的评测集合。','findById':'按评测标识查询数据库并恢复可空领域评测。','queryByChannel':'按渠道、模型、状态及分页排序查询评测记录，并单独统计总数。'},
2:{'save':'转换评测调度并按标识是否存在执行插入或更新。','findByChannelId':'按渠道标识加载并恢复评测调度。','findDueSchedules':'将给定时刻前到期的调度记录转换为领域模型。','deleteByChannelId':'删除指定渠道的评测调度配置。'},
3:{'toPO':'序列化评测状态和评分报告，生成数据库持久化对象。','toDomain':'解析评分报告并恢复标识、状态、时间和失败原因等评测字段。','writeScoreReport':'将可空评分报告序列化为 JSON，序列化失败时抛出带原因的转换异常。','readScoreReport':'将非空 JSON 解析为评分报告，解析失败时抛出带原因的转换异常。'},
4:{'toPO':'将 Cron、时区、模型列表 JSON 和触发时间转换为调度持久化对象。','toDomain':'从数据库记录恢复 Cron 调度、模型名称及触发时间。','writeModels':'将模型名称集合序列化为 JSON，失败时抛出带原因的转换异常。','readModels':'将模型名称 JSON 恢复为领域名称列表，空值返回空集合。'},
7:{'insert':'插入包含评分、失败原因和审计时间的完整评测记录。','update':'更新评测状态、探测任务、评分结果和运行时间。','selectById':'按标识查询单条评测记录。','selectByChannel':'构建模型与状态过滤，按受限排序字段和分页参数查询渠道评测记录。','countByChannel':'按与列表一致的模型和状态过滤统计渠道评测总数。','filter':'构建渠道条件及可选模型名与状态条件，使用命名参数绑定值。','orderBy':'将领域排序转换为 SQL 白名单列，缺省按创建时间和标识倒序。','columnName':'将受支持排序字段映射到数据库列，对未知字段抛出异常。','params':'绑定评测标识、状态、分数报告、运行时长及时间戳到 SQL 参数。'},
8:{'insert':'插入渠道评测 Cron 调度记录及触发时间。','update':'更新调度的 Cron、时区、模型、启用状态和触发时间。','selectById':'按调度标识读取单条记录。','selectByProviderChannelId':'按渠道标识读取其评测调度。','selectDue':'筛选已启用且下次触发时间到期的调度，按时间和标识排序并限制数量。','deleteByProviderChannelId':'删除指定渠道的调度记录。','params':'将调度标识、Cron、时区、模型 JSON 和触发时间绑定为命名 SQL 参数。'},
11:{'jdk8Module':'注册 Jackson Jdk8Module 以支持 Optional 等 JDK8 数据类型。'},
12:{'isClientDisconnect':'遍历可去重的异常原因链，通过容器异常类名及 I/O 错误消息判断客户端断连。','isDisconnectMessage':'将 I/O 错误消息归一化后匹配连接重置、管道断开和远端关闭等特征。'},
13:{'handleAuthenticationRequired':'将缺少认证异常映射为 HTTP 401 和统一认证错误码。','handleBusinessException':'将领域业务异常映射为 HTTP 400 并保留业务错误码与消息。','handleMethodArgumentNotValidException':'将请求体验证错误汇总为字段消息并返回 HTTP 400。','handleBindException':'汇总参数绑定字段错误并生成统一 HTTP 400 响应。','handleConstraintViolationException':'汇总约束违例路径和消息并返回 HTTP 400。','handleIllegalArgumentException':'将非法参数异常映射为统一 HTTP 400 响应。','handleHttpMessageNotReadableException':'将不可读取的请求体转换为统一参数错误响应。','handleHttpRequestMethodNotSupportedException':'将不受支持 HTTP 方法转换为统一参数错误响应。','handleException':'记录未预期异常完整堆栈并返回通用 HTTP 500 错误。'},
14:{'submit':'解析当前管理员身份，将可空请求体转换为提交命令并返回评测集合。','history':'将历史查询参数转换为命令，调用应用服务并组装分页响应。','loadSchedule':'按管理员身份和渠道标识加载调度，可空结果转换为响应。','upsertSchedule':'校验调度请求，构造更新命令并返回保存后的调度。'},
23:{'handleBusinessException':'结合请求 URI 识别客户端协议，将业务异常转换为对应错误结构和状态码。','handleRequestException':'将参数校验、请求绑定、请求体和参数异常统一转换为协议错误。','handleException':'检测流式客户端断连并停止响应，否则记录完整异常堆栈并返回 HTTP 500 协议错误。','errorResponse':'根据 Messages 或其他兼容入口构造不同错误对象和 HTTP 响应。','statusOf':'根据认证、权限、限流等业务错误码选择 HTTP 状态。','errorTypeOf':'将 HTTP 状态映射为协议所需的错误类型字符串。','messageOf':'优先采用非空异常消息，否则使用默认无效请求提示。'},
24:{'parse':'解析字符串或排序参数集合，支持逗号拆分和分号分隔并返回不可变 SortSpec。','append':'逐个解析排序片段，识别可选方向并创建排序记录。'},
25:{'PageResult':'校验分页结果参数并将记录集合复制为不可变列表。'},
26:{'PageSpec':'校验页号和每页数量均大于零。','offset':'根据一开始的页号计算数据库分页偏移。'},
27:{'SortSpec':'将排序记录列表规范化为空集合或不可变副本。','and':'在现有排序之后追加新的字段与方向记录。'},
31:{'test_returns_plaintext_when_material_was_encrypted_with_current_key':'验证当前密钥加密的材料可还原为原始明文。','test_returns_plaintext_when_material_was_encrypted_with_legacy_default_key':'验证旧默认密钥加密的材料仍可解密。','test_returns_plaintext_when_material_was_encrypted_with_previous_key':'验证配置历史密钥后可解密旧密钥材料。','test_returns_stored_plaintext_when_ciphertext_is_raw_api_key':'验证历史存储的原始 API 密钥按兼容规则直接返回。','test_throws_decryption_failed_when_no_key_matches':'验证没有任何匹配密钥时抛出密钥材料解密失败业务错误。','encrypt':'使用给定密钥构建 AES-GCM 加密材料作为测试样本。'},
32:{'test_defaultsToUtc_when_zoneIsMissing':'验证缺少 Cron 时区时命令使用 UTC。','test_trimsZone_when_zoneHasSurroundingWhitespace':'验证 Cron 时区字符串去除首尾空白后正确解析。','test_rejectsInvalidZone_when_zoneCannotBeParsed':'验证非法时区被转换为包含原始时区的明确参数异常。'}
}
test_texts={
28:['验证推理块重放时恢复原始 reasoning 项。','验证仅收到推理项完成事件时仍输出全部思考摘要。','验证响应 phase 保留轮次停止边界。','验证文本之后到达的 phase 仍决定停止原因。','验证旧 MCP 配置禁用服务器时移除该服务器工具。','验证旧 MCP 工具限制转换后保留允许列表。','验证嵌套 defer_loading 配置按缺省规则设置 MCP 加载行为。','验证工具加载模式混合时立即加载对应 MCP 服务器。','验证历史中的外部 redacted_thinking 不破坏对话内容。','验证消息项完成事件包含的多个文本部分全部输出。','验证文本部分与消息项均完成时每段文本仅输出一次。','验证多个摘要索引的思考内容全部保留。','验证最终事件省略 phase 时保留此前 commentary 阶段。','验证 commentary 含拒绝内容时保留 refusal 停止原因。'],
29:['验证带命名空间工具调用重放时保留工具集身份。','验证命名空间工具流式调用发出正确工具集名称。','验证默认计算机工具集公开预期成员。','验证禁用 zoom 时不公开该计算机工具成员。','验证分片工具参数被缓冲并在成员输入中正确输出。','验证默认浏览器工具集公开预期成员。','验证可选浏览器成员未明确启用时不公开。','验证明确启用后公开可选浏览器成员。','验证模型不支持工具搜索时取消嵌套函数延迟加载。','验证历史调用工具集任一成员后激活整个工具集。','验证同名成员不会误激活其他无关延迟工具。','验证会改变契约的不支持工具集配置被明确拒绝。','验证字符串形式文档来源保留正文。','验证不同工具集的同名同意图操作仍被区分。','验证同工具集相同输入重复调用触发重复调用保护。'],
30:['验证 Bedrock InvokeModel 原生事件转换为 Messages SSE 并累计用量。','验证 Bedrock 消息开始事件恢复客户端请求的模型名。','验证跨输入输出事件正确合并缓存与 Token 用量。','验证结构化输出包装数组被还原为客户端工具输入。','验证下游事件不泄漏 Bedrock 扩展元数据。','验证 Bedrock 流缺少 message_stop 时明确报错。','验证多个 Chat 工具调用各自拥有匹配的开始与结束块。','验证工具名迟到时缓冲已有 Chat 工具参数。','验证包含工具调用的流结束时返回 tool_use 停止原因。','验证过滤结束的流含工具调用时仍报告 tool_use。','验证 Chat 同时提供缓存写入字段时优先使用标准写入数量。','验证 Messages 未知停止原因降级为 Chat stop。','验证 Messages delta 缺少停止原因时仍正确结束 Chat 流。','验证 Messages 缓存读写用量映射到 Chat 用量明细。','验证工具未提供参数时输出空工具输入。','验证工具始终缺名时公布 unknown_tool 回退名称。','验证 Chat 转 Responses 发出完整生命周期事件。','验证首帧含完整工具参数时 Responses 完成项保留完整 JSON。','验证 Chat 工具名迟到后重放缓冲参数到 Responses。','验证 Chat 转 Responses 保留缓存用量并将长度截断标记为未完成。','验证 Messages 文本和工具调用生成 Responses 输出项。','验证 Messages 缓存用量和最大 Token 停止映射到 Responses。','验证文本之后到达的推理仍使用有效内容块索引。','验证 Responses 工具调用转换为 Messages SSE 的工具块与签名事件。','验证收到 Responses 终止事件后停止读取上游。','验证 Responses 自定义工具调用转换为 Messages 工具输入。','验证 Read 工具参数中空 pages 字段被移除。','验证 Responses 压缩事件生成可见文本和不透明思考状态。','验证压缩别名只在新增事件出现时仍输出压缩提示。','验证程序化工具调用保留调用者与服务器工具的关联。','验证 Responses 完成事件正确映射缓存读写用量。','验证仅收到文本完成事件时仍输出完整文本。','验证仅收到 completed 封套时恢复其中输出项。','验证 Responses 在终止事件前 EOF 时报错且不伪造成功结束。','验证 Responses 失败事件转为含上游原因的 I/O 错误。','验证 Messages 思考签名通过 Responses encrypted_content 隧道往返保留。','验证 redacted_thinking 块通过 Responses 桥接后保留不透明数据。','验证 Responses 桥接思考状态恢复原生签名和思考文本。','验证未知 Messages 内容块被忽略且不破坏正常文本。','验证 Chat 无名工具调用仍在 Responses 完成项中公布参数。','验证 Chat 工具参数截断为非法 JSON 时明确抛出 I/O 错误。'],
33:['验证 Messages 无效请求返回 invalid_request_error 和 HTTP 400。','验证无效 API 凭据返回 Messages 认证错误和 HTTP 401。','验证模型权限不足返回 Messages 权限错误和 HTTP 403。','验证配额耗尽返回限流错误和 HTTP 429。','验证模型每日调用额度耗尽返回 HTTP 429。','验证 Responses 无效请求使用 OpenAI 错误对象结构。','验证图像编辑无效请求使用 OpenAI 错误对象结构。','验证未预期异常返回 Messages 通用服务器错误。','验证流式客户端断开时不再返回错误响应体。']}
helpers={'convertRequest':'构建真实转换程序并将测试请求转换为目标协议。','convertResponse':'使用测试转换器将响应封套转换为目标协议。','convertStream':'运行流式转换并收集下游 SSE 事件作为断言输入。','convert':'构造指定源与目标协议的流式转换上下文并执行转换。','parseEvents':'解析 SSE data 行中的 JSON 事件。','response':'构建包含指定输出项的 Responses 测试封套。','request':'构建工具集与历史消息的协议转换测试请求。','deltaText':'汇总指定字段的增量事件文本。','rawClaudeToResponses':'构造原生 Messages 到 Responses 的流式转换测试。','adapter':'创建使用真实程序注册表与用量收集器的流式转换适配器。','context':'构建指定协议及模型信息的转换上下文。','dataEvents':'解析 SSE 数据行并过滤结束标记，返回 JSON 事件集合。','bedrockFrame':'将原生事件包装为 Bedrock event-stream 测试帧。','content':'按内容与角色构造协议桥接测试输入。'}
