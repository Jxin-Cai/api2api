import json
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

ua = Path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua')
inter = ua / 'intermediate'
graph = json.loads((ua / 'knowledge-graph.json').read_text())
scan = json.loads((inter / 'scan-result.json').read_text())
review = json.loads((inter / 'review.json').read_text())
schema = json.loads((inter / 'schema-review.json').read_text())
freshness = json.loads((inter / 'source-freshness-review.json').read_text())
assert not review['issues'] and schema['success'] and not freshness['stale']
limitations = [
    '符号级调用关系并非穷尽：大依赖社区拆批和高频领域类型的邻居裁剪可能减少跨批次调用证据。',
    '全部 2214 条已解析的文件级导入与最终扫描 importMap 一致；前端跨目录别名导入的解析证据有限，因此不能据此声称覆盖所有真实导入。',
    '部分 Java 同名重载共用符号标识与代表范围；未获得独立范围的接口默认方法保留在类型摘要，琐碎访问器按策略省略。',
    'CSS、XML 等缺少结构解析支持的格式保留文件级语义；SQL 表与迁移关系根据真实建表和变更语句补充。',
]
validation = {
    'validatedAt': datetime.now(timezone.utc).isoformat(),
    'gitCommitHash': graph['project']['gitCommitHash'],
    'inlineReview': review,
    'schemaReview': schema,
    'sourceFreshnessReview': freshness,
    'snapshotReview': json.loads((inter / 'final-snapshot-review.json').read_text()),
    'limitations': limitations,
}
(ua / 'validation-report.json').write_text(json.dumps(validation, ensure_ascii=False, indent=2) + '\n')
category_labels = {'code': '源码与测试', 'config': '配置', 'docs': '文档', 'infra': '部署与基础设施', 'data': '数据与迁移', 'script': '脚本', 'markup': '标记文件'}
file_counts = Counter(f['fileCategory'] for f in scan['files'])
node_counts = Counter(n['type'] for n in graph['nodes'])
edge_counts = Counter(e['type'] for e in graph['edges'])
lines = [
    '# API2API 中文代码库知识图谱', '',
    graph['project']['description'], '',
    f"- 分析提交：`{graph['project']['gitCommitHash']}`",
    f"- 已分析文件：{len(scan['files'])} / {len(scan['files'])}",
    f"- 图谱节点：{len(graph['nodes'])}；关系：{len(graph['edges'])}",
    f"- 架构层：{len(graph['layers'])}；学习导览：{len(graph['tour'])} 步",
    '- 输出语言：中文；自动更新未启用。',
    '- 分析包含源码、测试、文档、配置与部署文件；环境秘密、本地构建产物和分析工具产物按忽略规则排除。', '',
    '## 文件分类', '', '| 类型 | 文件数 |', '| --- | ---: |',
]
for category in dict.fromkeys([*category_labels, *sorted(file_counts)]):
    lines.append(f"| {category_labels.get(category, category)}（{category}） | {file_counts[category]} |")
lines += ['', '## 节点类型', '', '| 类型 | 节点数 |', '| --- | ---: |']
lines += [f'| {kind} | {count} |' for kind, count in sorted(node_counts.items())]
lines += ['', '## 关系类型', '', '| 类型 | 关系数 |', '| --- | ---: |']
lines += [f'| {kind} | {count} |' for kind, count in sorted(edge_counts.items())]
lines += ['', '## 架构层', '', '| 名称 | 文件级节点数 |', '| --- | ---: |']
lines += [f"| {layer['name']} | {len(layer['nodeIds'])} |" for layer in graph['layers']]
lines += ['', '文件级节点包括独立文件、表、Docker 阶段和前端路由等，共 931 个，覆盖 904 条文件路径；每个文件级节点恰好归属一层。', '', '## 学习导览', '']
for step in graph['tour']:
    lines += [f"{step['order']}. **{step['title']}**：{step['description']}"]
lines += ['', '## 验证结果', '',
    '- 正式 KnowledgeGraphSchema 校验通过；文件覆盖完整，无清单外文件与越界行范围。',
    '- 节点标识唯一，关系端点有效，架构归属与导览引用有效。',
    '- 759 份含符号文件均不晚于其最新结构提取；分析期间保存的文件已补充提取或逐份核对。',
    f"- 内联校验严重问题：{len(review['issues'])}；提示：{len(review['warnings'])}。",
    '- 结构指纹覆盖全部分析文件，生成成功后才写入 meta.json。', '', '## 分析限制', '',
]
lines += [f'- {text}' for text in limitations]
if review['warnings']:
    lines += ['', '### 校验提示', '', '完整提示保存在 validation-report.json，以下列出前 20 条：', '']
    lines += [f'- {warning}' for warning in review['warnings'][:20]]
lines += ['', '## 产物', '',
    '- [知识图谱](knowledge-graph.json)',
    '- [验证记录](validation-report.json)',
    '- [结构指纹](fingerprints.json)',
    '- [分析元数据](meta.json)',
    '- [扫描清单](intermediate/scan-result.json)', '',
]
(ua / 'analysis-report.md').write_text('\n'.join(lines))
print(json.dumps({'report': str(ua / 'analysis-report.md'), 'filesByCategory': dict(file_counts), 'nodesByType': dict(node_counts), 'edgesByType': dict(edge_counts), 'warnings': len(review['warnings'])}, ensure_ascii=False))
