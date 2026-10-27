import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'BootstrapAdminInitializer.java':('应用启动时按配置查找管理员，仅为不存在的账户创建管理员，或为缺少密码哈希的历史账户补充管理员身份和密码。保留已有密码账户不变。',['启动初始化','管理员账户','密码配置']),
'BootstrapAdminProperties.java':('绑定启动管理员的用户名与密码配置，供首次初始化和历史账户补全使用。',['配置绑定','管理员账户','启动配置']),
'PasswordHasher.java':('定义口令哈希与明文口令匹配契约，使账户应用服务与具体哈希算法解耦。',['哈希接口','密码认证','应用契约']),
'UserAccountApplicationService.java':('组织登录、自助资料更新和管理员用户管理，执行账户状态及后台权限检查，并通过密码哈希接口与仓储完成持久化。',['账户服务','登录认证','权限管理']),
'ChangeUserDisplayNameCommand.java':('承载管理员修改目标用户显示名的操作人、目标标识与新显示名。',['修改命令','用户管理','显示名称']),
'ChangeUserRoleCommand.java':('承载管理员修改目标用户角色的操作人、目标标识与新角色。',['修改命令','用户管理','角色变更']),
'ChangeUserStatusCommand.java':('承载管理员启用或禁用目标账户的操作人和目标用户标识。',['修改命令','账户状态','用户管理']),
'CreateUserCommand.java':('承载管理员创建用户的身份、用户名、显示名、角色和待哈希口令。',['创建命令','用户管理','账户创建']),
'LoginCommand.java':('承载不可变的登录用户名与明文口令，作为认证用例输入。',['登录命令','账户认证','输入模型']),
'UpdateCurrentUserProfileCommand.java':('承载当前登录用户自助更新显示名的身份及新显示名。',['修改命令','用户资料','自助服务']),
'AccessScope.java':('区分用户门户与管理员后台访问范围，供账户角色授权使用。',['权限范围','枚举类型','访问控制']),
'DisplayName.java':('封装用户显示名并校验去空白后的内容与长度，避免无效资料进入账户模型。',['显示名称','值对象','参数校验']),
'PasswordHash.java':('封装存储的密码哈希，支持可空历史数据与必需哈希构造，防止日志字符串泄露哈希内容。',['密码哈希','值对象','敏感数据']),
'UserAccount.java':('定义用户账户聚合，包含身份、名称、角色、状态及密码哈希，提供资料和角色变更、启停及访问授权约束。',['账户聚合','状态管理','访问控制']),
'UserAccountId.java':('用正数标识封装用户身份，作为账户与其他业务聚合之间的引用。',['用户标识','值对象','身份校验']),
'UserAccountStatus.java':('枚举账户启用和禁用状态。',['账户状态','枚举类型','访问控制']),
'UserRole.java':('定义用户角色及其允许的访问范围，为门户与后台权限判断提供依据。',['用户角色','枚举类型','访问控制']),
'Username.java':('封装用户名并检查去空白后的内容、长度和允许字符。',['用户名','值对象','格式校验']),
'UserAccountRepository.java':('定义账户标识生成、保存、用户名及标识查询和列表读取契约。',['仓储接口','用户账户','持久化契约']),
'BCryptPasswordHasher.java':('使用 BCrypt 编码口令并验证明文口令与已存哈希的匹配，拒绝无效输入。',['密码哈希','认证实现','安全校验']),
'UserAccountRepositoryImpl.java':('通过数据库映射器保存和查询账户，生成业务标识并在领域聚合与持久化对象之间转换。',['仓储实现','账户持久化','对象转换']),
'UserAccountPersistenceConverter.java':('使用 MapStruct 将账户聚合映射为数据库对象，并从持久化字段重建强类型账户及可空历史密码哈希。',['持久化转换','对象映射','账户模型']),
'MapStructConfig.java':('集中配置 MapStruct 的 Spring 组件模型、构造注入、未映射目标字段检查及空值映射策略。',['映射配置','编译检查','依赖注入']),
'JdbcUserAccountMapper.java':('使用命名参数 JDBC 实现账户插入、更新和软删除过滤查询，映射数据库行并统一绑定账户字段及审计时间。',['数据访问','账户存储','参数化查询']),
'UserAccountMapper.java':('定义账户插入、更新、按标识或用户名查询及列表读取的数据访问接口。',['数据映射','账户存储','数据库访问']),
'UserAccountPO.java':('定义用户账户表持久化字段，包括用户名、显示名、角色、状态、密码哈希与审计时间。',['持久化对象','账户存储','审计字段'])}
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('BootstrapAdminInitializer.java',{'run':'按配置用户名查找账户，分支处理现有历史账户和首次创建。','prepareExistingAdmin':'已有密码哈希时保持账户不变，否则恢复管理员启用状态并设置配置密码哈希。','createBootstrapAdmin':'分配账户标识并创建带配置密码哈希的启用管理员，持久化保存。'})
add('UserAccountApplicationService.java',{'login':'按用户名读取账户，统一拒绝缺失、禁用、无哈希或密码不匹配的凭据。','getCurrentUser':'读取并校验当前账户处于启用状态。','updateCurrentUserProfile':'读取当前启用账户，修改显示名并持久化。','listUsers':'校验管理员访问范围后列出账户。','getUser':'校验管理员权限后读取指定用户。','createUser':'校验管理员权限与用户名唯一性，哈希口令并创建保存新账户。','changeDisplayName':'由管理员修改目标账户显示名并保存。','changeRole':'由管理员修改目标账户角色并保存。','disableUser':'由管理员禁用目标账户并保存。','enableUser':'由管理员启用目标账户并保存。'})
add('UserAccount.java',{'create':'以指定身份、角色、名称和密码哈希创建启用账户并设置审计时间。','rehydrate':'从持久化字段恢复账户角色、状态、密码哈希与审计时间。','changeDisplayName':'更新账户显示名并推进修改时间。','changeRole':'校验账户状态后变更角色并推进修改时间。','disable':'将账户置为禁用并更新修改时间。','enable':'恢复账户启用状态并更新修改时间。','changePasswordHash':'更新账户密码哈希并记录修改时间。','ensureAdminActive':'将历史账户调整为启用管理员，供启动补全使用。','assertActive':'拒绝处于禁用状态的账户。','assertCanAccess':'同时校验账户启用状态与角色对目标访问范围的许可。','canAccess':'判断账户是否启用且角色允许目标访问范围。'})
add('UserAccountRepositoryImpl.java',{'save':'将账户聚合转换为数据库对象，按是否已存在执行插入或更新。','findById':'按业务标识查询数据库对象并恢复领域账户。','findByUsername':'按用户名查询数据库对象并恢复领域账户。','findAll':'读取全部账户并转换为领域模型列表。','nextIdentity':'生成下一个用户账户业务标识。'})
add('UserAccountPersistenceConverter.java',{'toDomain':'将数据库账户字段恢复为标识、用户名、显示名、角色、状态和可空密码哈希值对象。'})
add('BCryptPasswordHasher.java',{'hash':'校验明文口令非空后通过 BCrypt 编码为密码哈希值对象。','matches':'验证明文口令与非空 BCrypt 哈希是否匹配。'})
a='backend/src/main/java/com/api2api/'
add('JdbcUserAccountMapper.java',{'insert':'通过具名参数向用户表插入账户状态、角色、密码哈希与审计时间。','update':'按标识更新用户表字段及审计时间。','parameters':'将账户持久化对象展开为具名 SQL 参数，统一处理审计与删除字段。'})
o['_edges']=[('class:'+a+'infr/repository/user/mapper/JdbcUserAccountMapper.java:JdbcUserAccountMapper','class:'+a+'infr/repository/user/mapper/UserAccountMapper.java:UserAccountMapper','implements',0.9),('class:'+a+'infr/repository/user/UserAccountRepositoryImpl.java:UserAccountRepositoryImpl','class:'+a+'domain/user/repository/UserAccountRepository.java:UserAccountRepository','implements',0.9)]
m.build(17,d,o)
