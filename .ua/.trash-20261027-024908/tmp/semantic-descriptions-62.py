import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'ApiCredentialApplicationService.java':('编排用户密钥创建、查询、明文揭示、重命名、切换模型组和启停删除，校验所有权并汇总实际与原始用量。新建密钥使用无限旧额度，受限模型改由统一令牌配额服务判定，网关认证仍校验可用性和模型白名单。',['密钥服务','统一配额','权限校验']),
'ModelGroupApplicationService.java':('向可访问门户的用户提供全局模型组及当前渠道有效模型视图，仅允许管理员创建、修改和删除。管理操作校验全局名称唯一与密钥绑定，保存时清空旧模型日限额。',['模型组服务','全局目录','管理员管理']),
'TokenQuotaService.java':('在网关准入短事务中锁定密钥及模型组，按适用规则和当前周期检查额度。另可计算白名单内受限模型，支持所有模型共用规则或指定模型规则。',['统一配额','事务锁定','周期用量'])}
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('ApiCredentialApplicationService.java',{'createCredential':'校验用户权限并加载全局模型组，将其白名单复制到新密钥，使用无限旧额度并持久化。','listMyCredentials':'校验门户访问权限后读取当前用户名下密钥。','listMyCredentialUsageViews':'为当前用户每个密钥读取累计与当日用量，并从统一配额服务计算受限模型。','usageView':'组合密钥、累计原始及加权用量、当日用量和受限模型，构建密钥展示视图。','revealSecret':'验证密钥所有权和加密材料可用性，解密后核对存储哈希，拒绝材料不完整或被破坏的记录。','renameCredential':'验证所有权后更新密钥名称和审计时间并保存。','changeModelGroup':'验证密钥所有权，加载新全局模型组并同步组身份及白名单到密钥。','disableCredential':'验证所有权后禁用密钥并持久化。','enableCredential':'验证所有权后启用密钥并持久化。','deleteCredential':'验证所有权后按当前时刻软删除密钥。','authenticateForGateway':'按密钥哈希查询，校验启用状态、模型权限和已消费额度，记录最后使用时刻并保存。','authenticateForModelListing':'按密钥哈希查找并校验可用性，供模型目录查询使用。','todayTokenBreakdown':'按用户、密钥和当日范围查询令牌明细，拒绝负计费用量。'})
add('ModelGroupApplicationService.java',{'listMyGroups':'校验用户门户访问权限后读取全局模型组目录，不按所有者分割配置。','listMyGroupViews':'组合全局模型组、当前用户日用量和渠道可用模型，形成模型组展示视图。','listAdminGroupViews':'校验后台管理员权限后复用全局模型组视图查询。','viewGroup':'按模型组所有者补充日用量，并计算与当前渠道可用模型的交集。','createGroup':'校验管理员身份与全局名称唯一性，使用空旧日限额创建并保存模型组。','updateGroup':'校验管理员身份和名称唯一性，更新名称及白名单，清空旧日限额并保存。','deleteGroup':'校验管理员权限，拒绝删除仍绑定密钥的组，否则软删除。'})
add('TokenQuotaService.java',{'lockCredential':'在必需的现有事务内锁定密钥行并重新读取最新密钥，不存在时报告认证失效。','assertAvailable':'锁定模型组，逐条检查适用于请求模型的配额规则和当前周期用量，超额时抛出带重置时刻的配额异常。','limitedModels':'读取已启用规则的周期用量，将已耗尽的通用规则展开为全部白名单模型，指定模型规则仅加入白名单交集。'})
m.build(62,d,o)
