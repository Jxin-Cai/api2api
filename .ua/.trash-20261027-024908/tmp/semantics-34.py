from importlib.machinery import SourceFileLoader
b=SourceFileLoader('b','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/semantic-builder-worker7.py').load_module()
s={'apiCredentialApi.ts':('封装 API 凭据的列表、创建、明文查看及属性和状态变更，统一标识、配额和计量响应字段。',['凭据管理','接口适配','响应规范化']),
'types.ts':('定义 API 凭据、模型分组、令牌配额及明文密钥相关的请求响应契约。',['凭据管理','类型定义','配额']),
'useApiCredentials.ts':('缓存 API 凭据列表，并为选择控件派生名称和标识选项。',['凭据管理','查询钩子','选择选项'])}
d={'normalizeCredential':'将后端凭据标识、配额及用量规范化为前端类型，并为缺省集合与状态设置默认值。','listApiCredentials':'附带浏览器时区查询凭据列表，并逐项规范化响应。','createApiCredential':'提交凭据创建请求，将凭据信息和一次性明文密钥合并为前端响应。','revealApiCredentialSecret':'请求指定凭据的明文密钥，统一凭据标识及密钥字段名称。','renameApiCredential':'修改凭据显示名称并返回规范化凭据。','changeApiCredentialModelGroup':'更新凭据所属模型分组并返回规范化凭据。','changeApiCredentialTokenLimit':'更新凭据令牌上限并返回规范化凭据。','enableApiCredential':'启用指定 API 凭据并返回其最新状态。','disableApiCredential':'禁用指定 API 凭据并返回其最新状态。','deleteApiCredential':'对编码后的凭据路径发送删除请求。','useApiCredentials':'查询并缓存凭据列表，派生以名称优先、标识兜底的下拉选项。'}
p='frontend/src/entities/api-credential/';e=[('function:'+p+'model/useApiCredentials.ts:useApiCredentials','function:'+p+'api/apiCredentialApi.ts:listApiCredentials','calls')]
b.code(34,s,d,e)
