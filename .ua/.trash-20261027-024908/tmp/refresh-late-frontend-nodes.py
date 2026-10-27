import json
import re
from pathlib import Path

ua = Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua')
inter = ua / 'intermediate'
graph_path = inter / 'assembled-graph.json'
graph = json.loads(graph_path.read_text())
scan = json.loads((inter / 'scan-result.json').read_text())
files = {entry['path']: entry for entry in scan['files']}
results = json.loads((ua / 'tmp/api-credential-refresh.json').read_text())['results'] + json.loads((ua / 'tmp/final-refresh-results.json').read_text())['results']
function_summaries = {
    'normalizeCredential': '归一化凭据响应的模型组、白名单、限流模型、每日用量、总量和限额字段，并用旧版模型组名称作为兼容回退。',
    'credentialPath': '编码凭据标识符及可选资源后缀，生成当前用户凭据接口的请求路径。',
    'listApiCredentials': '携带浏览器时区读取当前用户的 API 密钥列表，并逐项归一化凭据响应。',
    'createApiCredential': '提交创建请求，返回新密钥及归一化后的凭据信息。',
    'revealApiCredentialSecret': '请求当前用户有权限访问的指定 API 密钥内容。',
    'renameApiCredential': '更新指定 API 密钥的名称，并归一化返回的凭据信息。',
    'changeApiCredentialModelGroup': '切换指定 API 密钥绑定的模型组，并归一化返回的凭据信息。',
    'enableApiCredential': '启用指定 API 密钥并返回归一化后的凭据信息。',
    'disableApiCredential': '停用指定 API 密钥并返回归一化后的凭据信息。',
    'deleteApiCredential': '删除指定 API 密钥。',
    'ApiCredentialToolbar': '显示凭据说明、本地名称或标识符搜索、列表刷新和新建密钥入口，并通过回调传递用户操作。',
    'useModelGroupMutations': '封装模型组创建、更新与删除操作；创建和删除后刷新模型组及配额缓存，更新后同时刷新凭据权限相关缓存。',
}
file_summaries = {
    'apiCredentialApi.ts': '封装当前用户 API 密钥的列表、创建、揭示、重命名、模型组切换、启停和删除请求，并统一归一化凭据与每日用量字段。',
    'ApiCredentialToolbar.tsx': '提供 API 密钥列表的搜索、刷新和创建入口，以回调连接列表状态与用户操作。',
    'useModelGroupMutations.ts': '为模型组管理界面提供创建、更新、删除 mutation，并在成功后使模型组、配额和凭据缓存按操作类型失效。',
}
notes = []


def edge(source, target, kind, weight):
    value = {'source': source, 'target': target, 'type': kind, 'weight': weight, 'direction': 'forward'}
    if source != target and value not in graph['edges']:
        graph['edges'].append(value)


for result in results:
    path = result['path']
    owned = [node for node in graph['nodes'] if node.get('filePath') == path]
    old_functions = {node['name'].rsplit('.', 1)[-1]: node for node in owned if node['type'] == 'function'}
    file_node = next(node for node in owned if node['type'] == 'file')
    old_source_ids = {node['id'] for node in owned}
    graph['edges'] = [item for item in graph['edges'] if item['source'] not in old_source_ids or (item['source'] == file_node['id'] and item['type'] == 'tested_by')]
    graph['nodes'] = [node for node in graph['nodes'] if node.get('filePath') != path or node['type'] != 'function']
    file_node['summary'] = file_summaries[Path(path).name]
    functions = {}
    for function in result['functions']:
        name = function['name']
        previous = old_functions.get(name, {})
        identifier = previous.get('id', 'function:' + path + ':' + name)
        entry = {**previous, 'id': identifier, 'type': 'function', 'name': name, 'filePath': path,
                 'summary': function_summaries[name], 'tags': previous.get('tags', ['前端业务逻辑']),
                 'lineRange': [function['startLine'], function['endLine']], 'complexity': previous.get('complexity', 'simple')}
        graph['nodes'].append(entry)
        functions[name] = identifier
        edge(file_node['id'], identifier, 'contains', 1.0)
        if any(export['name'] == name for export in result.get('exports', [])):
            edge(file_node['id'], identifier, 'exports', 0.8)
    source = (ua.parent / path).read_text()
    for cls in [node for node in owned if node['type'] == 'class']:
        match = re.search(r'^[ \t]*(?:export\s+)?(?:interface|type|class)\s+' + re.escape(cls['name']) + r'\b', source, re.M)
        if match:
            opening = source.find('{', match.end())
            if opening >= 0:
                depth = 0
                for position in range(opening, len(source)):
                    if source[position] == '{': depth += 1
                    if source[position] == '}':
                        depth -= 1
                        if depth == 0:
                            cls['lineRange'] = [source.count('\n', 0, match.start()) + 1, source.count('\n', 0, position) + 1]
                            break
            edge(file_node['id'], cls['id'], 'contains', 1.0)
            if source[match.start():match.end()].lstrip().startswith('export '):
                edge(file_node['id'], cls['id'], 'exports', 0.8)
    for call in result.get('callGraph', []):
        if call['caller'] not in functions:
            continue
        if call['callee'] in functions:
            edge(functions[call['caller']], functions[call['callee']], 'calls', 0.8)
        elif call['callee'] in {'createModelGroup', 'updateModelGroup', 'deleteModelGroup', 'browserTimeZone'}:
            candidates = [node for node in graph['nodes'] if node['type'] == 'function' and node['name'] == call['callee'] and node.get('filePath', '').startswith('frontend/')]
            if len(candidates) == 1:
                edge(functions[call['caller']], candidates[0]['id'], 'calls', 0.8)
    for target in scan['importMap'][path]:
        edge(file_node['id'], 'file:' + target, 'imports', 0.7)
    notes.append({'file': path, 'currentFunctions': sorted(functions), 'removedOldFunctions': sorted(set(old_functions) - set(functions))})
ids = {node['id'] for node in graph['nodes']}
graph['edges'] = [item for item in graph['edges'] if item['source'] in ids and item['target'] in ids]
clamped = []
for node in graph['nodes']:
    if node.get('filePath') in {'backend/Dockerfile', 'frontend/Dockerfile'} and node.get('lineRange') and node['lineRange'][1] > files[node['filePath']]['sizeLines']:
        node['lineRange'][1] = files[node['filePath']]['sizeLines']
        clamped.append(node['id'])
graph_path.write_text(json.dumps(graph, ensure_ascii=False, indent=2) + '\n')
(inter / 'late-source-corrections.json').write_text(json.dumps({'refreshedFiles': notes, 'normalizedDockerEndLines': clamped}, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'refreshedFiles': len(results), 'normalizedDockerEndLines': len(clamped), 'nodes': len(graph['nodes']), 'edges': len(graph['edges'])}, ensure_ascii=False))
