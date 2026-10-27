import json
import re
from pathlib import Path

root = Path('/Users/jxin/Desktop/Project/products/2api/api2api')
ua = root / '.ua'
inter = ua / 'intermediate'
task = json.loads((ua / 'tmp/batch-task-66.json').read_text())
extract = json.loads((ua / 'tmp/ua-file-extract-results-66.json').read_text())
known = {}
for output in inter.glob('batch-*.json'):
    for entry in json.loads(output.read_text()).get('nodes', []):
        known[entry['id']] = entry

repository_methods = {
    'findAll': '按规则标识符倒序读取全部令牌配额规则。',
    'findApplicable': '按模型组与 API 密钥筛选启用的配额规则，按标识符顺序返回以供准入校验。',
    'save': '根据规则标识符选择插入或更新作用域、目标、模型、周期、限额及启用状态，并返回持久化结果。',
    'delete': '按标识符物理删除配额规则，并报告是否实际删除了记录。',
    'targets': '合并未删除的全局模型组和 API 密钥，返回包含所属用户及模型组的配额目标目录。',
    'targetExists': '根据枚举作用域选择模型组或密钥表，确认配额目标存在且未软删除。',
    'lockCredential': '通过 SELECT FOR UPDATE 锁定密钥记录，供事务内串行处理配额准入。',
    'lockGroup': '通过 SELECT FOR UPDATE 锁定模型组记录，协调共享配额的并发准入。',
    'consumed': '按模型组或密钥、可选模型和半开时间窗口，累计成功及待结算用量的加权实际令牌数。',
}
mapper_methods = {
    'insert': '将用量持久化对象映射为命名参数并插入用量记录。',
    'updateById': '按标识符更新用量记录的状态、令牌、延迟、客户端信息和路由失败等字段。',
    'deleteById': '按标识符将用量记录标记为已删除。',
    'selectById': '按用量记录标识符查询未删除的数据并映射为持久化对象。',
    'selectByRequestId': '按请求标识符查询对应的未删除用量记录。',
    'selectByFilter': '按用户、凭据、渠道、模型、协议和时间条件分页查询用量记录。',
    'countByFilter': '使用相同过滤条件统计用量记录总数，供分页响应使用。',
    'sumTotalTokensByApiCredential': '汇总指定凭据未删除记录的 total_tokens 字段，空聚合返回零。',
    'sumActualTokensByApiCredential': '使用统一令牌权重 SQL 汇总指定凭据的实际令牌消耗，空结果返回零。',
    'sumActualTokensByModelGroupAndModel': '关联凭据与用量记录，按模型组、模型和半开时间窗口累计加权实际令牌数，并排除已删除数据。',
    'sumActualTokensByOwnerGroupedByModel': '按用户和时间窗口查询实际令牌消耗，再按模型组和请求模型分组返回。',
    'sumTokensByFilter': '按筛选条件分别汇总输入、输出、缓存和总令牌数，并合并用量已知标记。',
    'whereClause': '构造始终排除已删除记录的命名参数过滤条件，支持可选身份、模型、协议和半开时间范围。',
    'params': '将用量对象的身份、路由、令牌、延迟、客户端 IP 及审计字段转换为 JDBC 命名参数。',
    'nullableLong': '读取 ResultSet 的可空长整数字段，区分数据库 NULL 和数值零。',
}
test_methods = {
    'test_uses_calendar_boundaries_when_timezone_is_shanghai': '验证上海时区下日、周等配额窗口以业务日历边界计算。',
    'test_resets_correctly_when_period_crosses_calendar_boundary': '验证跨越日历周期边界后，配额窗口的起止与重置时间正确推进。',
}
nodes, edges = [], []


def add_edge(source, target, kind, weight):
    value = {'source': source, 'target': target, 'type': kind, 'weight': weight, 'direction': 'forward'}
    if source != target and value not in edges:
        edges.append(value)


def make_node(identifier, kind, name, path, summary, tags, line_range=None):
    result = {'id': identifier, 'type': kind, 'name': name, 'filePath': path, 'summary': summary, 'tags': tags,
              'complexity': 'complex' if name == 'JdbcUsageRecordMapper' else 'moderate' if kind != 'function' else 'simple'}
    if line_range:
        result['lineRange'] = line_range
    return result


def table(name):
    matches = [value['id'] for value in known.values() if value['type'] == 'table' and value['name'] == name]
    return matches[0] if len(matches) == 1 else None


