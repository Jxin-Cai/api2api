from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_53','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={
'frontend/src/shared/lib/index.ts':('统一导出格式化、剪贴板、协议元数据、图表数据、分页、时区及分包加载恢复工具。',['公开接口','重导出','工具函数'],'共享工具公开入口'),
'protocols.ts':('定义客户端及上游协议选项、协议标签颜色与官方规范信息，并提供转换能力说明及方向格式化。',['协议元数据','显示标签','共享配置'],'协议及转换能力展示'),
'frontend/src/shared/lib/table.ts':('统一用量表格的五十、一百及二百条分页选项，规范化条数输入并生成 Ant Design 分页配置。',['分页配置','输入校验','表格工具'],'用量表格分页配置'),
'timeZone.ts':('读取浏览器 IANA 时区，并在缺少显式时区时使用浏览器或 UTC 作为有效时区。',['工具函数','时区处理','默认值'],'浏览器时区解析'),
'frontend/src/shared/types/admin.ts':('定义管理端标识、角色、账户状态、模型来源及协议实现状态等公共类型，以及泛型选择项结构。',['类型定义','管理模型','共享契约'],'管理端共享类型'),
'frontend/src/shared/types/chart.ts':('定义趋势点、排名条目及指标涨跌信息，供指标卡、趋势图与排名组件共享数据结构。',['类型定义','图表数据','共享契约'],'共享可视化数据类型'),
'frontend/src/shared/types/portal.ts':('定义前台和管理后台两种门户类型，为路由导航及布局组件提供统一约束。',['类型定义','门户导航','共享契约'],'门户类型约束'),
'frontend/src/shared/types/table.ts':('定义分页条数联合类型，以及泛型分页请求、响应和表格分页状态。',['类型定义','分页契约','共享契约'],'表格分页数据类型'),
'DashboardSummaryGrid.tsx':('为仪表盘汇总卡提供统一的响应式网格包裹，并允许附加样式与类名。',['布局组件','仪表盘','响应式布局'],'仪表盘摘要网格布局'),
'PageHeader.tsx':('统一页面标题、说明文字与右侧操作区域的布局，支持仅有标题的紧凑展示。',['布局组件','页面标题','共享组件'],'页面标题区域布局'),
'PageState.tsx':('统一展示页面加载骨架、查询错误提示与重试操作，为各业务面板提供一致的数据状态体验。',['状态展示','错误反馈','加载骨架'],'页面数据加载与错误展示'),
'RouteErrorState.tsx':('处理路由加载异常，识别分包更新失败并触发受控刷新；提供手动刷新和首页操作，开发环境额外显示错误详情。',['错误边界','路由状态','分包恢复'],'路由异常恢复'),
'frontend/src/shared/ui/index.ts':('集中导出动画组件与仪表盘布局、页面状态、页面标题和路由错误展示组件。',['公开接口','重导出','共享组件'],'共享界面组件公开入口'),
'AnimatedContent.tsx':('使用 Motion 按轴向、位移、缩放及延时配置内容进入动画，支持进入视口时触发与只播放一次。',['动画组件','视口触发','共享组件'],'内容进入动画'),
'AuroraBackground.tsx':('提供具有光晕叠层与可选动效的背景容器，通过主题样式组合子内容。',['背景组件','视觉效果','共享组件'],'光晕背景容器'),
'CountUp.tsx':('将数字从起始值逐帧缓动到目标值，支持延时、时长、小数位、分隔符与视口触发，并在卸载时清理动画。',['动画组件','数值展示','视口触发'],'数字递增动画'),
'FadeContent.tsx':('使用 Motion 为内容提供透明度与可选模糊进入效果，支持延时和进入视口触发。',['动画组件','渐显效果','共享组件'],'内容渐显动画'),
'GradientText.tsx':('为文本提供可配置渐变色和背景动画速度，支持关闭动画并附加自定义类名。',['文本组件','渐变效果','共享组件'],'渐变文本展示'),
'ShinyText.tsx':('为文本提供可配置扫光速度与样式的高光动画，支持关闭动效。',['文本组件','扫光效果','共享组件'],'扫光文本展示'),
'SpotlightCard.tsx':('追踪鼠标相对卡片的位置并更新径向高光样式，支持自定义聚光颜色、圆角与内容类名。',['卡片组件','指针交互','视觉效果'],'鼠标聚光卡片'),
'frontend/src/shared/ui/reactbits/index.ts':('集中导出数字递增、渐变与扫光文本、聚光卡、进入与渐显动画，以及光晕背景组件。',['公开接口','重导出','动画组件'],'共享动画组件公开入口'),
'frontend/src/widgets/app-shell/index.ts':('公开导出应用外壳组件及菜单项类型，封装门户布局实现。',['公开接口','重导出','应用布局'],'应用外壳公开入口'),
'menu.ts':('定义前后台门户菜单及目标路由，根据门户返回菜单列表，并按当前路径定位激活菜单项。',['导航配置','门户菜单','路由匹配'],'门户菜单选择与高亮'),
'AppShell.css':('定义应用外壳侧栏、移动抽屉、品牌、菜单、顶栏与内容区样式，并为深浅主题及窄屏提供适配。',['组件样式','应用布局','主题适配','响应式布局'],'应用外壳布局样式'),
'AppShell.tsx':('组合响应式侧栏与移动抽屉、门户菜单、主题切换、账户菜单和路由出口，监听认证失效并支持退出登录跳转。',['应用布局','门户导航','认证状态','主题切换'],'前后台应用外壳')}
overrides={name[:-4]:cfg[0] for name,cfg in meta.items() if name.endswith('.tsx') and '/' not in name}
overrides.update({
'getProtocolMeta':'查询协议的名称、颜色与规范信息，未知协议使用原标识及默认颜色。',
'formatProtocolDirection':'分别解析源协议与目标协议名称，生成带箭头的转换方向展示文案。',
'isUsagePageSize':'判定数值是否为允许的五十、一百或二百条分页大小，并收窄类型。',
'normalizeUsagePageSize':'将字符串或数值条数规范化为支持的分页大小，无效输入回退为五十条。',
'buildTablePagination':'将页面分页状态转换为 Ant Design 配置，统一条数选项、总数文案和变更回调类型。',
'browserTimeZone':'读取浏览器解析出的 IANA 时区，无法获得时区时回退为 UTC。',
'resolveTimeZone':'优先采用显式时区参数，否则返回浏览器时区。',
'getMenuItems':'根据前台或后台门户选择对应的已定义菜单列表。',
'getActiveMenuKey':'按当前路径匹配前后台菜单项，返回供导航选中状态使用的键。'})
base='frontend/src/widgets/app-shell/'
extras=[('file:'+base+'ui/AppShell.css','file:'+base+'ui/AppShell.tsx','related')]
for name in ['getMenuItems','getActiveMenuKey']:extras.append(('function:'+base+'ui/AppShell.tsx:AppShell','function:'+base+'model/menu.ts:'+name,'calls'))
extras.append(('function:'+base+'ui/AppShell.tsx:AppShell','function:frontend/src/shared/ui/reactbits/GradientText.tsx:GradientText','contains'))
m.run(53,meta,overrides,extras)
