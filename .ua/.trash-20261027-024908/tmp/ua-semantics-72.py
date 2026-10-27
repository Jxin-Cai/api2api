from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_72','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={
'TokenQuotaExceededException.java':('表示调用方预算耗尽而非提供方渠道失败，错误信息包含配额作用域、周期及可选模型，并携带窗口重置时间。',['配额限制','业务异常','重置时间'],'调用方配额耗尽反馈'),
'QuotaScope.java':('定义模型分组与 API Key 两种配额作用域，用于区分共享预算和单凭证预算。',['配额模型','枚举','作用域'],'配额作用域约束'),
'QuotaTarget.java':('定义管理端可安全展示的配额目标元数据，包含作用域、标识、名称、归属者及模型分组，避免携带密钥材料。',['配额目标','不可变记录','安全元数据'],'配额管理目标展示'),
'TokenQuotaRule.java':('定义模型分组或凭证的令牌预算规则，校验目标与限额为正并规范化模型名，空模型表示汇总全部模型。',['配额规则','不可变记录','领域校验','模型匹配'],'令牌配额规则判定')}
overrides={
'resetAt':'返回此次配额耗尽对应的重置时刻，供上层计算可重试时间。',
'appliesTo':'仅对已启用且模型为空或与请求模型完全一致的配额规则返回适用。'}
m.run(72,meta,overrides)
