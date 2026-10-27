import runpy
x=runpy.run_path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-build-10.py');globals().update({k:v for k,v in x.items() if not k.startswith('__')})
old=file_info
file_info=[
('将网关领域调用结果和可选原始上游响应映射为 HTTP 输出；同协议保留上游状态、正文和响应头，跨协议错误转换为对应错误体，并根据最后一次路由失败映射限流或超时状态。',['HTTP','响应映射','错误处理','网关']),
('携带客户端协议、HTTP 状态、协议错误类型和消息的流式网关异常，提供无效请求工厂方法。',['异常模型','流式传输','协议适配']),
old[1],old[2],old[3],
('验证网关协议操作的可用协议、计费属性、流式能力、multipart 表单接收及原生协议限制。',['测试','协议操作','计费','请求类型']),old[6],old[7],old[8],old[9],old[10],
('验证 Messages 与 Responses 双向请求和响应转换，覆盖工具与文件、推理签名、思考预算、模型能力、缓存用量、上下文压缩及停止原因。',['测试','协议转换','推理状态','兼容性']),
('验证 Messages 与 Responses 新能力桥接，覆盖程序化调用、压缩状态、提示缓存稳定性、自定义工具、评分失败及输出回退。',['测试','协议转换','程序化工具','缓存']),
('验证 Messages 用量提取器对标准字段、兼容别名、显式零值、缓存创建细分及压缩迭代用量的优先级规则。',['测试','用量统计','缓存','兼容性']),
('验证当前 Responses schema 对角色文本类型、预算下限、思考控制、输出格式、缓存开关和逐消息 effort 配置的规范化。',['测试','Responses','schema','请求规范化']),
('验证会话级 effort 配置跨 Messages 与 Responses 转换时的覆盖和继承规则，同时保留提示、格式、历史与原请求不变。',['测试','会话配置','推理配置','协议转换']),
('验证 JSON 模型重写器保留解析失败原因、区分参数错误与编程错误，并在 Bedrock 请求中保持模型字段省略。',['测试','模型映射','JSON','错误处理']),
('验证 multipart 表单 JSON 封套保留文本、重复字段和文件，并允许图像协议读取模型与 stream 字段及后续模型改写。',['测试','multipart','序列化','图像接口']),
('验证 Chat 与 Responses 双向请求转换，覆盖 Token 上限、文本与图像、工具重排、结构化输出、推理轮次状态及不完整历史修复。',['测试','协议转换','工具调用','推理状态']),
('验证 Chat 与 Responses 响应转换保留推理、文本和工具输出项，并正确表达长度截断与拒绝元数据。',['测试','响应转换','推理状态']),
('验证 Chat 到 Messages 请求与响应转换，覆盖多模态、工具并行结果、角色、缺省 Token、拒绝、缓存写入用量和消息标识。',['测试','协议转换','工具调用','用量统计']),
('验证图像响应中存在 Token 用量时可提取输入输出数量，缺少用量时保持未知状态。',['测试','图像生成','用量统计']),
('验证协议程序注册表的可执行矩阵、同协议与 Bedrock 程序、逐字段和流事件覆盖文档及相关 beta 语义。',['测试','转换注册表','字段映射','可追溯性']),
old[16],old[17],old[18],old[19],
('验证评测探测客户端按协议选择路径、鉴权、原生 Bedrock 正文和 Token 字段，并保持模型参数、Response 文本格式和采样兼容性。',['测试','渠道评测','HTTP','协议适配']),
('验证全局异常处理将业务、参数、不可读取正文、内部计量和未知错误映射为一致 HTTP 响应，并测试客户端断连的识别边界。',['测试','异常处理','客户端断连']),
('验证渠道评测管理接口的权限与校验边界：透传 null 请求到用例、非法状态、时区、空白 Cron 与非法表达式返回错误，并正确组装有效调度命令。',['测试','管理接口','渠道评测','请求校验']),
('验证图像编辑 HTTP 接口同时接受 multipart 和 JSON，规范化多图像与质量参数，保留重要表单字段，拒绝流式编辑并保持原始上游 JSON 响应。',['测试','图像编辑','multipart','HTTP'])
]
assert len(file_info)==len(d['results'])
test_lines.update({
5:'INVOKE 在全部协议上可用|图像编辑只在图像协议可用|生成图像操作需要计费|Token 计数操作不计费|图像变体操作不支持流式|编辑和变体操作接受 multipart 表单|非 INVOKE 操作要求原生协议',
10:'Messages 工具调用映射为 Chat 且保留服务层级|Base64 文档映射为 Chat 文件内容|无法原生表达的 MCP 工具集被明确拒绝|工具回复被移动到对应助手调用之后|缺少助手调用的孤立工具结果被丢弃|系统计费头被过滤且文本块正确分隔|多个助手文本块以空行分隔|无文本工具结果使用空结果占位|缺少数据的图像块被跳过|过小 Token 限制采用 Chat 最低值|预算推导 effort 最高为 high|推理模型缺省 effort 为 medium|助手调用工具时回放思考为 reasoning_content|助手未调用工具时不回放思考|无图像用户文本合并为字符串|含图像用户内容保留分块形式|工具结果图像并入用户消息|不支持的服务器工具历史被跳过|无托管搜索支持时移除服务器搜索工具|仅有服务器搜索工具时省略 tools 字段|未声明工具对应的特定选择被移除',
11:'流式 Messages 工具与工具结果映射为 Responses 请求|失败工具结果不错误标记完成状态|无操作的思考清理配置可兼容转换|非法思考保留策略被拒绝|Responses 无原生上下文编辑时在本地清理工具结果|Responses 非流式函数调用映射为 Messages|客户端未请求时不强制推理或详细程度|新版工具、文件和 MCP schema 得到映射|Base64 文档的媒体类型与上下文被保留|上传图像的 file_id 得到映射|内联文档内容被展开|思考显示被省略时不请求推理摘要|fast 模式被映射且未知字段被拒绝|旧输出格式与 priority 服务层级被映射|缺省 auto 服务层级被省略|仅含 schema 的输出格式被规范化|Responses 工具和推理保留到 Chat 响应|交错的助手文本与工具保持顺序|加密推理经思考签名可往返恢复|托管工具状态经不透明签名可往返恢复|工具前的助手文本保留 commentary 阶段|支持的模型映射延迟函数与工具搜索|加密推理回放保留 max effort 和全轮次上下文|外部不兼容思考状态被省略|预算推导 effort 最高为 high|旧目标模型将 max 降为 xhigh|新一代模型变体支持思考推理映射|新一代模型变体省略不支持采样字段|新一代模型保留 max effort|新一代模型重放推理时保留上下文|旧非推理模型拒绝思考配置|仅缓存预热请求无法保持语义时被拒绝|文本与工具交错响应保持输出项顺序|最大 Token 停止生成 incomplete_details|上下文窗口耗尽原因被保留|服务器工具循环暂停时生成完成响应|拒绝响应映射为 refusal 内容|原生思考签名通过 encrypted_content 传递|携带 Responses 签名的思考恢复加密内容|容器中的会话标识被保留|无法无损映射的未知内容块被拒绝|redacted_thinking 经加密内容桥接|压缩块映射为 compaction 项|缓存创建用量映射为缓存写入 Token|Responses 结构化输入直接映射为 Messages|桥接推理回放恢复原生思考块|外部 OpenAI 加密推理状态被丢弃|推理 effort 映射为思考预算|图像 data URI 映射为 Base64 来源|文件 data URI 与文件名正确映射|图像 file_id 映射为目标图像来源|原始 Base64 文件按扩展名推断媒体类型|JSON schema 文本格式映射为 output_config|带 previous_response_id 的状态请求被拒绝|Responses 响应中的桥接状态恢复为原生思考',
12:'直接调用限定的 allowed_callers 在不需要时省略|代码执行调用者映射为程序化工具调用|新一代模型变体保留程序化调用|工具结果回放保留程序调用者|目标不支持时省略实验缓存断点|程序调用未完成时拒绝非文本结果|程序输出映射为代码执行结果|追加后续轮次不改变提示缓存键|仅含可回放状态时映射 pause_turn|重放压缩状态时裁剪压缩前历史|原生压缩使用可读摘要|自定义工具调用与结果往返保持|Read 工具空 pages 参数被移除|显式缓存填充用量映射为缓存写入|无输出项时采用 output_text 回退|失败 Responses 响应转为明确转换错误',
13:'标准缓存读取字段显式零值优先|标准缓存读取缺失时采用 cached_tokens 别名|标准缓存读取优先于别名|缓存创建总量为零时求和明细|标准 Token 缺失时采用兼容别名|缺少标准输出字段时采用 completion_tokens|顶层用量优先于压缩迭代统计|缺少顶层用量时求和迭代 Token',
14:'助手历史文本使用 output_text|非助手历史使用 input_text|工具续轮保留各角色内容类型|历史搜索结果使用角色相关文本类型|助手压缩摘要使用 output_text|助手文本省略仅输入可用的缓存断点|小于 Responses 最小 Token 预算时明确拒绝|非推理模型关闭思考时省略 reasoning|禁用推理时省略 summary|仅有 effort 配置时保留旧 schema|不接受 schema 的输出格式只输出 type|显式缓存参数受上游能力开关控制|默认配置省略显式缓存参数|无法保留语义的模式被拒绝|未知 effort 值被拒绝|无法保留生命周期的临时系统消息被拒绝|逐消息 effort 随新增用户输入正确生效',
15:'顶层未指定推理时采用消息 effort|协议不变时保留原生配置历史|旧 adaptive 模型无思考覆盖时保留采样|现代目标采用 adaptive 思考|支持新等级的目标保留 xhigh|adaptive 思考目标省略采样覆盖|跨轮次配置解析当前有效 effort|仅含 effort 的系统消息不破坏提示文本|携指令的 effort 更新保留系统文本|有效 effort 覆盖仍保留输出格式|解析 effort 不修改调用方请求|旧 Responses 目标将 max 映射为支持等级|无法映射的消息配置被拒绝|非系统角色携带配置被拒绝|消息无覆盖时保留基础 effort|配置历史采用最新更新|消费配置项后保留对话内容|非法 Responses 配置更新被拒绝',
16:'JSON 格式错误保留解析异常原因|非对象 JSON 保留参数校验错误|解析器编程错误向外传播|Bedrock InvokeModel 不写入正文模型字段|普通 JSON 协议正确重写 model',
17:'编码后文本字段位于封套顶层|文本 true 的 stream 字段编码为布尔值|重复同名表单字段合并为数组|封套解码保留文本与文件|图像协议可从封套读取 model 和 stream|模型重写后封套保留改写模型|非对象封套被拒绝',
18:'Chat 系统消息映射为 Responses 系统输入项|两个 Token 限制同时存在时优先 max_completion_tokens|推理模型省略不支持采样参数|助手 reasoning_content 包装为思考标签|工具调用映射为 function_call 项|空工具结果映射为 function_call_output|Chat 工具定义被展开为 Responses schema|旧 functions API 被转换为工具定义|特定工具选择被展开|reasoning_effort 映射为推理配置|JSON schema 响应格式被展开|图像内容块正确映射|不支持内容类型被明确拒绝|Chat 流式工具调用请求可被转换|Responses instructions 映射为系统消息|小输出预算采用目标最低完成 Token 数|流式请求添加 stream_options|函数调用前推理附加到工具调用助手消息|连续函数调用合并为一个助手轮次|截断的工具参数调用被移除|无回复的工具调用从历史清理|函数工具重新嵌套且移除服务器工具|已删除工具的选择被移除|仍存在工具的特定选择被重新嵌套|JSON schema 文本格式恢复为 Chat 响应格式|推理 effort 恢复为 Chat 字段|developer 角色映射为 system|用户图像块被保留|服务器网页搜索轨迹被跳过|同轮链式工具调用复用轮次推理|文本助手消息接收前置推理|新用户消息清除旧轮次推理|裸字符串输入项映射为用户消息',
19:'Chat 推理文本与工具调用生成完整 Responses 输出项|长度截断标记为 Responses incomplete|Responses 拒绝元数据映射到 Chat',
20:'Chat 基础文本映射为 Messages|developer 角色映射为系统指令|函数工具定义正确映射|工具调用与结果正确映射|图像 data URL 转为 Base64 来源|外部图像 URL 被保留|停止序列正确映射|特定工具选择正确映射|不支持内容类型被明确拒绝|多个系统消息被合并|工具调用要求得到满足|Chat 响应数组文本保持完整|仅有推理时将其作为可见文本返回|含工具调用的 stop 正确变为 tool_use|过滤拒绝映射为拒绝内容|标准缓存写入字段优先于别名|缺少 Token 限制时采用缺省值|并行工具结果归入同一用户轮次|未声明对应工具时移除选择|空 Chat 响应标识触发消息标识生成|过滤响应中有工具时仍使用 tool_use',
21:'图像响应有用量时提取输入输出 Token|图像响应缺少用量时保持未知状态',
22:'Responses 相关转换组合均登记|Bedrock 的 request response 与 stream 程序可执行|全部受支持协议组合均有可执行程序|同协议配置使用 Identity 程序|映射文档包含运行时语义字段|Responses 与 Messages 流文档覆盖可执行事件|映射文档覆盖流式完整性增强字段|映射文档包含 Bedrock beta 语义',
23:'Chat 在 DONE 前 EOF 时明确失败|Responses 在终止事件前 EOF 时明确失败|未识别终态不被自动补成成功|Responses done 哨兵被视为终止|完成帧仅有 response_id 时仍恢复身份|裸用量帧填充累计用量|不完整帧保留响应标识并结束为长度限制|完成后附加用量帧仍可读取|超长 SSE 数据行被明确拒绝',
24:'Messages 显式零缓存读取优先于别名|Chat 缓存别名累积为统一读取写入字段|Messages 顶层缓存创建为零时求和明细|Responses cached_tokens 与 cache_read 别名均可读取|Responses reasoning_tokens 映射为推理用量|Chat 首个选择缺少文本时读取后续选择|Messages 缺少用量时保持未知',
25:'仅提供原生封套时不复用陈旧原始请求|同协议 Bedrock 编码器重建请求正文|Bedrock 仅原生响应仍解包结构化输出|Bedrock 仅原生流仍解包结构化数组|Bedrock 仅原生流保留客户端模型|只有原生投影且出现 Bedrock 异常时明确失败|同协议 Chat 规范化请求 Token 字段|未改动且有合法原始载荷时允许透传',
27:'OpenAI 兼容 Chat 请求正确解析|Messages 请求保留模型参数|Bedrock 模型探测使用 InvokeModel 正文与鉴权|Bedrock 探测模型列表使用模型摘要路径|Responses text 配置被规范化|缺省兼容请求使用 max_completion_tokens|Responses 请求仅使用其原生 Token 限制',
28:'业务错误返回 HTTP 400 及原始业务码|非法参数返回 HTTP 400 而非服务器错误|未知异常返回通用 HTTP 500|内部计量业务错误返回 HTTP 500|缺失不可读请求体使用友好提示|嵌套管道断开异常被识别|业务错误包装 I/O 异常不误判断连|普通应用异常不误判断连',
29:'空请求体到达用例而非触发控制器空指针|缺省状态映射为可空领域状态|非法状态返回 HTTP 400|非法时区返回 HTTP 400|空白 Cron 在校验阶段被拒绝|非法 Cron 表达式返回业务错误|有效调度提交类型化命令',
30:'multipart 图像编辑字段被编码为网关请求封套|JSON 图像编辑将单输入规范化为输入数组|JSON 图像编辑将字符串质量参数规范化|图像编辑流式请求被拒绝|图像编辑保留原始上游 JSON 响应'
})
function_info={
0:{'toRawResponse':'根据领域调用状态与原始上游响应选择透传、协议转换或合成错误响应，并保留上游限流元数据。','mapProviderResponse':'将上游正文、状态、响应头和内容类型映射为网关原始响应。','mapProviderFailure':'同协议上游错误原样返回，跨协议时按客户端格式重建错误体并保留状态和头。','upstreamErrorMessage':'从 error.message、message、detail 或 error 提取上游原因，非 JSON 错误保持原文。','buildClaudeUpstreamErrorBody':'将上游 HTTP 状态映射为 Messages 错误类型。','mapSuccessResponse':'要求存在非空响应转换结果，并按调用是否流式选择 JSON 或 SSE 内容类型。','mapFailureResponse':'根据领域错误映射状态与协议错误体，并保留失败尝试的上游响应头。','mapErrorToHttpStatus':'结合调用错误类别与最后路由失败区分认证、限流、超时、不可用和网关错误。','buildClaudeErrorBody':'按认证、权限、限额、转换或上游失败生成 Messages 错误对象。','mapErrorTypeToOpenAIType':'将领域错误转换为 OpenAI 兼容错误类型，区分上游限流。'},
1:{'GatewayStreamingProtocolException':'构造携带协议、HTTP 状态和协议错误类型的运行时异常。','invalidRequest':'创建指定客户端协议的 HTTP 400 无效请求异常。'},
2:{'toStreamingResponse':'封装流式回调，协调首字节与空闲超时、SSE 保活、用量记录、连接租约和流清理。','streamInterrupted':'识别可保留的已知流异常，其他异常包装为流中断业务错误。','keepAliveTicker':'创建按固定间隔运行的 SSE 保活调度器，并在流结束后取消任务。'},
3:{'hasImageTool':'检查 Responses 载荷是否声明 image_generation 工具。','invoke':'解析图像工具请求和预算，先规划再按允许的图像调用次数执行生成或编辑，累计用量并返回 Responses 或 SSE。','planResponses':'规范化规划载荷、调用网关并计量规划结果，上游规划失败时返回明确协议错误。','invokeImagesOnce':'按输入图像决定生成或编辑，选择可执行原生操作，构造调用命令并记录图像用量。','imageBody':'合并图像模型、提示词、数量和工具覆盖参数。','binaryField':'把图像 data URI 转换为 multipart 文件，远程 URL 保持文本字段。','planningBody':'为图像规划清理旧会话状态并写入工具选择、模型和安全 Token 预算。','applyImagePlannerInstructions':'向规划指令加入图像工具执行约束与提示整理要求。','usageFrom':'从规划响应中提取可用的 Token 用量与细分字段。','responseEnvelope':'组装 Responses 输出列表、用量、状态、图像结果与规划文本。','sseStream':'由最终响应封套生成图像与文本的 Responses 生命周期 SSE 事件。'},
4:{'from':'解析图像工具、工具选择、提示、图像输入与覆盖参数，限制执行预算并拒绝不可用 mask。','rejectRemoteMask':'拒绝普通 HTTP URL mask，允许可解析的内联图像数据。','normalizeOverrides':'规范化图像参数键并复制工具覆盖配置。','firstImageTool':'从单个工具或工具数组找到首个图像生成工具。','copied':'提取并复制受支持的图像工具参数。','text':'从字符串或多模态输入块提取并组合提示文本。','imageInputs':'收集图像输入，优先规范化可供图像编辑调用的 URL 与 data URI。','addContentImages':'遍历多模态内容，提取直接或嵌套的图像来源。'},
26:{'readHandlesLfAndCrLfLineEndings':'验证 SSE 读取器兼容 LF 与 CRLF 换行并正确识别事件边界。','readProcessesLastLineWithoutTrailingNewline':'验证最后一行没有终止换行时仍处理其内容。','readJoinsMultiLineDataWithNewlines':'验证同一 SSE 事件的多条 data 行以换行连接。','readDispatchesEmptyDataEvent':'验证空 data 也被发布为合法事件。','readKeepsMultibyteUtf8WhenReaderReturnsTinyChunks':'验证极小分片读取不会破坏多字节 UTF-8 字符。','readRejectsOversizedLine':'验证单行超过限制时明确失败。','readSupportsBareCarriageReturnLineEndings':'验证单独 CR 也能作为换行符。'}}
for i,text in test_lines.items():
 fs=[z for z in d['results'][i].get('functions',[]) if z['name'].startswith('test_')];ts=text.split('|');assert len(fs)==len(ts),(i,len(fs),len(ts));function_info.setdefault(i,{}).update({z['name']:'验证'+t+'。' for z,t in zip(fs,ts)})
