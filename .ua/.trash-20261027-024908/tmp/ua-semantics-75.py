from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_75','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={
'QuotaFormModal.tsx':('提供模型分组与指定 API Key 的配额新增和编辑表单，支持指定模型、日周月及累计周期、六位小数百万令牌额度与启用状态；保存后失效配额及凭证缓存。',['配额表单','输入校验','查询缓存','目标选择'],'配额规则新增与编辑'),
'TokenQuotaPanel.tsx':('按模型分组或 API Key 展示并搜索限流规则，呈现已用、剩余、耗尽状态及计量时区下的重置时间，支持刷新、编辑与确认删除。',['配额管理','列表筛选','状态展示','删除确认'],'管理端配额规则维护')}
overrides={name[:-4]:cfg[0] for name,cfg in meta.items()}
base='frontend/src/features/manage-token-quotas/'
m.run(75,meta,overrides,[('function:'+base+'TokenQuotaPanel.tsx:TokenQuotaPanel','function:'+base+'QuotaFormModal.tsx:QuotaFormModal','contains')])
