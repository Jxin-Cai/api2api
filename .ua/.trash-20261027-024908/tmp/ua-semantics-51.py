from importlib.machinery import SourceFileLoader
import json
m=SourceFileLoader('ua_writer_51','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={
'ApiCredentialTablePanel.css':('定义 API 凭证管理面板的统计网格、指标卡、工具栏和表格外观，统一主题色与紧凑布局。',['组件样式','凭证管理','响应式布局'],'API 凭证列表样式'),
'RequireAuth.tsx':('校验当前用户登录态，未登录时携带原路径跳转登录页，禁用账户显示错误状态，已登录有效账户才渲染子内容。',['登录校验','路由守卫','访问控制'],'登录态路由守卫'),
'RequireRole.tsx':('校验当前用户是否具有指定角色，未登录时跳转登录页，权限不符时展示禁止访问及返回前台操作。',['角色校验','路由守卫','访问控制'],'角色权限路由守卫'),
'RoleLandingRedirect.tsx':('在登录后优先跳转经过站内路径校验的 redirect 参数，否则依据管理员或普通用户角色进入相应仪表盘。',['登录跳转','路由导航','访问控制'],'角色默认入口跳转'),
'PortalMenuSwitcher.tsx':('为管理员提供前台与后台门户切换，并为普通用户显示禁用的用户门户标识。',['门户导航','角色展示','交互组件'],'前后台门户切换'),
'AdminDashboardPanel.css':('定义后台仪表盘分区、协议指标网格及图表块布局，使指标与排行在不同屏宽下保持整齐显示。',['组件样式','仪表盘','响应式布局'],'后台仪表盘布局'),
'AdminDashboardPanel.tsx':('组合全平台令牌概览、协议请求速率、用户排行、模型渠道分布与七日趋势，统一时区及失败重试展示。',['后台仪表盘','指标聚合','可视化'],'管理端全平台用量展示'),
'FrontDashboardPanel.css':('为前台仪表盘趋势标题与凭证筛选器提供纵向排列和全宽布局。',['组件样式','仪表盘','筛选布局'],'前台趋势筛选布局'),
'FrontDashboardPanel.tsx':('展示当前用户令牌概览、模型渠道分布、凭证排行及最近调用，并支持按 API Key 多选筛选令牌趋势与并发曲线。',['用户仪表盘','凭证筛选','可视化','用量记录'],'用户门户用量概览'),
'operationsApi.ts':('定义系统资源与流量快照类型，并提供可暂停的运维查询钩子，分别按十秒与三十秒轮询资源和请求统计。',['运维监控','接口查询','定时轮询'],'运维资源与流量查询'),
'OperationsPanel.css':('定义运维面板健康概览、采样信息、资源卡、说明与延迟表格样式，并处理移动端纵向排布。',['组件样式','运维监控','响应式布局'],'运维监控布局'),
'OperationsPanel.tsx':('展示系统健康、宿主机资源、JVM 与数据库连接池及请求流量，支持自动刷新切换、错误状态和最近六十个资源采样趋势。',['运维监控','资源水位','定时轮询','可视化'],'运行资源与请求表现观测'),
'ProtocolMetadataDetailModal.css':('定义协议元数据详情弹窗的边界、滚动区域、标题、版本标记及事实区块样式，适配主题与视口尺寸。',['组件样式','协议元数据','弹窗布局'],'协议元数据弹窗样式'),
'main.tsx':('初始化 React 应用根节点、全局提供者与路由，首屏应用持久化主题，并处理 Vite 预加载分包错误及重载标记。',['入口点','应用启动','主题初始化','分包恢复'],'前端应用启动'),
'frontend/src/features/view-front-dashboard/model/types.ts':('定义前台仪表盘面板的可选时区属性，供面板与页面组合时共享类型约束。',['类型定义','仪表盘','时区配置'],'前台仪表盘属性约定')}
barrels={
'manage-channel-models':'统一导出渠道模型管理类型、变更钩子、编辑表格、模型获取弹窗与映射管理面板。',
'manage-model-groups':'公开导出模型分组管理列表面板。',
'manage-provider-channels':'统一导出提供方渠道管理类型、变更钩子、模型批量获取弹窗、表单抽屉与列表工具栏。',
'manage-user-accounts':'统一导出用户账户管理类型、变更钩子、操作面板、创建弹窗与编辑抽屉。',
'route-by-role':'公开导出登录态校验、角色权限校验与角色入口跳转组件。',
'switch-portal-navigation':'公开导出管理员前后台门户导航切换组件。',
'view-admin-dashboard':'公开导出管理端全平台仪表盘面板。',
'view-front-dashboard':'公开导出前台仪表盘面板类型及面板组件。',
'view-protocol-conversions':'统一导出协议转换管理类型、变更钩子、详情抽屉、筛选条与列表面板。',
'view-protocol-metadata':'公开导出协议元数据概览面板与详情面板。'}
for folder,s in barrels.items():meta[f'frontend/src/features/{folder}/index.ts']=(s,['公开接口','重导出','切片封装'],'功能切片公开接口')
overrides={name[:-4]:cfg[0] for name,cfg in meta.items() if name.endswith('.tsx') and '/' not in name}
overrides.update({'ResourceCard':'展示资源百分比、进度条与容量详情，依据普通资源或负载阈值标记正常、预警和高危状态。','useSystemMetrics':'查询管理员系统资源快照，自动模式每十秒刷新并跟随窗口聚焦刷新，暂停模式停止轮询。','useOperationsTraffic':'按浏览器时区查询管理员流量快照，自动模式每三十秒刷新并禁用自动重试。'})
base='frontend/src/features/view-operations/'
extras=[]
for hook in ['useSystemMetrics','useOperationsTraffic']:
 extras.append(('function:'+base+'ui/OperationsPanel.tsx:OperationsPanel','function:'+base+'api/operationsApi.ts:'+hook,'calls'))
extras.append(('file:'+base+'ui/OperationsPanel.tsx','file:'+base+'api/operationsApi.ts','depends_on'))
task=json.loads((m.UA/'tmp/batch-task-51.json').read_text())
for p,ns in task.get('neighborMap',{}).items():
 if p.endswith('.css'):
  for n in ns:extras.append(('file:'+p,'file:'+n['path'],'related'))
for stem in ['AdminDashboardPanel','FrontDashboardPanel','OperationsPanel']:
 f=next(f['path'] for f in task['files'] if f['path'].endswith('/'+stem+'.css'));target=f[:-4]+'.tsx';extras.append(('file:'+f,'file:'+target,'related'))
m.run(51,meta,overrides,extras)
