from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={'useModelGroupMutations.ts':('组合模型分组创建、更新和删除，修改权限后同步刷新分组及 API 凭据缓存。',['模型分组','变更钩子','权限刷新']),
'ModelGroupFormModal.tsx':('提供模型分组创建与编辑表单，维护名称和模型白名单，并说明配额在限流页统一配置。',['模型分组','表单弹窗','模型白名单']),
'ModelGroupTablePanel.tsx':('展示分组模型配置与可用情况，统计密钥绑定数，并在解绑完成后允许删除分组。',['模型分组','列表组件','绑定校验'])}
d={'useModelGroupMutations':'封装创建、更新和删除分组操作，按变更影响刷新分组及凭据查询。','ModelGroupFormModal':'在弹窗打开时载入分组或空表单，提交规范化名称与模型白名单并回传保存结果。','ModelGroupTablePanel':'加载分组及密钥额度目标，计算绑定数，协调创建编辑弹窗并阻止删除仍有关联密钥的分组。'}
p='frontend/src/features/manage-model-groups/';e=[]
for name in ['ModelGroupFormModal','ModelGroupTablePanel']:e.append(('function:'+p+'ui/'+name+'.tsx:'+name,'function:'+p+'model/useModelGroupMutations.ts:useModelGroupMutations','calls'))
e.append(('function:'+p+'ui/ModelGroupTablePanel.tsx:ModelGroupTablePanel','function:'+p+'ui/ModelGroupFormModal.tsx:ModelGroupFormModal','contains'))
b.code(36,s,d,e)
