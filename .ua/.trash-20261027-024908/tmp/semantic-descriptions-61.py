import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'modelGroupApi.ts':('封装模型组列表读取及后台创建、更新、删除请求，统一将后端标识和数值映射规范化，补齐白名单、实际支持模型、日限额与限流状态的默认值。',['模型组接口','响应归一化','前端实体']),
 'types.ts':('定义模型组响应、日限额映射及下拉选项类型；保存请求仅包含名称与模型白名单，响应仍展示跨组内密钥的加权日用量和限流信息。',['类型定义','模型组','额度展示'])}
f='modelGroupApi.ts';a={
'normalizeGroup':'将模型组标识转为字符串，补齐缺失名称与集合，将日限额和日用量映射转换为数值，并保留时间戳。',
'listModelGroups':'调用用户模型组列表接口，并逐项规范化返回的模型组数据。',
'createModelGroup':'向管理员模型组接口提交名称和白名单，规范化新建模型组响应。',
'updateModelGroup':'编码模型组标识并提交后台更新请求，规范化返回的模型组。',
'deleteModelGroup':'编码模型组标识并调用后台删除接口。'}
p='frontend/src/entities/model-group/api/modelGroupApi.ts'
o={f+':'+k:v for k,v in a.items()};o['_edges']=[('function:'+p+':listModelGroups','function:'+p+':normalizeGroup','calls',0.8)]
m.build(61,d,o)
