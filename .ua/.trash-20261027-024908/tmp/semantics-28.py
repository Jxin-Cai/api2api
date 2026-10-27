from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={
'protocolMetadataApi.ts':('封装协议元数据列表与按协议类型查询详情的 HTTP 请求。',['协议元数据','接口访问','请求封装']),
'types.ts':('定义协议列表、字段定义、分区及详情响应的 TypeScript 数据契约。',['协议元数据','类型定义','响应契约']),
'useProtocolMetadata.ts':('为协议元数据列表和详情提供查询键与 React Query 查询钩子，支持按协议类型启用详情加载。',['协议元数据','查询钩子','缓存']),
'ProtocolFieldTable.tsx':('使用表格展示字段路径、类型、必填性、输入输出方向及用途说明。',['字段元数据','表格组件','协议说明']),
'ProtocolMetadataCard.tsx':('以可选中的协议卡片展示版本、默认端点、描述及输入输出字段数量。',['协议元数据','展示组件','协议选择'])}
d={'listProtocolMetadata':'请求协议元数据列表接口并返回类型化响应。','getProtocolMetadataDetail':'编码协议类型后查询其字段分区与详情。','useProtocolMetadataList':'用稳定查询键缓存协议元数据列表。','useProtocolMetadataDetail':'按协议类型构造详情查询，并结合启用标记及有效类型控制加载。','ProtocolFieldTable':'渲染不分页的协议字段表格，用标签标记字段类型、必填性和方向。','ProtocolMetadataCard':'根据协议元信息设置卡片样式，展示协议说明、端点及字段数量并支持点击选择。'}
p='frontend/src/entities/protocol-metadata/'
e=[('function:'+p+'model/useProtocolMetadata.ts:useProtocolMetadataDetail','function:'+p+'api/protocolMetadataApi.ts:getProtocolMetadataDetail','calls')]
b.code(28,s,d,e)
