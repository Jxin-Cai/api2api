from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={'ProtocolMetadataDetailModal.tsx':('在可关闭并销毁内容的弹窗中按选中协议展示规范详情。',['协议元数据','详情弹窗','界面容器']),
'ProtocolMetadataDetailPanel.tsx':('加载协议详情，展示版本、端点、字段统计、官方参考及分区字段表，并区分规范字段与运行时转换支持。',['协议元数据','字段字典','规范参考']),
'ProtocolMetadataOverviewPanel.tsx':('以响应式卡片展示协议列表，用地址查询参数保持当前详情弹窗选择。',['协议元数据','概览组件','地址同步'])}
d={'ProtocolMetadataDetailModal':'按打开状态显示协议详情弹窗，仅在协议类型非空时挂载详情面板。','ProtocolMetadataDetailPanel':'查询协议详情并处理加载与错误，渲染官方参考、字段统计及折叠的字段分区。','ProtocolMetadataOverviewPanel':'查询协议列表，渲染可选择卡片并通过 protocol 查询参数控制详情弹窗。'}
p='frontend/src/features/view-protocol-metadata/ui/';e=[]
for parent,child in [('ProtocolMetadataOverviewPanel','ProtocolMetadataDetailModal'),('ProtocolMetadataDetailModal','ProtocolMetadataDetailPanel')]:e.append(('function:'+p+parent+'.tsx:'+parent,'function:'+p+child+'.tsx:'+child,'contains'))
b.code(37,s,d,e)
