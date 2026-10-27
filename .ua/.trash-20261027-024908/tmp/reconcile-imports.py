import json
from pathlib import Path

ua = Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua')
inter = ua / 'intermediate'
graph_path = inter / 'assembled-graph.json'
graph = json.loads(graph_path.read_text())
scan = json.loads((inter / 'scan-result.json').read_text())
file_types = {'file', 'config', 'document', 'service', 'pipeline', 'table', 'schema', 'resource', 'endpoint'}
by_path = {}
for entry in graph['nodes']:
    if entry['type'] not in file_types or not entry.get('filePath'):
        continue
    path = entry['filePath']
    rank = (entry['id'] != entry['type'] + ':' + path, len(entry['id']))
    if path not in by_path or rank < by_path[path][0]:
        by_path[path] = (rank, entry['id'])
old_imports = {(edge['source'], edge['target']) for edge in graph['edges'] if edge['type'] == 'imports'}
new_imports = set()
missing = []
for source_path, targets in scan['importMap'].items():
    for target_path in targets:
        if source_path not in by_path or target_path not in by_path:
            missing.append({'source': source_path, 'target': target_path})
        elif source_path != target_path:
            new_imports.add((by_path[source_path][1], by_path[target_path][1]))
if missing:
    raise ValueError('导入关系缺少文件级节点：' + json.dumps(missing, ensure_ascii=False))
graph['edges'] = [edge for edge in graph['edges'] if edge['type'] != 'imports']
graph['edges'].extend({'source': source, 'target': target, 'type': 'imports', 'weight': 0.7, 'direction': 'forward'} for source, target in sorted(new_imports))
graph_path.write_text(json.dumps(graph, ensure_ascii=False, indent=2) + '\n')
report = {'oldImports': len(old_imports), 'currentImports': len(new_imports), 'removedStaleImports': len(old_imports - new_imports), 'restoredImports': len(new_imports - old_imports)}
(inter / 'import-reconciliation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(report, ensure_ascii=False))
