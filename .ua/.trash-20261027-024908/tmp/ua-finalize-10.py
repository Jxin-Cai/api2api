import pathlib
namespace={};p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-complete-10.py');exec(p.read_text().split('for i,text in test_lines.items():')[0],namespace);globals().update({k:v for k,v in namespace.items() if not k.startswith('__')})
file_info[1]=('封装客户端协议、HTTP 状态与错误类型的网关运行时异常，并提供无效请求工厂方法。',['异常模型','HTTP','协议适配'])
file_info[2]=('将网关流式调用转换为 StreamingResponseBody，按是否跨协议选择转换或透传并提取用量；跟踪首个语义 Token，区分客户端断连与上游中断并完成调用计量。',['流式传输','SSE','协议适配','用量统计'])
file_info[4]=('解析 Responses 图像工具请求、工具选择、调用预算与多模态历史；为规划器生成兼容函数定义，并将既有图像结果还原为可编辑图像输入。',['Responses','图像生成','请求解析','工具选择'])
file_info[22]=('验证 Responses 请求读取器支持字符串或 input_text 输入，并校验必须存在模型。',['测试','Responses','请求解析'])
file_info[23]=('验证 Responses 非流式用量提取器在标准缓存字段缺失时采用兼容别名，并按标准字段优先级处理显式零值。',['测试','Responses','用量统计','缓存'])
file_info[24]=('验证协议程序注册表登记可执行矩阵、Bedrock 与 Identity 程序，并提供覆盖运行时字段、流事件及 beta 语义的映射文档。',['测试','转换注册表','字段映射','可追溯性'])
file_info[25]=('验证同协议流透传器按原始字节转发内容，并提取 SSE 或 Bedrock 累计用量，处理多行事件及先接收用量再发生 I/O 错误的情况。',['测试','流式传输','透传','用量统计'])
file_info[26]=('验证 Messages 网关请求从历史工具调用、思考签名及输出 effort 中推导工具、推理和用量能力要求，以约束可用路由。',['测试','Messages','能力检测','路由'])
file_info[27]=('验证调用结果映射器保留同协议上游错误状态与 Retry-After，将跨协议错误转换为客户端格式，并按路由限流结果推导错误状态。',['测试','响应映射','错误处理','限流'])
file_info[28]=('验证共享错误体构造器生成 Messages 原生错误结构或 OpenAI 兼容错误结构。',['测试','错误处理','协议适配'])
file_info[29]=('验证流式响应映射的字节透传、响应头、首 Token 标记、协议转换选择、失败计量与断连处理，以及多种协议的流式错误帧。',['测试','流式传输','响应映射','错误处理'])
file_info[30]=('覆盖 Responses 图像桥接的规划与生成路由、参数覆盖、编辑输入、SSE、用量审计、调用预算及工具选择；验证不支持的路径、取消和恢复错误。',['测试','Responses','图像生成','工具调用'])
function_info[1]={'GatewayProtocolException':'构造携带客户端协议、HTTP 状态和错误类型的网关异常。','badRequest':'创建指定协议的 HTTP 400 无效请求异常。','protocol':'读取异常对应的客户端协议。','status':'读取异常应返回的 HTTP 状态。','errorType':'读取异常对应的协议错误类型。'}
function_info[0].update({'requiresProtocolConversion':'判断是否存在跨协议路由转换。','buildOpenAIUpstreamErrorBody':'构造保留上游状态的 OpenAI 兼容错误对象。','buildProtocolErrorBody':'按客户端协议选择 Messages 或 OpenAI 兼容错误结构。','buildOpenAIErrorBody':'构造领域错误对应的 OpenAI 兼容错误封套。','latestRouteFailureType':'从最后一次路由尝试中读取失败类型。'})
function_info[2]={'toResponseBody':'创建流式回调，跟踪首语义 Token，转换或透传上游内容并提取用量；区分断连、I/O 和运行时失败并完成调用。','writeStreamingError':'按 Messages、Responses、Chat 或图像协议写入 SSE 错误帧，写入失败时将其附加为原 I/O 异常的 suppressed 原因。','applyHeaders':'应用上游状态与允许的响应头，按是否转换选择 SSE 内容类型并关闭代理缓冲。'}
function_info[3]={'ResponsesImageBridge':'注入网关调用、请求解析、图像字段和输出封套等桥接依赖。','tryHandle':'判断图像桥接是否适用并执行接口与协议校验，按工具选择进入规划或强制执行，处理流式和非流式响应。','execute':'执行规划返回的图像调用，检查预算和工具调用类型，再组合图像结果与规划文本。','generate':'使用请求覆盖字段和图像输入构建生成或编辑命令，按原生图像操作执行并计量结果。','streamImage':'生成 Responses SSE 生命周期、文本与图像事件，处理错误并完成输出。','plan':'构建并发送内部 Responses 规划调用，计入内部调用用量并解析上游结果。','readUpstreamObject':'校验上游成功状态及非空对象响应，失败时转换为明确网关异常。','startSse':'设置 SSE 内容类型及禁用缓冲等响应头。','failStream':'将桥接执行失败转换为 Responses 流式错误事件。','errorBody':'根据异常生成 Responses 兼容错误 JSON。','child':'构建规划或图像子调用命令，保留操作者、请求上下文和计量归属。'}
function_info[4]={'ResponsesImageRequest':'保存请求副本并分配不与现有函数重名的规划函数标识。','parse':'校验图像工具唯一性、模型、动作、partial_images 和调用预算；按工具选择决定是否隐式补充图像工具。','plannerRequest':'复制请求并关闭流式存储，将图像工具替换为规划函数；把历史图像结果转换为可移植的用户图像内容。','functionTool':'生成包含必填 prompt 与 action 的严格函数 schema，供规划器选择图像生成或编辑。','prompt':'合并系统指令与各消息文本为独立图像提示，缺少提示时拒绝请求。','inputImages':'从原图像生成结果和多模态内容中收集可编辑输入，拒绝缺失图像结果或不支持的 file_id。','readObject':'解析 JSON 对象，非对象或 JSON 格式错误转换为协议无效请求异常。','imageChoiceAllows':'识别 auto、required、指定 image_generation 与 allowed_tools 选择是否允许图像调用。'}
file_info[22]=('验证 Responses 请求规范化按角色调整文本类型、只移除客户端封套 author、保持工具参数中的同名字段，并满足原树不变与幂等要求。',['测试','Responses','请求规范化','兼容性'])
file_info[23]=('验证 Responses 非流式用量提取器读取顶层和嵌套缓存创建 Token 字段。',['测试','Responses','用量统计','缓存'])
file_info[24]=('验证全部登记转换器可被索引，并且字段映射非空、元数据完整、无等价字段明确标记丢弃；检查叶子映射及 Bedrock 往返方向。',['测试','转换注册表','字段映射','可追溯性'])
file_info[25]=('验证同协议流透传器完整保留字节并及时 flush，兼容不完整 SSE 帧与无终态流，同时提取 Chat、Messages 和图像事件用量。',['测试','流式传输','透传','用量统计'])
file_info[27]=('验证响应映射保留上游错误原因、将最终路由限流映射为 HTTP 429 并传递 Retry-After，同时确保解析器和构造器的编程错误不被吞掉。',['测试','响应映射','错误处理','限流'])
file_info[28]=('验证共享协议错误体构造器的 Messages 与 OpenAI 结构、缺省错误类型和序列化回退，同时要求非序列化类编程异常继续传播。',['测试','错误处理','协议适配'])
file_info[29]=('验证转换流中断时写入协议错误，同协议无终态流可成功透传，并在断连时避免额外错误写入与异常重抛。',['测试','流式传输','响应映射','客户端断连'])
# 覆盖末尾九个文件，以当前元数据中的真实路径和测试顺序为准。
test_lines.update({
22:'字符串和 input_text 两种输入均被接受|缺少模型时明确拒绝',
23:'标准缓存写入为零时保留显式零值|标准缓存明细缺失时采用缓存别名',
24:'Responses 相关转换组合均登记|Bedrock request response 与 stream 程序可执行|全部受支持组合均有可执行程序|同协议配置使用 Identity 程序|映射文档包含运行时语义字段|Responses 与 Messages 流事件文档可追溯|映射文档覆盖流完整性增强字段|映射文档包含 Bedrock beta 语义',
25:'同协议 SSE 被转发且用量得到提取|同协议 SSE 多行 data 正确拼接|同协议 Bedrock 流被转发且用量得到提取|用量后的 I/O 失败仍保留已读取用量|Chat 最后一次用量快照不会重复累计',
26:'合法请求可被读取|缺少模型时被拒绝|缺少 max_tokens 时被拒绝|非布尔 stream 被拒绝|system 为工具结果块时被拒绝|非字符串模型被拒绝',
27:'路由候选为空时映射协议错误|同协议上游错误保留状态和 Retry-After|跨协议上游错误转换为 Messages 格式|候选耗尽且最后失败为限流时返回 HTTP 429|最后限流仍保留对应上游元数据',
28:'Messages 客户端得到原生错误格式|Chat 和 Responses 客户端得到 OpenAI 错误格式',
29:'同协议流透传且不调用转换器|观察到语义增量后标记首 Token|同协议流保留上游响应头|跨协议流调用转换器|用量后流失败时完成失败计量|用量后客户端断连仍保留已读取用量|Messages 上游中断写入原生错误事件|Responses 上游中断写入错误事件|Chat 上游中断写入 OpenAI 兼容错误数据',
30:'Responses 图像工具调用转为生成操作且允许调用者密钥|规划模型选择图像工具后才执行生成|非 Responses 规划上游仍可使用托管图像调用|托管图像调用的改写提示成为最终图像提示|SSE 请求生成图像事件|流式规划保留输出与用量|图像子调用可流式执行并发出 partial_images|不支持执行 partial_images 的路线被拒绝|自动选择且规划无图像时不强制生成|多个规划图像调用严格遵守 max_tool_calls|省略 tools 的普通文本请求不被强制桥接|空 tools 的普通文本请求不被强制桥接|仅声明 web_search 时可隐式补充图像工具|有原生 Responses 支持时允许图像路线跳过规划|客户端取消生成时仍记录图像审计|强制图像调用缺少 Token 权限时被拒绝|不支持协议在规划前被拒绝|强制图像调用传递图像工具覆盖字段|编辑动作强制使用已有输入图像|生成动作即使带输入图像也使用生成操作|缺省 auto 动作可由规划器决定|不支持的部分图像数量被拒绝|tool_choice=none 时不执行图像|指定其他工具时不执行图像|回退规划器缺少计数时仍累计可见用量|规划错误时不执行图像|规划器空错误可恢复为强制图像执行|编辑失败在 SSE 中发出错误|多个工具结果遵循工具调用预算|图像输出携带 URL 时保留可用来源|带输入图像的请求使用编辑路线|编辑请求传递 mask 数据|远端 mask 来源被明确拒绝|强制图像工具的非 Responses 目标被明确处理|会话参数不兼容强制执行时拒绝|图像桥接参数冲突得到校验|直接图像模型标识与规划模型分离|批量图像结果保持顺序|内部规划调用用量被独立计量|图像桥接生成结果可再次用于编辑'
})
# 第30文件按真实测试名精确标注，不按猜测补齐尾部行为。
image_tests={
 'test_generatesImage_when_responsesToolRequestsImageGeneration':'Responses 工具请求图像时执行生成操作',
 'test_plansWithBaseModel_before_generatingImage':'先由基础模型规划再生成图像',
 'test_usesToolModelAndPrompt_when_plannerCallsImageTool':'图像调用采用工具模型与规划提示',
 'test_streamsResponsesLifecycle_when_requestIsStreaming':'流式请求输出 Responses 生命周期事件',
 'test_passesThroughPlannerText_when_autoDoesNotCallImageTool':'auto 模式无图像调用时保留规划文本',
 'test_honorsToolChoiceNone_when_imageToolConfigured':'tool_choice 为 none 时不执行图像',
 'test_preservesOtherTools_when_planningImageRequests':'规划图像请求时保留其他工具',
 'test_enforcesMaxToolCalls_when_plannerRequestsMultipleImages':'多个图像请求遵守最大调用次数',
 'test_recordsPlannerAndImageUsage_when_bridgeCompletes':'规划与图像用量均被记录'
}
test_lines[22]='助手历史 input_text 被规范化为输出文本类型|历史已兼容角色时正文保持逐字节一致|规范化不修改原始 JSON 树|长历史中只移除客户端封套 author|最新字段且无 author 时保留原始字节|工具历史中的应用 author 字段被保留|字符串 input 简写保持不变|重复规范化保持幂等'
test_lines[23]='Responses 嵌套缓存创建字段被正确提取|Responses 顶层缓存创建字段被正确提取'
test_lines[24]='十五种已登记转换器均可索引|已登记转换器有非空字段映射|每条映射均有必填描述字段|目标无等价字段时标记为丢弃映射|工具定义多字段均有叶子映射|Messages 与 Bedrock 路线具有往返映射|响应映射采用反向可执行转换方向'
test_lines[25]='完整事件到达后及时 flush|Messages 无终态流仍按原始内容透传|Messages 缺少末尾空行仍提取用量|Messages 无 event 行仍识别终态|Chat DONE 标记被正确接受|图像生成完成事件提取用量|图像编辑完成事件提取用量|图像流被逐字节透传'
test_lines[27]='转换上游失败时保留原始错误消息|错误解析器编程错误继续向外传播|错误体构造器编程错误继续向外传播|最后上游失败为限流时返回 HTTP 429|最后限流失败返回 Messages rate_limit_error|流打开前限流仍透传 Retry-After'
test_lines[28]='JSON 序列化失败时返回协议回退错误|JSON 映射器编程错误继续传播|Messages 错误体采用原生结构|Messages 空类型缺省为 invalid_request_error|OpenAI 错误体采用兼容结构|OpenAI 空类型缺省为 invalid_request_error'
test_lines[29]='转换流在终态前结束时写入 Messages 错误事件|透传流无终态结束仍正常完成|透传流无终态结束记录成功|透传 flush 遇管道断开时标记客户端断连|转换流断连时不写额外错误事件|转换流断连时不再向外抛出'
test_lines[30]='强制图像工具时将 Base64 结果封装为 Responses|强制图像工具仅路由图像且保留原始凭据|工具显式指定模型时覆盖默认图像模型|规划器未选择图像时保留文本响应|规划器选择桥接函数后执行图像调用|同时选择图像和外部函数时保留外部工具调用|规划与图像执行后合并用量|tool_choice 为 none 时从规划移除图像工具|tool_choice 为 none 时不隐式补充图像工具|未声明图像工具时普通 Responses 不受影响|内联参考图像生成 multipart 编辑请求|重放历史图像结果可作为编辑输入|远端非内联编辑图像在上游调用前被拒绝|强制图像前检查父模型权限|图像网关拒绝额度时保留原始错误|图像限流保留状态与 Retry-After|图像成功响应未含图像时返回网关错误|图像流发出 Responses partial_image 事件|流完成时输出项标识与序号保持一致|缺少事件名时仍记录可解析用量|图像流截断时发出失败而非成功|上游 error 事件映射为失败|支持未带版本前缀的 Responses 路径|规划器忽略工具限制时禁止图像调用|无法恢复有状态历史时拒绝请求|edit 动作缺少参考图像时被拒绝|allowed_tools 只要求图像时强制执行|自动选择的 allowed_tools 映射正确|规划器超过调用预算时在产生副作用前拒绝|SSE 已开始后保留图像模型权限错误|流式图像请求传递质量与 partial_images|自动规划使用兼容函数代替托管图像工具|代理工具未声明图像时允许隐式补充|规划器原生返回 image_generation_call 时执行图像|规划器已返回完成图像时直接桥接结果|规划器拒绝桥接工具时回退透传|流式规划拒绝桥接工具时回退透传|流式回退移除桥接函数|规划器选择图像时保留 previous_response_id|规划模型与图像子调用使用不同调用标识'
test_lines.pop(26)
# 使用实际提取名称清单导出待补充项，所有正式节点仍必须有明确语义。
for i,text in test_lines.items():
 fs=[z for z in d['results'][i].get('functions',[]) if z['name'].startswith('test_')];ts=text.split('|')
 if len(fs)!=len(ts):raise ValueError((i,len(fs),len(ts)))
 function_info.setdefault(i,{}).update({z['name']:'验证'+t+'。' for z,t in zip(fs,ts)})
