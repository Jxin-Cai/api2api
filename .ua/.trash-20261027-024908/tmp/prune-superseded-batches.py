import json
import re
import shutil
from pathlib import Path

ua = Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua')
inter = ua / 'intermediate'
plan = json.loads((ua / 'tmp/batches-all.json').read_text())
drift = json.loads((ua / 'tmp/inventory-drift.json').read_text())
replaced_paths = set(drift['added'] + drift['modified'] + drift['removed'])
file_types = {'file', 'config', 'document', 'service', 'pipeline', 'table', 'schema', 'resource', 'endpoint'}
outputs = {}
for batch in plan['batches']:
    index = batch['batchIndex']
    candidates = sorted(path for path in inter.glob(f'batch-{index}*.json') if re.fullmatch(rf'batch-{index}(?:-part-\d+)?\.json', path.name))
    nodes = [node for path in candidates for node in json.loads(path.read_text())['nodes']]
    covered = {node.get('filePath') for node in nodes if node.get('type') in file_types}
    expected = {entry['path'] for entry in batch['files']}
    if not expected <= covered:
        raise ValueError(f'批次 {index} 尚未完成，缺少文件：{sorted(expected - covered)}')
    outputs[index] = candidates

removed_ids = set()
for index, paths in outputs.items():
    if index <= 54:
        for path in paths:
            data = json.loads(path.read_text())
            removed_ids.update(node['id'] for node in data['nodes'] if node.get('filePath') in replaced_paths)
backup = ua / 'tmp/original-batches-before-reconciliation'
backup.mkdir(exist_ok=True)
removed_nodes = removed_edges = 0
for index, paths in outputs.items():
    if index > 54:
        continue
    for path in paths:
        data = json.loads(path.read_text())
        shutil.copy2(path, backup / path.name)
        filtered_nodes = [node for node in data['nodes'] if node.get('filePath') not in replaced_paths]
        filtered_edges = [edge for edge in data['edges'] if edge['source'] not in removed_ids]
        removed_nodes += len(data['nodes']) - len(filtered_nodes)
        removed_edges += len(data['edges']) - len(filtered_edges)
        data['nodes'] = filtered_nodes
        data['edges'] = filtered_edges
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
report = {'replacedFiles': sorted(replaced_paths), 'removedOldNodes': removed_nodes, 'removedOldOutgoingEdges': removed_edges, 'backup': str(backup)}
(inter / 'snapshot-reconciliation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({key: value for key, value in report.items() if key != 'replacedFiles'}, ensure_ascii=False))
