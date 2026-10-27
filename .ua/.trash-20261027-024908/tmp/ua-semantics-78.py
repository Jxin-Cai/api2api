from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_78','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={
'menu.ts':('定义前台与后台导航菜单，新增管理端模型分组和限流入口，并按门户选择菜单及按路径前缀确定当前激活项。',['导航配置','配额入口','门户菜单','路由匹配'],'门户菜单选择与高亮'),
'AppShell.tsx':('组合前后台响应式侧栏、移动抽屉、门户切换、主题和账户菜单及路由出口，并为模型分组与限流入口提供图标及认证失效跳转。',['应用布局','门户导航','配额入口','认证状态'],'前后台门户外壳')}
overrides={
'getMenuItems':'依据前台或后台门户类型返回对应菜单列表，管理员列表包含模型分组和限流入口。',
'getActiveMenuKey':'从前后台全部菜单中选择路径前缀匹配项，未匹配时返回前台仪表盘键。',
'AppShell':meta['AppShell.tsx'][0]}
base='frontend/src/widgets/app-shell/'
extras=[('function:'+base+'ui/AppShell.tsx:AppShell','function:'+base+'model/menu.ts:'+name,'calls') for name in ['getMenuItems','getActiveMenuKey']]
m.run(78,meta,overrides,extras)
