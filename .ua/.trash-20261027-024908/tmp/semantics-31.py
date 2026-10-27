from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={'dashboardMetricApi.ts':('请求个人、密钥和管理员仪表盘指标，统一时区参数并规范化令牌数、排行与趋势响应。',['仪表盘','接口适配','数值规范化']),
'types.ts':('定义令牌计量、用户与凭据排行、渠道延迟和趋势等仪表盘请求响应契约。',['仪表盘','类型定义','指标契约']),
'useDashboardMetrics.ts':('为个人、凭据、管理员及用量分布指标提供按请求参数分隔缓存的查询钩子。',['仪表盘','查询钩子','缓存']),
'SlowestChannelTable.tsx':('展示最多五个耗时最长的渠道，包含最长总耗时、首字耗时、平均耗时和今日请求数。',['渠道延迟','排行表格','耗时格式化'])}
d={'toFrontDashboard':'兼容新旧令牌字段，将今日和本月令牌规范化，并适配近期调用与密钥数量。','getFrontDashboard':'补全时区后查询个人仪表盘，并转换后端响应为前端指标结构。','getFrontKeyMetrics':'序列化凭据筛选及趋势天数，查询密钥指标并规范化排行和趋势集合。','getAdminDashboard':'查询管理员仪表盘，清理协议请求速率、令牌量、排行、趋势及慢渠道数据。','getUsageDistributions':'按个人或管理员范围选择接口，返回模型和渠道用量分布。','useFrontDashboardMetrics':'以个人范围和请求参数构造查询键，加载个人仪表盘指标。','useFrontKeyMetrics':'以凭据指标范围和请求参数缓存密钥排行及趋势。','useAdminDashboardMetrics':'按管理员范围与请求参数缓存管理仪表盘指标。','useUsageDistributions':'按范围及时区参数缓存模型和渠道的用量分布。','formatDurationMillis':'检查耗时是否为有效非负数，不足一秒显示毫秒，否则保留一位小数显示秒。','SlowestChannelTable':'渲染最多五条慢渠道排名，并处理加载、空状态及各耗时列的格式化。'}
p='frontend/src/entities/dashboard-metric/';e=[]
for hook,api in [('useFrontDashboardMetrics','getFrontDashboard'),('useFrontKeyMetrics','getFrontKeyMetrics'),('useAdminDashboardMetrics','getAdminDashboard'),('useUsageDistributions','getUsageDistributions')]:e.append(('function:'+p+'model/useDashboardMetrics.ts:'+hook,'function:'+p+'api/dashboardMetricApi.ts:'+api,'calls'))
b.code(31,s,d,e)
