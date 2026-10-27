from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={'types.ts':('定义用户创建表单和最近操作结果的前端状态结构。',['用户管理','类型定义','表单状态']),
'useUserAccountMutations.ts':('组合创建用户、修改显示名或角色、启用和禁用操作，成功后记录结果并使用户查询缓存失效。',['用户管理','变更钩子','缓存刷新']),
'UserAccountActionPanel.tsx':('展示用户列表与最近操作对象，协调创建弹窗及按用户标识打开的编辑抽屉。',['用户管理','操作面板','界面编排']),
'UserCreateModal.tsx':('提供用户名、显示名、密码和角色的创建表单，重置弹窗状态并在创建成功后回调。',['用户管理','创建弹窗','表单校验']),
'UserEditDrawer.tsx':('按目标用户加载详情，提供显示名、角色及启停状态编辑，并显示异步操作反馈。',['用户管理','编辑抽屉','操作反馈'])}
d={'useUserAccountMutations':'统一管理五类用户变更与最近结果，在操作成功时刷新用户查询缓存。','UserAccountActionPanel':'渲染用户操作入口和列表，维护创建及编辑界面的打开状态并刷新更新后的用户数据。','UserCreateModal':'初始化创建表单，校验必填输入并提交用户创建请求，展示创建结果。','UserEditDrawer':'加载用户详情并同步编辑草稿，分别提交显示名、角色和启停操作并反馈结果。'}
p='frontend/src/features/manage-user-accounts/';e=[]
for fn in ['UserCreateModal','UserEditDrawer']:
 e.append(('function:'+p+'ui/'+fn+'.tsx:'+fn,'function:'+p+'model/useUserAccountMutations.ts:useUserAccountMutations','calls'))
 e.append(('function:'+p+'ui/UserAccountActionPanel.tsx:UserAccountActionPanel','function:'+p+'ui/'+fn+'.tsx:'+fn,'contains'))
b.code(29,s,d,e)
