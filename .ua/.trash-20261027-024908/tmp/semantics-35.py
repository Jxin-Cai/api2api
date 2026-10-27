from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={'types.ts':('复用用量实体与分页类型，并定义筛选钩子的状态更新和分页接口。',['用量筛选','类型定义','状态接口']),
'useUsageFilters.ts':('从地址查询参数解析凭据、模型、协议、时间和分页条件，校验值后通过替换历史记录同步更新。',['用量筛选','地址同步','参数校验']),
'UsageRecordFilterBar.tsx':('提供凭据、模型、协议及时间范围筛选，管理员额外可按用户和供应商渠道过滤。',['用量筛选','筛选组件','权限范围'])}
d={'useUsageFilters':'解析并验证地址中的筛选与分页条件，提供单项、批量、重置和分页更新方法。','UsageRecordFilterBar':'加载凭据、模型及管理员渠道选项，渲染对应范围的筛选控件并提交变更回调。'}
b.code(35,s,d)
