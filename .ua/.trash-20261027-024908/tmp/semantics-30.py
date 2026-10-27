from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={'types.ts':('定义协议转换列表筛选条件和按源目标方向查询的参数结构。',['协议转换','筛选参数','类型定义']),
'useProtocolConversionMutations.ts':('提供协议转换启停与按方向查找操作，状态变更成功后使转换查询缓存失效。',['协议转换','变更钩子','缓存刷新']),
'ProtocolConversionDetailDrawer.tsx':('展示转换能力、实现状态和请求响应字段映射，支持启停定义并内嵌源目标协议字段字典。',['协议转换','详情抽屉','映射说明']),
'ProtocolConversionFilterBar.tsx':('提供源协议、目标协议、启用和实现状态筛选，以及方向查找与重置入口。',['协议转换','筛选组件','方向查询']),
'ProtocolConversionTablePanel.tsx':('加载协议转换列表并应用筛选，组合方向查询、列表选择及详情抽屉。',['协议转换','列表组件','界面编排'])}
d={'useProtocolConversionMutations':'组合启用、禁用和按方向查询的变更操作，并公开统一查询失效方法。','ProtocolConversionDetailDrawer':'加载或接收转换详情，渲染能力、字段映射和协议字典，并提交启停操作。','MappingOverview':'根据请求和响应映射视图展示完整、部分及未映射字段的数量。','InlineProtocolMetadata':'查询源与目标协议详情，按分区折叠显示各自的原始字段定义。','formatTimestamp':'兼容秒级和毫秒级时间戳，按中文区域格式输出日期与分钟。','ProtocolConversionFilterBar':'渲染源目标协议与状态筛选项，并将变更、方向查找和重置事件传递给调用方。','ProtocolConversionTablePanel':'对转换列表执行本地条件筛选，协调按方向查找和转换详情选中状态。'}
p='frontend/src/features/view-protocol-conversions/';e=[]
for fn in ['ProtocolConversionDetailDrawer','ProtocolConversionTablePanel']:
 e.append(('function:'+p+'ui/'+fn+'.tsx:'+fn,'function:'+p+'model/useProtocolConversionMutations.ts:useProtocolConversionMutations','calls'))
for child in ['ProtocolConversionDetailDrawer','ProtocolConversionFilterBar']:
 e.append(('function:'+p+'ui/ProtocolConversionTablePanel.tsx:ProtocolConversionTablePanel','function:'+p+'ui/'+child+'.tsx:'+child,'contains'))
for child in ['MappingOverview','InlineProtocolMetadata']:
 e.append(('function:'+p+'ui/ProtocolConversionDetailDrawer.tsx:ProtocolConversionDetailDrawer','function:'+p+'ui/ProtocolConversionDetailDrawer.tsx:'+child,'contains'))
b.code(30,s,d,e)