for result in extract['results']:
    path = result['path']
    name = Path(path).stem
    is_test = name.endswith('Test')
    summaries = test_methods if is_test else repository_methods if name == 'JdbcTokenQuotaRepository' else mapper_methods
    summary = ('覆盖业务时区下配额周期窗口与跨日历边界重置的单元测试。' if is_test else
               '通过 JDBC 持久化统一令牌配额规则，提供作用域目标目录、准入事务行锁及成功和待结算用量的周期消耗统计。' if name == 'JdbcTokenQuotaRepository' else
               '实现用量记录的 JDBC 写入、软删除、分页筛选和令牌聚合，包含按凭据、模型组及用户统计实际消耗的查询。')
    file_id = 'file:' + path
    tags = ['单元测试', '配额周期'] if is_test else ['JDBC', '统一配额' if name == 'JdbcTokenQuotaRepository' else '用量持久化']
    nodes.append(make_node(file_id, 'file', Path(path).name, path, summary, tags))
    classes = result.get('classes', [])
    for cls in classes:
        class_id = 'class:' + path + ':' + cls['name']
        nodes.append(make_node(class_id, 'class', cls['name'], path, summary, tags, [cls['startLine'], cls['endLine']]))
        add_edge(file_id, class_id, 'contains', 1.0)
        source = (root / path).read_text()
        for interface in re.findall(r'implements\s+([A-Za-z_]\w*)', source):
            allowed = set(task['batchImportData'].get(path, []))
            matches = [value for value in known.values() if value['type'] == 'class' and value['name'] == interface and value.get('filePath') in allowed]
            if len(matches) == 1:
                add_edge(class_id, matches[0]['id'], 'implements', 0.9)
    functions = {}
    for function in result.get('functions', []):
        if function['name'] not in summaries:
            raise ValueError('缺少方法语义：' + function['name'])
        function_id = 'function:' + path + ':' + name + '.' + function['name']
        functions[function['name']] = function_id
        nodes.append(make_node(function_id, 'function', function['name'], path, summaries[function['name']], ['测试用例', '周期边界'] if is_test else ['仓储方法', '配额查询' if name == 'JdbcTokenQuotaRepository' else '用量查询'], [function['startLine'], function['endLine']]))
        add_edge(file_id, function_id, 'contains', 1.0)
        if classes:
            add_edge('class:' + path + ':' + classes[0]['name'], function_id, 'contains', 1.0)
    for call in result.get('callGraph', []):
        if call['caller'] in functions and call['callee'] in functions:
            add_edge(functions[call['caller']], functions[call['callee']], 'calls', 0.8)
    if name == 'JdbcTokenQuotaRepository':
        read_tables = {'findAll': ['token_quota_rules'], 'findApplicable': ['token_quota_rules'], 'targets': ['model_groups', 'api_credentials', 'user_accounts'], 'targetExists': ['model_groups', 'api_credentials'], 'lockCredential': ['api_credentials'], 'lockGroup': ['model_groups'], 'consumed': ['usage_records']}
        for method, table_names in read_tables.items():
            for table_name in table_names:
                target = table(table_name)
                if target:
                    add_edge(functions[method], target, 'reads_from', 0.5)
        target = table('token_quota_rules')
        if target:
            for method in ('save', 'delete'):
                add_edge(functions[method], target, 'writes_to', 0.5)
    elif name == 'JdbcUsageRecordMapper':
        target = table('usage_records')
        if target:
            for method in functions:
                if method in ('whereClause', 'params', 'nullableLong'):
                    continue
                add_edge(functions[method], target, 'writes_to' if method in ('insert', 'updateById', 'deleteById') else 'reads_from', 0.5)
        credentials = table('api_credentials')
        if credentials:
            for method in ('sumActualTokensByModelGroupAndModel', 'sumActualTokensByOwnerGroupedByModel'):
                add_edge(functions[method], credentials, 'reads_from', 0.5)
    else:
        add_edge('file:backend/src/main/java/com/api2api/application/quota/TokenQuotaWindow.java', file_id, 'tested_by', 0.5)
    for target in task['batchImportData'].get(path, []):
        add_edge(file_id, 'file:' + target, 'imports', 0.7)
assert {item['path'] for item in task['files']} == {entry['filePath'] for entry in nodes}
assert len({entry['id'] for entry in nodes}) == len(nodes)
(inter / 'batch-66.json').write_text(json.dumps({'nodes': nodes, 'edges': edges}, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'batch': 66, 'files': 3, 'nodes': len(nodes), 'edges': len(edges)}, ensure_ascii=False))
