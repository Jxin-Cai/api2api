import json
from pathlib import Path

root = Path('/Users/jxin/Desktop/Project/products/2api/api2api')
ua = root / '.ua'
inter = ua / 'intermediate'
known_nodes = []
for output in inter.glob('batch-*.json'):
    data = json.loads(output.read_text())
    known_nodes.extend(data.get('nodes', []))


def add_edge(edges, source, target, kind, weight):
    value = {'source': source, 'target': target, 'type': kind, 'weight': weight, 'direction': 'forward'}
    if value not in edges:
        edges.append(value)


def make_node(identifier, kind, name, path, summary, tags, lines=None):
    result = {'id': identifier, 'type': kind, 'name': name, 'filePath': path, 'summary': summary, 'tags': tags, 'complexity': 'moderate' if 'TablePanel' in path else 'simple'}
    if lines:
        result['lineRange'] = lines
    return result


for index in (68, 69, 70):
    task = json.loads((ua / 'tmp' / f'batch-task-{index}.json').read_text())
    extract = json.loads((ua / 'tmp' / f'ua-file-extract-results-{index}.json').read_text())
    structures = {entry['path']: entry for entry in extract['results']}
    nodes, edges = [], []
    for item in task['files']:
        path = item['path']
        filename = Path(path).name
        file_id = 'file:' + path
        structure = structures[path]
        if index == 68:
            summary = '注册模型分组、令牌限流、运维、用户、渠道、模型映射与协议转换管理路由，统一要求 ADMIN 角色并提供 Suspense 加载态；旧元数据入口保留查询参数重定向到转换页。'
            nodes.append(make_node(file_id, 'file', filename, path, summary, ['管理路由', '角色鉴权', '懒加载']))
            functions = {'withAdminGuard': '为管理页面组合 ADMIN 角色鉴权与 Suspense 加载状态，阻止非管理员访问。', 'LegacyProtocolMetadataRedirect': '读取旧元数据入口的查询参数，并用替换导航转到管理端协议转换页。'}
            for function in structure.get('functions', []):
                if function['name'] not in functions:
                    continue
                function_id = 'function:' + path + ':' + function['name']
                nodes.append(make_node(function_id, 'function', function['name'], path, functions[function['name']], ['路由适配', '访问控制'], [function['startLine'], function['endLine']]))
                add_edge(edges, file_id, function_id, 'contains', 1.0)
            routes = [
                ('model-groups', '模型分组', 'AdminModelGroupsPage'),
                ('token-quotas', '令牌配额', 'AdminTokenQuotasPage'),
                ('operations', '运行运维', 'AdminOperationsPage'),
                ('users', '用户管理', 'AdminUsersPage'),
                ('channels', '渠道管理', 'AdminChannelsPage'),
                ('model-mappings', '模型映射', 'AdminModelMappingsPage'),
                ('conversions', '协议转换', 'AdminConversionsPage'),
                ('protocol-metadata', '旧协议元数据入口', None),
            ]
            for name, label, page in routes:
                route_id = 'endpoint:' + path + ':admin-' + name
                nodes.append(make_node(route_id, 'endpoint', '/admin/' + name, path, '声明' + label + '的前端路由，并应用统一管理员访问保护。', ['前端路由', '管理员入口']))
                add_edge(edges, file_id, route_id, 'contains', 1.0)
                add_edge(edges, route_id, 'function:' + path + ':withAdminGuard', 'middleware', 0.5)
                if page:
                    add_edge(edges, route_id, 'file:frontend/src/pages/' + page + '.tsx', 'routes', 0.5)
                else:
                    add_edge(edges, route_id, 'function:' + path + ':LegacyProtocolMetadataRedirect', 'calls', 0.8)
            add_edge(edges, 'function:' + path + ':LegacyProtocolMetadataRedirect', 'endpoint:' + path + ':admin-conversions', 'routes', 0.5)
        elif index == 69:
            nodes.append(make_node(file_id, 'file', filename, path, '定义 API 凭据状态、列表与创建响应、密钥揭示结果、重命名和模型组切换请求类型，包含模型白名单、限流模型、令牌用量及密钥预览等字段。', ['凭据契约', '请求响应类型', '共享业务数据']))
        else:
            form = 'FormModal' in filename
            summary = ('提供模型组创建和编辑表单，维护名称及模型白名单，允许保留暂不可用的模型；保存前去除名称首尾空白并在成功后回调和关闭。'
                       if form else '展示管理员全局模型组、白名单与有效模型、绑定密钥数，提供创建编辑及绑定保护下的确认删除，并通过统一配额查询派生绑定计数。')
            nodes.append(make_node(file_id, 'file', filename, path, summary, ['模型分组管理', '表单交互' if form else '管理列表']))
            for function in structure.get('functions', []):
                function_id = 'function:' + path + ':' + function['name']
                nodes.append(make_node(function_id, 'function', function['name'], path, summary, ['模型分组', '业务组件'], [function['startLine'], function['endLine']]))
                add_edge(edges, file_id, function_id, 'contains', 1.0)
                if any(export['name'] == function['name'] for export in structure.get('exports', [])):
                    add_edge(edges, file_id, function_id, 'exports', 0.8)
                for call in structure.get('callGraph', []):
                    if call['caller'] != function['name']:
                        continue
                    targets = [node for node in known_nodes if node.get('type') == 'function' and node.get('name') == call['callee'] and node.get('filePath') in task['neighborMap']]
                    if len(targets) == 1:
                        add_edge(edges, function_id, targets[0]['id'], 'calls', 0.8)
            if not form:
                add_edge(edges, 'function:' + path + ':ModelGroupTablePanel', 'function:frontend/src/features/manage-model-groups/ui/ModelGroupFormModal.tsx:ModelGroupFormModal', 'calls', 0.8)
    for source, targets in task['batchImportData'].items():
        for target in targets:
            add_edge(edges, 'file:' + source, 'file:' + target, 'imports', 0.7)
    expected = {entry['path'] for entry in task['files']}
    assert expected == {entry['filePath'] for entry in nodes}
    assert len({entry['id'] for entry in nodes}) == len(nodes)
    (inter / f'batch-{index}.json').write_text(json.dumps({'nodes': nodes, 'edges': edges}, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'batch': index, 'files': len(expected), 'nodes': len(nodes), 'edges': len(edges)}, ensure_ascii=False))
