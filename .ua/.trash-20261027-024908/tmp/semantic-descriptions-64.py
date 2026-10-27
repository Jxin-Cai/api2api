import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'TokenQuotaWindow.java':('根据注入时钟与配置时区计算日、周、月和累计配额周期，周窗口以周一为起点，各周期均返回起点包含、终点不包含的业务时间范围。',['配额周期','时区边界','应用组件']),
'QuotaPeriod.java':('定义日、周、月和累计四类令牌配额周期，作为规则和窗口计算的公共领域枚举。',['配额周期','枚举类型','领域模型']),
'TokenQuotaRepository.java':('定义令牌配额规则管理、适用规则查询、周期内加权令牌汇总及密钥和模型组准入锁定的领域仓储契约。',['仓储接口','统一配额','并发准入'])}
f='TokenQuotaWindow.java';o={f+':current':'按业务时区确定当前日期，分别计算当天、周一开始的本周、当月或长期累计窗口，返回半开时间范围。',f+':zoneId':'返回配额窗口使用的业务时区标识，供界面展示重置规则。'}
m.build(64,d,o)
