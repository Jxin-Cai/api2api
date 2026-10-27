from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_54','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={
'frontend/src/widgets/auth-layout/index.ts':('公开导出认证页面布局，封装登录入口的品牌与能力介绍区域。',['公开接口','重导出','认证布局'],'认证布局公开入口'),
'AuthLayout.css':('定义登录落地页品牌导航、协议标签、路由示意、表单及能力卡样式，提供移动端布局、安全区和减少动效偏好支持。',['组件样式','认证布局','响应式布局','无障碍'],'认证落地页布局样式'),
'AuthLayout.tsx':('组合网关品牌、支持协议、路由示意、平台能力与认证表单插槽，构建登录落地页并应用光晕、渐变与聚光装饰。',['认证布局','组件组合','平台介绍'],'认证落地页组合'),
'frontend/src/widgets/channel-models-panel/index.ts':('公开导出渠道模型配置面板，封装模型编辑与发现功能的组合入口。',['公开接口','重导出','模型配置'],'渠道模型面板公开入口'),
'ChannelModelsPanel.tsx':('统计渠道中可路由及限流模型数量，组合模型编辑表格和验证获取弹窗，并将变更后的渠道信息交回父组件。',['渠道模型','组件组合','模型发现'],'渠道模型配置面板'),
'frontend/src/widgets/protocol-mapping-panel/index.ts':('公开导出协议字段映射面板，封装请求与响应规则展示入口。',['公开接口','重导出','字段映射'],'协议映射面板公开入口'),
'ProtocolMappingPanel.tsx':('通过标签页展示请求与响应映射并标注字段数，支持在图解层级与紧凑表格之间切换。',['字段映射','组件组合','视图切换'],'协议映射展示面板'),
'frontend/src/widgets/usage-records-panel/index.ts':('公开导出用量记录组合面板，供前后台页面按作用域复用。',['公开接口','重导出','用量记录'],'用量记录面板公开入口'),
'UsageRecordsPanel.tsx':('按门户作用域协调用量筛选、查询、分页及令牌汇总，组合筛选条、记录表格和摘要，并提供失败重试与重置筛选。',['用量记录','组件组合','分页筛选','错误反馈'],'前后台用量记录面板'),
'vite.config.ts':('配置 Vite 的 React 插件与 FSD 层级路径别名，并在设置 VITE_API_BASE_URL 时启用开发服务器的 /api 反向代理。',['构建配置','路径别名','开发代理'],'前端开发及构建配置')}
overrides={name[:-4]:cfg[0] for name,cfg in meta.items() if name.endswith('.tsx') and '/' not in name}
m.run(54,meta,overrides,[('file:frontend/src/widgets/auth-layout/ui/AuthLayout.css','file:frontend/src/widgets/auth-layout/ui/AuthLayout.tsx','related')])
