from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={
'types.ts':('定义凭据编辑抽屉的名称与模型分组两个编辑分区。',['API凭据','类型定义','编辑状态']),
'useApiCredentialMutations.ts':('集中封装凭据创建、改名、换组、明文读取和启停删除，状态变更成功后同时刷新凭据与额度缓存。',['API凭据','变更钩子','缓存刷新']),
'ApiCredentialCreateModal.tsx':('提供名称与模型分组创建表单，成功后展示明文密钥并要求保存确认，避免关闭时丢失展示内容。',['API凭据','创建弹窗','密钥保存']),
'ApiCredentialEditDrawer.tsx':('以名称和模型分组分区编辑已有 API 凭据，变更后通知上层同步数据。',['API凭据','编辑抽屉','模型权限']),
'ApiCredentialTablePanel.tsx':('展示凭据和分组权限，协调创建、编辑、启停、删除与复制密钥；界面提示配额已集中到限流页管理。',['API凭据','列表组件','界面编排'])}
d={
'useApiCredentialMutations':'组合七种凭据变更操作，除只读明文获取外均在成功后刷新凭据与额度查询。',
'ApiCredentialCreateModal':'创建已命名且绑定分组的凭据，展示返回的明文密钥并在确认保存后允许关闭。',
'ApiCredentialEditDrawer':'在选中凭据变化时初始化名称与分组草稿，分别提交修改并反馈更新结果。',
'ApiCredentialTablePanel':'维护凭据列表的创建编辑状态，执行启停、删除和明文复制，结合分组白名单展示允许模型。'}
p='frontend/src/features/manage-api-credentials/';e=[]
for fn in ['ApiCredentialCreateModal','ApiCredentialEditDrawer','ApiCredentialTablePanel']:
 e.append(('function:'+p+'ui/'+fn+'.tsx:'+fn,'function:'+p+'model/useApiCredentialMutations.ts:useApiCredentialMutations','calls'))
for fn in ['ApiCredentialCreateModal','ApiCredentialEditDrawer']:
 e.append(('function:'+p+'ui/ApiCredentialTablePanel.tsx:ApiCredentialTablePanel','function:'+p+'ui/'+fn+'.tsx:'+fn,'contains'))
b.code(67,s,d,e)
