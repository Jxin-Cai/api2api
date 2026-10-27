import importlib.util,pathlib
p=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-worker-5.py');s=importlib.util.spec_from_file_location('sem',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d={
'ModelGroupRepository.java':('定义模型组保存、按标识与所有者查询、全局目录和全局名称检查，以及密钥绑定检查与软删除契约。',['仓储接口','全局模型组','名称唯一性']),
'ModelGroupRepositoryImpl.java':('将模型组聚合映射为持久化对象并选择插入或更新，提供全局目录与名称唯一性查询，同时保留旧所有者查询及删除保护。',['仓储实现','全局目录','删除保护']),
'JdbcModelGroupMapper.java':('使用具名参数 JDBC 保存模型白名单与兼容日限额 JSON，执行全局及所有者查询、名称与密钥绑定存在性检查及软删除。',['数据访问','全局模型组','参数化查询']),
'ModelGroupMapper.java':('定义模型组表的数据库读写接口，包含全局目录、全局名称检查、所有者查询、密钥绑定和软删除操作。',['数据访问接口','模型组存储','全局目录']),
'ApiCredentialController.java':('提供用户密钥列表、签发、明文揭示、名称与模型组更新、启停和删除接口；列表按时区构建当日用量窗口，新建时生成并加密密钥材料。',['密钥接口','会话身份','密钥材料']),
'ModelGroupController.java':('在管理员路由下提供全局模型组列表和创建、更新、删除接口，绑定会话操作者并返回包含当前有效模型的视图。',['管理接口','全局模型组','请求适配'])}
o={}
def add(f,items):o.update({f+':'+k:v for k,v in items.items()})
add('ModelGroupRepositoryImpl.java',{'save':'将模型组转换为数据库对象，根据标识是否存在选择插入或更新。','findById':'按模型组标识读取未删除数据并恢复领域聚合。','findByOwner':'按所有者读取其历史模型组并恢复领域对象。','findAll':'读取全部未删除模型组并转换为全局目录。','existsByNameExcludingId':'检查排除当前标识后是否已有同名全局模型组。','existsByOwnerAndNameExcludingId':'保留旧所有者范围的模型组名称重复检查。','existsCredentialBinding':'检查目标模型组是否仍有未删除密钥绑定。','delete':'按模型组标识和修改时刻执行软删除。'})
add('JdbcModelGroupMapper.java',{'insert':'插入模型组身份、所有者、名称、白名单及兼容日限额与审计字段。','update':'按标识更新模型组配置和审计时间。','selectById':'读取指定标识的未删除模型组。','selectAll':'按创建时间和标识倒序读取全局未删除模型组。','existsByNameExcludingId':'以参数化存在性查询检查全局同名模型组，排除当前标识和已删除记录。','selectByOwnerUserId':'按所有者查询未删除模型组并保持倒序。','existsByOwnerAndNameExcludingId':'在所有者范围内检查名称冲突，排除当前记录。','existsCredentialBinding':'查询是否存在绑定目标组的未删除密钥。','softDeleteById':'将模型组标记为删除并更新修改时刻。','params':'绑定模型组 SQL 参数，缺失旧日限额时写入空 JSON 对象。'})
add('ApiCredentialController.java',{'listMyCredentials':'从会话读取用户，按请求时区构建今日半开窗口，查询并转换带用量和限流模型的密钥列表。','createCredential':'生成密钥身份和材料，加密明文后转换创建命令，保存后仅本次返回生成的明文。','revealCredentialSecret':'绑定当前用户和目标密钥，执行授权揭示并转换密钥材料响应。','renameCredential':'绑定当前用户和密钥标识，将新名称转换为命令并返回更新密钥。','changeModelGroup':'绑定当前用户和密钥标识，提交新模型组选择并返回更新密钥。','disableCredential':'由当前所有者禁用指定密钥并返回更新状态。','enableCredential':'由当前所有者启用指定密钥并返回更新状态。','deleteCredential':'绑定当前用户和密钥身份，执行软删除命令并返回成功响应。'})
add('ModelGroupController.java',{'listMyGroups':'读取当前操作者并调用管理员全局模型组视图查询，转换为列表响应。','createGroup':'生成模型组身份并绑定操作者创建组，返回含有效支持模型的详情。','updateGroup':'解析路径模型组标识和当前操作者，提交配置更新并返回模型组视图。','deleteGroup':'绑定操作者和模型组标识，执行带权限和绑定约束的删除命令。'})
a='backend/src/main/java/com/api2api/'
o['_edges']=[('class:'+a+'infr/repository/credential/ModelGroupRepositoryImpl.java:ModelGroupRepositoryImpl','class:'+a+'domain/credential/repository/ModelGroupRepository.java:ModelGroupRepository','implements',0.9),('class:'+a+'infr/repository/credential/mapper/JdbcModelGroupMapper.java:JdbcModelGroupMapper','class:'+a+'infr/repository/credential/mapper/ModelGroupMapper.java:ModelGroupMapper','implements',0.9)]
m.build(63,d,o)
