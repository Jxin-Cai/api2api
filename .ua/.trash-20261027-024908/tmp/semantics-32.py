from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={'loginApi.ts':('封装用户名密码登录请求，返回当前用户登录响应。',['用户登录','接口访问','认证']),
'types.ts':('定义用户名密码登录请求、表单值和成功回调参数类型。',['用户登录','类型定义','表单契约']),
'useLoginByUsername.ts':('将登录接口封装为变更钩子，在登录成功后使当前用户查询失效。',['用户登录','变更钩子','会话刷新']),
'LoginForm.tsx':('提供用户名密码登录表单与校验，成功后按重定向参数或用户角色跳转，失败时显示通知。',['用户登录','表单组件','导航'])}
d={'loginByUsername':'提交用户名和密码到登录接口，返回类型化登录响应。','useLoginByUsername':'执行登录变更并提取响应数据，成功后刷新当前用户查询。','LoginForm':'渲染登录表单，规范化用户名并提交认证，根据角色或重定向参数导航并反馈错误。'}
p='frontend/src/features/login-by-username/';e=[('function:'+p+'model/useLoginByUsername.ts:useLoginByUsername','function:'+p+'api/loginApi.ts:loginByUsername','calls'),('function:'+p+'ui/LoginForm.tsx:LoginForm','function:'+p+'model/useLoginByUsername.ts:useLoginByUsername','calls')]
b.code(32,s,d,e)
