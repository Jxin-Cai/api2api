'use strict';
const fs = require('node:fs');
const path = require('node:path').posix;

function increment(map, key, amount = 1) { map[key] = (map[key] || 0) + amount; }
function append(map, key, value) { (map[key] ||= []).push(value); }
function sortedUnique(values) { return [...new Set(values)].sort(); }
const directoryPatterns = {
  api: ['routes', 'api', 'controllers', 'controller', 'endpoints', 'handlers', 'routers', 'blueprints', 'serializers', 'ohs'],
  service: ['services', 'core', 'lib', 'domain', 'logic', 'internal', 'composables', 'mailers', 'jobs', 'channels', 'signals', 'application'],
  data: ['models', 'db', 'data', 'persistence', 'repository', 'entities', 'entity', 'migrations', 'sql', 'database', 'schema'],
  ui: ['components', 'views', 'pages', 'ui', 'layouts', 'screens'],
  middleware: ['middleware', 'plugins', 'interceptors', 'guards'],
  utility: ['utils', 'helpers', 'common', 'shared', 'tools', 'pkg', 'templatetags'],
  config: ['config', 'constants', 'env', 'settings', 'management', 'commands'],
  test: ['__tests__', 'test', 'tests', 'spec', 'specs'],
  types: ['types', 'interfaces', 'schemas', 'contracts', 'dtos', 'dto', 'request', 'response'],
  hooks: ['hooks'], state: ['store', 'state', 'reducers', 'actions', 'slices'],
  assets: ['assets', 'static', 'public'], entry: ['cmd', 'bin'],
  documentation: ['docs', 'documentation', 'wiki'],
  infrastructure: ['deploy', 'deployment', 'infra', 'infrastructure', 'infr', 'k8s', 'kubernetes', 'helm', 'charts', 'terraform', 'tf', 'docker'],
  'ci-cd': ['.github', '.gitlab', '.circleci']
};
const directoryLabel = new Map(Object.entries(directoryPatterns).flatMap(([label, dirs]) => dirs.map(dir => [dir, label])));
function filePattern(filePath) {
  const name = path.basename(filePath);
  if (/\.(test|spec)\./.test(name) || /^test_.*\.py$|_test\.go$|Test\.java$|_spec\.rb$|Test\.php$|Tests\.cs$/.test(name)) return 'test';
  if (/\.d\.ts$/.test(name)) return 'types';
  if (/^(index\.(ts|js)|__init__\.py|Application\.java|Program\.cs|config\.ru)$/.test(name)) return 'entry';
  if (name === 'manage.py' && !filePath.includes('/')) return 'entry';
  if (/\/cmd\/[^/]+\/main\.go$/.test('/' + filePath) || /\/src\/(main|lib)\.rs$/.test('/' + filePath)) return 'entry';
  if (/^(wsgi\.py|asgi\.py|Cargo\.toml|go\.mod|Gemfile|pom\.xml|build\.gradle|composer\.json|package(-lock)?\.json)$/.test(name)) return 'config';
  if (/^Dockerfile(?:\.|$)|^docker-compose\./.test(name) || /\.(tf|tfvars)$/.test(name) || name === 'Makefile') return 'infrastructure';
  if (/^\.github\/workflows\//.test(filePath) || /^(\.gitlab-ci\.yml|Jenkinsfile)$/.test(name)) return 'ci-cd';
  if (/\.sql$/.test(name)) return 'data';
  if (/\.(graphql|gql|proto)$/.test(name)) return 'types';
  if (/\.(md|rst)$/.test(name)) return 'documentation';
  if (/\.config\.|^tsconfig.*\.json$|^\.env(?:\.|$)|^application.*\.(yml|yaml|properties)$/.test(name)) return 'config';
  return null;
}
function semanticGroup(node) {
  const p = node.filePath;
  const java = p.match(/^backend\/src\/(main|test)\/java\/com\/api2api\/([^/]+)/);
  if (java) return 'backend/' + (java[2].endsWith('.java') ? 'bootstrap' : java[2]);
  if (p.startsWith('backend/src/main/resources/')) return 'backend/resources';
  if (p.startsWith('backend/src/test/')) return 'backend/test-support';
  const frontend = p.match(/^frontend\/src\/([^/]+)/);
  if (frontend) return 'frontend/' + (frontend[1].includes('.') ? 'entry' : frontend[1]);
  if (p.startsWith('frontend/tests/')) return 'frontend/tests';
  if (p.startsWith('frontend/public/')) return 'frontend/public';
  if (p.startsWith('frontend/scripts/')) return 'frontend/tooling';
  if (p.startsWith('frontend/')) return 'frontend/project';
  if (p.startsWith('backend/')) return 'backend/project';
  return p.includes('/') ? p.split('/')[0] : '(root)';
}
function graphStats(groups, imports, byId) {
  const groupOf = new Map(Object.entries(groups).flatMap(([group, ids]) => ids.map(id => [id, group])));
  const pairCounts = new Map();
  const density = Object.fromEntries(Object.keys(groups).map(g => [g, {internalEdges: 0, totalEdges: 0, density: 0}]));
  const links = Object.fromEntries(Object.keys(groups).map(g => [g, {importsFrom: new Set(), importedBy: new Set()}]));
  for (const edge of imports) {
    if (!byId.has(edge.source) || !byId.has(edge.target)) continue;
    const from = groupOf.get(edge.source), to = groupOf.get(edge.target);
    const key = JSON.stringify([from, to]);
    pairCounts.set(key, (pairCounts.get(key) || 0) + 1);
    density[from].totalEdges++;
    if (from === to) density[from].internalEdges++;
    else { density[to].totalEdges++; links[from].importsFrom.add(to); links[to].importedBy.add(from); }
  }
  for (const value of Object.values(density)) value.density = value.totalEdges ? Number((value.internalEdges / value.totalEdges).toFixed(4)) : 0;
  const frequencies = [...pairCounts].map(([key, count]) => { const [from, to] = JSON.parse(key); return {from, to, count}; }).sort((a, b) => b.count - a.count || a.from.localeCompare(b.from));
  const dependencyDirection = frequencies.filter(e => e.from !== e.to && e.count > (pairCounts.get(JSON.stringify([e.to, e.from])) || 0)).map(e => ({dependent: e.from, dependsOn: e.to, forwardCount: e.count, reverseCount: pairCounts.get(JSON.stringify([e.to, e.from])) || 0}));
  return {interGroupImports: frequencies, intraGroupDensity: density, directoryAdjacency: Object.fromEntries(Object.entries(links).map(([k, v]) => [k, {importsFrom: [...v.importsFrom].sort(), importedBy: [...v.importedBy].sort()}])), dependencyDirection};
}
function analyze(input) {
  const nodes = input.fileNodes;
  if (!Array.isArray(nodes) || !Array.isArray(input.importEdges) || !Array.isArray(input.allEdges)) throw new Error('输入必须包含 fileNodes、importEdges 和 allEdges 数组');
  const byId = new Map(nodes.map(n => [n.id, n]));
  if (byId.size !== nodes.length) throw new Error('输入存在重复节点 ID');
  for (const n of nodes) if (!n.id || !n.type || !n.filePath) throw new Error('输入节点缺少必需字段');
  const dirs = nodes.map(n => path.dirname(n.filePath).split('/').filter(p => p !== '.'));
  const common = [...(dirs[0] || [])];
  for (const parts of dirs) while (common.some((part, i) => part !== parts[i])) common.pop();
  const prefix = common.length ? common.join('/') + '/' : '';
  const isFlat = dirs.every(parts => parts.length === common.length);
  const directoryGroups = {}, nodeTypeGroups = {}, architectureGroups = {}, patternMatches = {}, filePatternMatches = {};
  const fileFanIn = {}, fileFanOut = {}, adjacency = {}, reverseAdjacency = {};
  for (const n of nodes) {
    const relative = n.filePath.slice(prefix.length);
    const pattern = filePattern(n.filePath);
    const group = isFlat ? pattern || path.extname(n.filePath).slice(1) || 'other' : relative.includes('/') ? relative.split('/')[0] : '(root)';
    append(directoryGroups, group, n.id); append(nodeTypeGroups, n.type, n.id); append(architectureGroups, semanticGroup(n), n.id);
    if (directoryLabel.has(group)) patternMatches[group] = directoryLabel.get(group);
    filePatternMatches[n.id] = {filePattern: pattern, directoryPatterns: n.filePath.split('/').slice(0, -1).filter(s => directoryLabel.has(s)).map(s => ({directory: s, pattern: directoryLabel.get(s)}))};
    fileFanIn[n.id] = 0; fileFanOut[n.id] = 0; adjacency[n.id] = new Set(); reverseAdjacency[n.id] = new Set();
  }
  const imports = input.importEdges.filter(e => byId.has(e.source) && byId.has(e.target));
  for (const e of imports) { adjacency[e.source].add(e.target); reverseAdjacency[e.target].add(e.source); }
  // Fan-in counts importers; fan-out counts imported targets.
  for (const n of nodes) { fileFanIn[n.id] = reverseAdjacency[n.id].size; fileFanOut[n.id] = adjacency[n.id].size; }
  const edges = input.allEdges.filter(e => byId.has(e.source) && byId.has(e.target));
  const crossCounts = new Map(); const nonCodeConnections = [];
  for (const e of edges) {
    const source = byId.get(e.source), target = byId.get(e.target);
    const key = JSON.stringify([source.type, target.type, e.type]);
    if (source.type !== target.type) crossCounts.set(key, (crossCounts.get(key) || 0) + 1);
    if ((source.type === 'file') !== (target.type === 'file')) nonCodeConnections.push({source: e.source, target: e.target, type: e.type});
  }
  const crossCategoryEdges = [...crossCounts].map(([key, count]) => {const [fromType, toType, edgeType] = JSON.parse(key); return {fromType, toType, edgeType, count};}).sort((a,b) => b.count-a.count);
  const paths = sortedUnique(nodes.map(n => n.filePath));
  const infraFiles = paths.filter(p => ['infrastructure', 'ci-cd'].includes(filePattern(p)) || p.split('/').some(s => directoryLabel.get(s) === 'infrastructure') || /nginx\.conf$/.test(p));
  const deploymentTopology = {
    hasDockerfile: paths.some(p => /^Dockerfile(?:\.|$)/.test(path.basename(p))),
    hasCompose: paths.some(p => /^(docker-)?compose(?:\.[^.]+)?\.ya?ml$/.test(path.basename(p))),
    hasK8s: paths.some(p => /(^|\/)(k8s|kubernetes|helm|charts)\//.test(p)),
    hasTerraform: paths.some(p => /\.(tf|tfvars)$/.test(p)),
    hasCI: paths.some(p => filePattern(p) === 'ci-cd'), infraFiles,
    environmentVariants: paths.filter(p => /(?:Dockerfile|compose).*\.(dev|prod|test|staging)(?:\.|$)|application-(dev|prod|test|staging)\./.test(p)),
    relationships: edges.filter(e => ['deploys', 'provisions', 'depends_on', 'contains'].includes(e.type) && (infraFiles.includes(byId.get(e.source).filePath) || infraFiles.includes(byId.get(e.target).filePath)))
  };
  const dataPipeline = {
    schemaFiles: paths.filter(p => /\.(sql|graphql|gql|proto|prisma)$/.test(p) && !/(migration|migrations)\//.test(p)),
    migrationFiles: paths.filter(p => /\/(migration|migrations)\//.test(p)),
    dataModelFiles: paths.filter(p => /\/(models|entity|entities|persistence|repository)\//.test(p) && !/\/test\//.test(p)),
    apiHandlerFiles: paths.filter(p => /\/(routes|controllers|controller|endpoints|handlers|ohs)\//.test(p) && !/\/test\//.test(p)),
    generatedFiles: paths.filter(p => /(^|\/)(generated|__generated__)\//.test(p)),
    relationships: edges.filter(e => ['migrates', 'defines_schema', 'generates'].includes(e.type))
  };
  const groupOf = new Map(Object.entries(directoryGroups).flatMap(([g, ids]) => ids.map(id => [id, g])));
  const documented = new Set();
  for (const n of nodes) if (n.type === 'document' || filePattern(n.filePath) === 'documentation') documented.add(groupOf.get(n.id));
  for (const e of edges) if (e.type === 'documents' && byId.get(e.source).type === 'document') documented.add(groupOf.get(e.target));
  const totalGroups = Object.keys(directoryGroups).length;
  const docCoverage = {groupsWithDocs: documented.size, totalGroups, coverageRatio: Number((documented.size / totalGroups).toFixed(4)), undocumentedGroups: Object.keys(directoryGroups).filter(g => !documented.has(g)), method: '依据同目录文档及输入中的 documents 边计算，不读取源码或文档正文'};
  const stats = graphStats(directoryGroups, imports, byId);
  const architectureStats = graphStats(architectureGroups, imports, byId);
  return {
    scriptCompleted: true, commonPathPrefix: prefix, directoryGroups, nodeTypeGroups, crossCategoryEdges, nonCodeConnections,
    ...stats, patternMatches, filePatternMatches, deploymentTopology, dataPipeline, docCoverage,
    fileStats: {totalFileNodes: nodes.length, uniqueFilePaths: paths.length, filesPerGroup: Object.fromEntries(Object.entries(directoryGroups).map(([g, ids]) => [g, ids.length])), nodeTypeCounts: Object.fromEntries(Object.entries(nodeTypeGroups).map(([t, ids]) => [t, ids.length]))},
    fileFanIn, fileFanOut,
    importAdjacency: Object.fromEntries(Object.entries(adjacency).map(([id, set]) => [id, [...set].sort()])),
    architectureGroups, architectureStats,
    groupSummaries: Object.fromEntries(Object.entries(architectureGroups).map(([g, ids]) => [g, {count: ids.length, pattern: directoryLabel.get(g.split('/').at(-1)) || null, nodes: ids.map(id => {const n = byId.get(id); return {id: n.id, filePath: n.filePath, type: n.type, summary: n.summary, tags: n.tags};})}])),
    edgeStats: {inputImports: input.importEdges.length, validFileImports: imports.length, excludedImports: input.importEdges.length-imports.length, inputAllEdges: input.allEdges.length, fileLevelEdges: edges.length, excludedSubFileOrUnknownEdges: input.allEdges.length-edges.length},
    warnings: input.allEdges.length > edges.length ? ['allEdges 包含子文件级或输入集合以外的端点；结构分析仅使用两个端点均属于 fileNodes 的边。'] : []
  };
}
try {
  if (process.argv.length !== 4) throw new Error('用法：node ua-arch-analyze.js 输入路径 输出路径');
  const input = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const result = analyze(input);
  fs.writeFileSync(process.argv[3], JSON.stringify(result, null, 2) + '\n');
} catch (error) {
  process.stderr.write(error.stack + '\n');
  process.exitCode = 1;
}
