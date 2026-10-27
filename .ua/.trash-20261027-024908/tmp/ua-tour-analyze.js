'use strict';

const fs = require('node:fs');
const path = require('node:path');

function analyze(input) {
  const { nodes, edges: rawEdges, layers } = input;
  if (!Array.isArray(nodes) || !Array.isArray(rawEdges) || !Array.isArray(layers)) {
    throw new Error('输入必须包含 nodes、edges 和 layers 数组');
  }
  const index = new Map(nodes.map(node => [node.id, node]));
  if (index.size !== nodes.length) throw new Error('节点 ID 重复');
  const filesByPath = new Map(nodes.filter(node => node.filePath).map(node => [node.filePath, node.id]));
  function resolveFile(id) {
    if (index.has(id)) return id;
    if (typeof id !== 'string') return null;
    const payload = id.slice(id.indexOf(':') + 1);
    if (filesByPath.has(payload)) return filesByPath.get(payload);
    const separator = payload.indexOf(':');
    return separator < 0 ? null : filesByPath.get(payload.slice(0, separator)) || null;
  }
  const unresolvedEdges = [];
  let foldedEdges = 0;
  let internalEdges = 0;
  const edges = [];
  for (const edge of rawEdges) {
    const source = resolveFile(edge.source);
    const target = resolveFile(edge.target);
    if (!source || !target) {
      unresolvedEdges.push({ source: edge.source, target: edge.target, type: edge.type });
      continue;
    }
    if (source !== edge.source || target !== edge.target) foldedEdges += 1;
    if (source === target) { internalEdges += 1; continue; }
    edges.push({ ...edge, source, target });
  }
  const incoming = new Map(nodes.map(node => [node.id, new Set()]));
  const outgoing = new Map(nodes.map(node => [node.id, new Set()]));
  const adjacency = new Map(nodes.map(node => [node.id, new Set()]));
  const related = new Map(nodes.map(node => [node.id, new Set()]));
  const relationTypes = new Map();
  for (const edge of edges) {
    if (!index.has(edge.source) || !index.has(edge.target)) {
      throw new Error(`边引用不存在的节点：${edge.source} -> ${edge.target}`);
    }
    incoming.get(edge.target).add(edge.source);
    outgoing.get(edge.source).add(edge.target);
    related.get(edge.source).add(edge.target);
    related.get(edge.target).add(edge.source);
    if (edge.type === 'imports' || edge.type === 'calls') {
      adjacency.get(edge.source).add(edge.target);
      const key = JSON.stringify([edge.source, edge.target]);
      if (!relationTypes.has(key)) relationTypes.set(key, new Set());
      relationTypes.get(key).add(edge.type);
    }
  }
  const rank = (degree, field) => nodes.map(node => ({
    id: node.id, [field]: degree.get(node.id).size, name: node.name,
  })).sort((a, b) => b[field] - a[field] || a.id.localeCompare(b.id));
  const fanIn = rank(incoming, 'fanIn');
  const fanOut = rank(outgoing, 'fanOut');
  const highFanOut = new Set(fanOut.slice(0, Math.ceil(nodes.length * 0.1)).map(item => item.id));
  const lowFanInThreshold = [...fanIn].reverse()[Math.max(0, Math.ceil(nodes.length * 0.25) - 1)]?.fanIn ?? 0;
  const entryNames = new Set(['index.ts', 'index.js', 'main.ts', 'main.js', 'main.tsx', 'app.ts', 'app.js', 'server.ts', 'server.js', 'mod.rs', 'main.go', 'main.py', 'main.rs', 'manage.py', 'app.py', 'wsgi.py', 'asgi.py', 'run.py', '__main__.py', 'Application.java', 'Main.java', 'Program.cs', 'config.ru', 'index.php', 'App.swift', 'Application.kt', 'main.cpp', 'main.c']);
  const isDocumentation = node => node.type === 'document' || /\.md$/i.test(node.filePath || node.name || '');
  const isCode = node => node.type === 'file' && /\.(?:[cm]?[jt]sx?|java|py|rs|go|kt|swift|cs|cpp|c|php|ru)$/i.test(node.filePath || node.name || '');
  const candidates = nodes.map(node => {
    const filePath = node.filePath || node.name || '';
    const filename = path.posix.basename(filePath);
    let score = 0;
    if (isDocumentation(node)) {
      if (filePath === 'README.md') score += 5;
      else if (!filePath.includes('/') && /\.md$/i.test(filename)) score += 2;
    } else if (isCode(node)) {
      if (entryNames.has(filename) || /Application\.java$/.test(filename)) score += 3;
      if (filePath.split('/').length <= 2) score += 1;
      if (highFanOut.has(node.id)) score += 1;
      if (incoming.get(node.id).size <= lowFanInThreshold) score += 1;
    }
    return { id: node.id, score, name: node.name, summary: node.summary || '' };
  }).filter(item => item.score > 0).sort((a, b) => b.score - a.score || a.id.localeCompare(b.id));
  const codeStart = candidates.find(item => isCode(index.get(item.id)));
  const order = [];
  const depthMap = {};
  const byDepth = {};
  if (codeStart) {
    order.push(codeStart.id);
    depthMap[codeStart.id] = 0;
    for (let cursor = 0; cursor < order.length; cursor += 1) {
      const id = order[cursor];
      const depth = depthMap[id];
      (byDepth[depth] ??= []).push(id);
      for (const next of adjacency.get(id)) {
        if (Object.hasOwn(depthMap, next)) continue;
        depthMap[next] = depth + 1;
        order.push(next);
      }
    }
  }
  const nonCodeFiles = { documentation: [], infrastructure: [], data: [], config: [] };
  const categories = { document: 'documentation', service: 'infrastructure', pipeline: 'infrastructure', resource: 'infrastructure', table: 'data', schema: 'data', endpoint: 'data', config: 'config' };
  for (const node of nodes) {
    const category = categories[node.type];
    if (category) nonCodeFiles[category].push({ id: node.id, name: node.name, type: node.type, summary: node.summary || '' });
  }
  const clustersByKey = new Map();
  for (const [key, types] of relationTypes) {
    const [source, target] = JSON.parse(key);
    if (source === target || source.localeCompare(target) >= 0) continue;
    const reverse = relationTypes.get(JSON.stringify([target, source]));
    if (!reverse || ![...types].some(type => reverse.has(type))) continue;
    const cluster = new Set([source, target]);
    while (cluster.size < 5) {
      const additions = nodes.filter(node => !cluster.has(node.id)).map(node => ({
        id: node.id, connections: [...cluster].filter(member => related.get(node.id).has(member)).length,
      })).filter(item => item.connections >= 2).sort((a, b) => b.connections - a.connections || a.id.localeCompare(b.id));
      if (!additions.length) break;
      cluster.add(additions[0].id);
    }
    const members = [...cluster].sort();
    const edgeCount = edges.filter(edge => cluster.has(edge.source) && cluster.has(edge.target)).length;
    clustersByKey.set(JSON.stringify(members), { nodes: members, edgeCount });
  }
  const clusters = [...clustersByKey.values()].sort((a, b) => b.edgeCount - a.edgeCount || a.nodes.join().localeCompare(b.nodes.join())).slice(0, 10);
  const nodeSummaryIndex = Object.fromEntries(nodes.map(node => [node.id, {
    name: node.name, type: node.type, summary: node.summary || '', filePath: node.filePath,
  }]));
  return {
    scriptCompleted: true,
    entryPointCandidates: candidates.slice(0, 5),
    fanInRanking: fanIn.slice(0, 20),
    fanOutRanking: fanOut.slice(0, 20),
    bfsTraversal: { startNode: codeStart?.id || null, order, depthMap, byDepth },
    nonCodeFiles,
    clusters,
    layers: { count: layers.length, list: layers.map(({ id, name, description }) => ({ id, name, description })) },
    nodeSummaryIndex,
    totalNodes: nodes.length,
    totalEdges: rawEdges.length,
    fileLevelProjection: {
      foldedEdges, internalEdges, crossFileEdges: edges.length,
      unresolvedEdgeCount: unresolvedEdges.length, unresolvedEdges,
      method: '保留所有原始输入；仅按节点 ID 中精确匹配的 filePath 将符号端点投影到现有文件，文件内部边不参与文件间度数和 BFS。',
    },
    projectContext: {
      projectName: input.projectName,
      projectDescription: input.projectDescription,
      languages: input.languages,
      additionalContext: input.additionalContext,
    },
    analysisNotes: [
      '扇入扇出按不同相邻节点去重计数。',
      'Java 的项目名 Application 类与 main.tsx 纳入入口名称识别。',
      '非代码清单保留输入类型，不根据扩展名改写类型；文件语义可在摘要索引中查询。',
      'BFS 仅跟随输入中的 imports 与 calls；未出现的前端跨目录关系不能据此推断为已验证。',
    ],
  };
}

try {
  const [, , inputPath, outputPath] = process.argv;
  if (!inputPath || !outputPath) throw new Error('用法：node ua-tour-analyze.js 输入.json 输出.json');
  const result = analyze(JSON.parse(fs.readFileSync(inputPath, 'utf8')));
  fs.writeFileSync(outputPath, JSON.stringify(result, null, 2) + '\n');
  process.exitCode = 0;
} catch (error) {
  process.stderr.write(`${error.stack || error}\n`);
  process.exitCode = 1;
}
