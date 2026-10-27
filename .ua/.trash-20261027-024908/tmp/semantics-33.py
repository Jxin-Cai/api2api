from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={'AppProviders.tsx':('组合查询客户端、中文组件配置、主题和遵循系统偏好的动效配置，为应用提供共享上下文。',['应用入口','上下文提供器','主题']),
'queryClient.ts':('创建全局查询客户端，配置缓存时效、瞬态错误重试及指数退避，禁用变更自动重试。',['查询客户端','缓存策略','错误重试']),
'theme.ts':('根据明暗模式生成 Ant Design 主题，统一中性色板、字体、圆角、阴影及布局组件样式。',['主题配置','明暗模式','设计令牌'])}
d={'AppProviders':'读取主题模式，将应用子树包装在查询、组件、动效与通知上下文中。','retryTransientQuery':'最多重试两次查询，排除除限流以外的客户端 HTTP 错误。','buildAppTheme':'按明暗模式选择算法和调色值，生成全局设计令牌与组件主题覆盖。'}
p='frontend/src/app/providers/';e=[('function:'+p+'AppProviders.tsx:AppProviders','function:'+p+'theme.ts:buildAppTheme','calls')]
b.code(33,s,d,e)
