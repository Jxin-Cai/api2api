from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_74','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={'frontend/src/entities/token-quota/index.ts':('提供令牌配额实体类型、管理列表查询及新增、修改、删除接口，三十秒轮询用量并为未加载数据提供稳定空数组与默认上海时区。',['配额实体','接口封装','查询缓存','定时轮询'],'管理端令牌配额查询与变更')}
overrides={
'useTokenQuotas':'每三十秒刷新管理端令牌配额规则与目标列表，返回查询状态、稳定默认数组及计量时区。',
'saveQuota':'根据是否传入规则标识选择新增或更新配额，编码路径标识并返回保存后的规则。',
'deleteQuota':'对经过路径编码的规则标识发起删除请求，成功后不返回业务数据。'}
m.run(74,meta,overrides)
