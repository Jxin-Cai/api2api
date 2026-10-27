from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_50','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={
'ApiKeySecretBlock.tsx':('展示创建后的一次性 API Key 明文，支持复制、手动显隐及复制后遮罩，并提示手动复制或缺失明文状态。',['密钥管理','交互组件','剪贴板'],'API Key 安全展示与复制'),
'DistributionPieChart.tsx':('展示最多十二项正值令牌分布，提供随主题切换的饼图、自定义占比提示及图例，并处理加载和空数据状态。',['可视化','分布统计','主题适配'],'令牌分布饼图展示'),
'MetricCard.tsx':('展示带单位的指标卡片及变化趋势，解析数值字符串并以提示层补充原始值，支持加载骨架。',['指标卡片','数值格式化','交互组件'],'仪表盘指标展示'),
'TopRankList.tsx':('展示前十项排名、原始数值提示与可选点击交互，统一加载及无数据状态。',['排行榜','交互组件','仪表盘'],'仪表盘排行展示'),
'TrendChart.tsx':('绘制多系列平滑趋势折线，按分类分配颜色并适配主题、单位、小数精度和图例，支持加载与空状态。',['可视化','趋势统计','主题适配'],'多系列趋势展示'),
'FieldMappingHierarchy.css':('定义协议字段映射层级视图的摘要、阅读指引、树形行与状态标记样式，使用主题变量组织布局及交互层次。',['组件样式','字段映射','主题适配'],'字段映射层级样式'),
'ProtocolMetadataLink.tsx':('根据协议元数据生成展示文案，点击后跳转管理端协议转换页面并携带协议查询参数。',['协议元数据','导航链接','交互组件'],'协议字段定义导航'),
'UsageTokenSummary.css':('定义用量摘要的主次卡片、令牌拆分、请求计数与骨架样式，并在窄屏下切换为单列布局。',['组件样式','用量统计','响应式布局'],'用量摘要样式'),
'useUpdateCurrentUserProfile.ts':('封装当前用户资料更新 mutation，成功后写入并失效当前用户查询缓存，保持跨组件资料同步。',['查询缓存','资料更新','状态管理'],'个人资料更新与缓存同步'),
'PersonalSettingsPanel.tsx':('展示当前账户资料并提供显示名称编辑表单，处理资料加载、失败重试与更新成功或错误反馈。',['个人设置','表单组件','资料更新'],'个人资料编辑'),
'UsagePageSizeSelector.tsx':('使用统一可选条数组件展示用量记录分页大小，并通过回调通知父组件更新，支持禁用。',['分页控制','交互组件','用量记录'],'用量分页条数选择'),
'LoginForm.css':('定义登录表单标题、下划线输入框、焦点状态与提交按钮样式，并兼容浏览器自动填充及移动端显示。',['组件样式','登录表单','主题适配'],'登录表单样式')}
barrels={
'frontend/src/entities/channel-model-support/index.ts':'统一导出渠道模型支持的接口、类型及模型行、优先级与来源标记组件。',
'frontend/src/entities/dashboard-metric/index.ts':'统一导出仪表盘指标类型、查询、接口，以及指标卡、趋势图、排名和分布组件。',
'frontend/src/entities/model-group/index.ts':'统一导出模型分组类型、查询钩子及接口，作为实体切片公开入口。',
'frontend/src/entities/protocol-conversion/index.ts':'统一导出协议转换接口、查询、映射视图模型及能力标签和字段映射组件。',
'frontend/src/entities/protocol-metadata/index.ts':'统一导出协议元数据接口、查询、类型与字段定义展示组件。',
'frontend/src/entities/provider-channel/index.ts':'统一导出提供方渠道接口、查询、类型及渠道行和协议状态标记组件。',
'frontend/src/entities/provider-model/index.ts':'统一导出提供方模型查询与接口，作为模型实体公开入口。',
'frontend/src/entities/usage-record/index.ts':'统一导出用量记录类型、查询、接口及表格、令牌摘要与状态组件。',
'frontend/src/entities/user-account/index.ts':'统一导出用户账户接口、当前用户和账户查询、退出钩子，以及角色与状态展示组件。',
'frontend/src/features/edit-personal-settings/index.ts':'公开导出个人资料更新钩子与个人设置面板。',
'frontend/src/features/filter-usage-records/index.ts':'公开导出用量筛选类型、筛选状态钩子、筛选条与分页大小选择器。',
'frontend/src/features/login-by-username/index.ts':'公开导出用户名登录接口、类型、登录钩子与表单。',
'frontend/src/features/manage-api-credentials/index.ts':'公开导出 API 凭证管理类型、变更钩子，以及工具栏、创建弹窗、编辑抽屉和列表面板。'}
for p,s in barrels.items():meta[p]=(s,['公开接口','重导出','切片封装'],'切片公开接口')
overrides={
'ApiKeySecretBlock':meta['ApiKeySecretBlock.tsx'][0], 'DistributionPieChart':meta['DistributionPieChart.tsx'][0], 'MetricCard':meta['MetricCard.tsx'][0], 'TopRankList':meta['TopRankList.tsx'][0], 'TrendChart':meta['TrendChart.tsx'][0], 'ProtocolMetadataLink':meta['ProtocolMetadataLink.tsx'][0], 'useUpdateCurrentUserProfile':meta['useUpdateCurrentUserProfile.ts'][0], 'PersonalSettingsPanel':meta['PersonalSettingsPanel.tsx'][0], 'UsagePageSizeSelector':meta['UsagePageSizeSelector.tsx'][0],
'formatDistributionTokens':'将令牌数格式化为中文千位分隔的精确值，并在百万及以上时补充紧凑单位展示。',
'resolveTooltipDatum':'从图表提示事件中定位对应分布项，缺少原始数据时根据名称和值补全提示数据。',
'renderSliceTooltip':'对名称和格式化数值进行 HTML 转义，生成包含令牌数与占比的分布提示内容。',
'parseDisplayValue':'识别数字及带后缀的数值字符串，保留小数位和单位，并为缺失值生成占位文本。'}
extras=[('file:frontend/src/features/edit-personal-settings/ui/PersonalSettingsPanel.tsx','file:frontend/src/features/edit-personal-settings/model/useUpdateCurrentUserProfile.ts','depends_on'),('function:frontend/src/features/edit-personal-settings/ui/PersonalSettingsPanel.tsx:PersonalSettingsPanel','function:frontend/src/features/edit-personal-settings/model/useUpdateCurrentUserProfile.ts:useUpdateCurrentUserProfile','calls')]
for css,tsx in [('frontend/src/entities/protocol-conversion/ui/FieldMappingHierarchy.css','frontend/src/entities/protocol-conversion/ui/FieldMappingHierarchy.tsx'),('frontend/src/entities/usage-record/ui/UsageTokenSummary.css','frontend/src/entities/usage-record/ui/UsageTokenSummary.tsx'),('frontend/src/features/login-by-username/ui/LoginForm.css','frontend/src/features/login-by-username/ui/LoginForm.tsx')]:extras.append(('file:'+css,'file:'+tsx,'related'))
m.run(50,meta,overrides,extras)
