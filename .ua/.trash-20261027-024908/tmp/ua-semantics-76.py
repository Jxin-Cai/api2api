from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_76','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={
'AdminModelGroupsPage.tsx':('加载可用模型选项并组合管理员模型分组面板，说明前台凭证通过选择分组继承模型权限。',['页面入口','模型分组','组件组合'],'管理员模型分组页面'),
'AdminTokenQuotasPage.tsx':('组合配额管理面板与页面说明，为管理员提供按模型分组或指定 API Key 配置百万令牌额度的入口。',['页面入口','配额管理','组件组合'],'管理员配额页面'),
'ApiKeysPage.tsx':('查询管理员配置的模型分组选项并传入凭证管理面板，供前台用户创建应用访问凭证。',['页面入口','凭证管理','模型分组'],'前台 API Key 页面')}
overrides={name[:-4]:cfg[0] for name,cfg in meta.items()}
m.run(76,meta,overrides)
