import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'UserAccountMapper.java':('定义账户新增、更新、按标识和用户名查询及列表读取的数据库接口。',['数据访问接口','账户存储','持久化契约']),
'UserAccountPO.java':('保存用户表的标识、用户名、显示名、角色、状态、密码哈希和通用审计删除字段。',['持久化对象','账户存储','审计字段']),
'BCryptPasswordHasher.java':('使用 BCrypt 对有效口令编码并验证明文与已存密码哈希的匹配，通过应用层接口隔离算法实现。',['密码哈希','认证实现','口令校验']),
'AdminUserController.java':('提供后台账户列表、创建、显示名与角色变更及启停接口，解析当前操作者并转交账户应用服务。',['管理员接口','账户管理','请求适配']),
'AdminUserHttpConverter.java':('使用 MapStruct 将管理员账户请求转为带操作者的应用命令，并将账户领域模型转换为列表及详情响应。',['账户转换','命令映射','响应映射']),
'AdminChangeUserDisplayNameRequest.java':('承载管理员更新用户显示名的请求及参数约束。',['请求模型','用户资料','参数校验']),
'AdminChangeUserRoleRequest.java':('承载管理员更新目标用户角色的请求参数。',['请求模型','角色变更','参数校验']),
'AdminCreateUserRequest.java':('承载管理员创建账户的用户名、显示名、角色和口令请求，并检查必填字段。',['请求模型','账户创建','参数校验']),
'UserAccountListResponse.java':('包装管理员账户列表中的账户详情响应。',['响应模型','账户列表','数据传输']),
'UserAccountResponse.java':('输出账户标识、用户名、显示名、角色、启用状态和审计时间。',['响应模型','账户详情','数据传输']),
'AuthController.java':('提供会话登录、退出、当前用户读取和自助资料更新接口，负责会话身份绑定及清理并转换认证请求响应。',['认证接口','会话管理','用户资料']),
'AuthHttpConverter.java':('将登录及资料更新请求转换为应用命令，并将账户映射为登录和当前用户响应。',['认证转换','命令映射','响应映射']),
'CurrentUserResponse.java':('输出当前会话用户的标识、用户名、显示名、角色和账户状态。',['响应模型','当前用户','会话认证']),
'LoginRequest.java':('承载登录用户名和口令并提供必填参数约束。',['请求模型','登录认证','参数校验']),
'LoginResponse.java':('包装成功登录后的当前用户信息。',['响应模型','登录认证','用户信息']),
'UpdateCurrentUserProfileRequest.java':('承载当前用户自助修改显示名的请求，并校验输入。',['请求模型','用户资料','参数校验']),
'MapStructConfig.java':('统一 OHS 转换器的 Spring 组件模型与构造注入策略，对未映射目标字段发出编译警告。',['映射配置','依赖注入','编译检查']),
'BootstrapAdminInitializerTest.java':('验证启动管理员初始化对已有密码哈希保持不变，并为缺少哈希的历史管理员补全配置口令。',['初始化测试','密码兼容','行为验证']),
'UserAccountApplicationServiceTest.java':('验证管理员创建账户会哈希并保存口令，以及当前用户自助修改显示名的持久化行为。',['账户服务测试','口令哈希','用户资料']),
'AuthControllerTest.java':('验证当前用户资料更新接口正确传递会话身份及显示名，并返回映射后的当前用户。',['认证接口测试','会话身份','用户资料'])}
d['UsageQueryApplicationServiceTest.java']=('验证用户本人用量记录查询会补充关联密钥名称，使门户无需额外查询即可展示来源密钥。',['用量查询测试','密钥名称','用户门户'])
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('AdminUserController.java',{'listUsers':'从会话读取操作者并查询后台用户列表，转换为统一响应。','createUser':'生成用户标识，将管理员创建请求转换为命令并返回新账户。','changeDisplayName':'绑定操作者与目标用户，将显示名请求转换为命令并返回更新账户。','changeRole':'绑定操作者与目标用户，提交新角色变更并返回更新账户。','disableUser':'由当前管理员禁用目标用户并返回统一响应。','enableUser':'由当前管理员启用目标用户并返回统一响应。'})
add('AdminUserHttpConverter.java',{'toListResponse':'逐个映射账户领域模型，包装为后台用户列表响应。','toUsername':'将用户名字符串转换为带格式校验的领域值对象。','toDisplayName':'将显示名字符串转换为带约束的领域值对象。'})
add('AuthController.java',{'login':'将登录请求转为命令，认证后轮换并绑定会话，返回用户登录信息。','logout':'清理当前会话用户并返回成功响应。','getCurrentUser':'从会话解析用户身份，读取启用账户并返回当前用户信息。','updateCurrentUserProfile':'将会话用户和新显示名转换为资料更新命令，保存后返回当前用户响应。'})
add('AuthHttpConverter.java',{'toUsername':'将登录用户名转换为领域用户名值对象。','toDisplayName':'将资料显示名转换为领域显示名值对象。'})
add('BCryptPasswordHasher.java',{'hash':'验证输入口令后以 BCrypt 编码为密码哈希值对象。','matches':'验证明文口令与已存 BCrypt 哈希的匹配，兼容缺失哈希输入。'})
add('BootstrapAdminInitializerTest.java',{'test_keepsExistingPasswordHash_whenAdminAlreadyHasHash':'已存在管理员具有密码哈希时，验证初始化不会覆盖其原有凭据。','test_upgradesPasswordHash_whenExistingAdminHasNoHash':'历史管理员缺少密码哈希时，验证初始化使用配置密码补全哈希。'})
add('UserAccountApplicationServiceTest.java',{'test_createUser_persistsAccountWithPasswordHash_when_adminCreatesUser':'管理员创建用户时，验证应用服务将明文口令哈希后保存到新账户。','test_updatesCurrentUserDisplayName_when_userSubmitsProfile':'当前用户提交新显示名时，验证服务更新本人账户并持久化。'})
add('AuthControllerTest.java',{'test_updatesProfile_when_currentUserSubmitsDisplayName':'当前会话用户提交显示名时，验证控制器转换正确身份的命令并返回更新后的当前用户。'})
a='backend/src/main/java/com/api2api/'
o['_edges']=[('class:'+a+'infr/security/BCryptPasswordHasher.java:BCryptPasswordHasher','class:'+a+'application/user/PasswordHasher.java:PasswordHasher','implements',0.9)]
add('UsageQueryApplicationServiceTest.java',{'test_exposes_api_credential_name_when_querying_my_usage_records':'查询当前用户调用记录时，验证服务通过密钥仓储补充密钥名称并保留在用量视图中。'})
m.build(19,d,o)
