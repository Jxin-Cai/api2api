import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path

root = Path('/Users/jxin/Desktop/Project/products/2api/api2api')
ua = root / '.ua'
inter = ua / 'intermediate'
graph = json.loads((inter / 'assembled-graph.json').read_text())
scan = json.loads((inter / 'scan-result.json').read_text())
node_ids = {node['id'] for node in graph['nodes']}
known_prefixes = ('file:', 'config:', 'document:', 'service:', 'pipeline:', 'table:', 'schema:', 'resource:', 'endpoint:')
normalizations = []


def references(values):
    result = []
    for value in values:
        value = value.get('id') if isinstance(value, dict) else value
        if not isinstance(value, str):
            normalizations.append('丢弃非字符串节点引用')
            continue
        if not value.startswith(known_prefixes) and value not in node_ids:
            value = 'file:' + value
        if value not in node_ids:
            normalizations.append('移除悬空引用：' + value)
        elif value not in result:
            result.append(value)
    return result


layers = json.loads((inter / 'layers.json').read_text())
if isinstance(layers, dict):
    layers = layers['layers']
for index, layer in enumerate(layers, 1):
    if 'nodeIds' not in layer and 'nodes' in layer:
        layer['nodeIds'] = layer.pop('nodes')
    if not layer.get('id'):
        layer['id'] = 'layer:' + str(index)
        normalizations.append('补充层级ID：' + layer['id'])
    layer['nodeIds'] = references(layer.get('nodeIds', []))
    for field in ('id', 'name', 'description'):
        if not isinstance(layer.get(field), str) or not layer[field].strip():
            raise ValueError('层级缺少有效字段：' + field)

tour = json.loads((inter / 'tour.json').read_text())
if isinstance(tour, dict):
    tour = tour.get('steps', tour.get('tour'))
if not isinstance(tour, list):
    raise ValueError('导览必须是数组')
for step in tour:
    if 'nodeIds' not in step and 'nodesToInspect' in step:
        step['nodeIds'] = step.pop('nodesToInspect')
    if 'description' not in step and 'whyItMatters' in step:
        step['description'] = step.pop('whyItMatters')
    step['nodeIds'] = references(step.get('nodeIds', []))
    if not isinstance(step.get('order'), int):
        raise ValueError('导览缺少整数 order')
    for field in ('title', 'description'):
        if not isinstance(step.get(field), str) or not step[field].strip():
            raise ValueError('导览缺少有效字段：' + field)
    if 'languageLesson' in step and not isinstance(step['languageLesson'], str):
        raise ValueError('languageLesson 必须是字符串')
tour.sort(key=lambda step: step['order'])
commit = json.loads((inter / 'final-snapshot-review.json').read_text())['currentCommit']
current_commit = subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()
if current_commit != commit:
    normalizations.append('分析过程中 HEAD 发生变化；图谱仍标记为扫描开始时的提交，请随后运行增量更新。')
graph.update({
    'version': '1.0.0',
    'project': {
        'name': scan.get('name', 'api2api'),
        'languages': scan['languages'],
        'frameworks': scan['frameworks'],
        'description': '面向 AI 应用的多协议网关与管理平台，提供鉴权、模型映射、协议转换、供应商路由、故障切换和用量治理。',
        'analyzedAt': datetime.now(timezone.utc).isoformat(),
        'gitCommitHash': commit,
    },
    'layers': layers,
    'tour': tour,
})
(inter / 'assembled-graph.json').write_text(json.dumps(graph, ensure_ascii=False, indent=2) + '\n')
(inter / 'normalization-report.json').write_text(json.dumps({'notes': normalizations}, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'nodes': len(graph['nodes']), 'edges': len(graph['edges']), 'layers': len(layers), 'tourSteps': len(tour), 'normalizations': normalizations}, ensure_ascii=False))
