'use strict';
const fs = require('node:fs');
const root = '/Users/jxin/Desktop/Project/products/2api/api2api/.ua';
const results = JSON.parse(fs.readFileSync(root + '/tmp/ua-arch-results.json', 'utf8'));
if (!results.scriptCompleted) throw new Error('结构分析尚未成功完成');
const layers = [
  {
    id: 'layer:backend-api',
    name: '后端 HTTP 接入层',
    description: '在独立 Spring Boot 后端的 ohs/http 中提供网关、管理端与用户门户控制器、请求响应模型、鉴权过滤和全局异常处理，并包含对应接入测试。',
    nodeIds: []
  },
  {
    id: 'layer:backend-application',
    name: '后端应用编排层',
    description: '在 application 中编排网关执行、渠道与模型、凭据与配额、测评、用户、用量和运维用例，定义命令、查询及外部能力端口并验证其行为。',
    nodeIds: []
  },
  {
    id: 'layer:backend-domain',
    name: '后端领域层',
    description: '在 domain 中定义渠道、路由、协议契约、凭据、配额、测评与用量分析的领域模型、规则和仓储接口，并通过领域测试验证业务约束。',
    nodeIds: []
  },
  {
    id: 'layer:backend-infrastructure',
    name: '后端适配与持久化层',
    description: '在 infr 中实现上游客户端、协议与流式转换、仓储、安全和宿主机监控适配，承载数据库迁移及表结构、协议工具资源、Spring Boot 启动装配和相应测试。',
    nodeIds: []
  },
  {
    id: 'layer:frontend-composition',
    name: '前端应用与页面装配层',
    description: '在独立 React 管理端的 app、pages 与 widgets 中装配路由及管理员端点、全局提供者、布局、主题、页面与业务面板，并承载 HTML 和 React 启动入口。',
    nodeIds: []
  },
  {
    id: 'layer:frontend-features',
    name: '前端业务功能层',
    description: '在 features 中实现登录、角色导航、渠道与模型管理、凭据与配额、用户设置、协议浏览、用量筛选和运维查看的交互流程及相关功能测试。',
    nodeIds: []
  },
  {
    id: 'layer:frontend-entities',
    name: '前端业务实体层',
    description: '在 entities 中维护渠道、模型、分组、凭据、配额、用户、用量、协议与仪表盘指标的前端类型、接口访问、查询状态和实体展示组件。',
    nodeIds: []
  },
  {
    id: 'layer:frontend-shared',
    name: '前端共享基础层',
    description: '在 shared 中提供管理端通用 API 访问、查询参数处理、基础 UI、工具函数、共享类型和配置，并包含查询参数一致性测试。',
    nodeIds: []
  },
  {
    id: 'layer:delivery',
    name: '工程配置与部署交付层',
    description: '分别维护后端 Maven 与前端 Vite、TypeScript 和测试工具配置，提供运行环境模板、后端运行配置、独立 Docker 构建阶段、Nginx、Compose 服务编排及 GitHub Actions 部署。',
    nodeIds: []
  },
  {
    id: 'layer:documentation',
    name: '项目与运维文档层',
    description: '说明 API2API 网关用途、独立前后端开发部署方式、仓库协作规范以及宿主机资源监控的配置与排障方法。',
    nodeIds: []
  }
];
const byLayer = new Map(layers.map(layer => [layer.id, layer]));
const directGroups = {
  'backend/ohs': 'layer:backend-api',
  'backend/application': 'layer:backend-application',
  'backend/domain': 'layer:backend-domain',
  'backend/infr': 'layer:backend-infrastructure',
  'backend/bootstrap': 'layer:backend-infrastructure',
  'frontend/app': 'layer:frontend-composition',
  'frontend/pages': 'layer:frontend-composition',
  'frontend/widgets': 'layer:frontend-composition',
  'frontend/entry': 'layer:frontend-composition',
  'frontend/features': 'layer:frontend-features',
  'frontend/entities': 'layer:frontend-entities',
  'frontend/shared': 'layer:frontend-shared',
  'frontend/tooling': 'layer:delivery'
};
const testAssignments = {
  'file:frontend/tests/modelAvailability.test.ts': 'layer:frontend-features',
  'file:frontend/tests/providerChannelForm.test.ts': 'layer:frontend-features',
  'file:frontend/tests/queryString.test.ts': 'layer:frontend-shared'
};
const reasons = [];
function selectLayer(group, node) {
  if (node.type === 'document') return ['layer:documentation', '文档节点归入项目与运维文档'];
  if (['service', 'resource', 'pipeline'].includes(node.type)) return ['layer:delivery', '容器、构建阶段、编排服务及流水线属于部署交付'];
  if (node.filePath === 'backend/src/main/resources/protocol/claude-client-toolsets-20260801.json') return ['layer:backend-infrastructure', '协议工具参数资源由后端协议适配职责承载'];
  if (node.type === 'config') return ['layer:delivery', '工程与运行配置保留各自前后端项目路径并归入工程交付'];
  if (['table', 'schema'].includes(node.type)) return ['layer:backend-infrastructure', '数据库迁移与嵌套表节点归入后端持久化'];
  if (testAssignments[node.id]) return [testAssignments[node.id], '独立测试按所验证的功能或共享工具归层'];
  if (node.filePath === 'frontend/index.html') return ['layer:frontend-composition', 'HTML 挂载入口属于 React 应用装配'];
  if (directGroups[group]) return [directGroups[group], '沿实际架构目录归层，包含包内测试和嵌套路由节点'];
  if (['backend/project', 'frontend/project', '.github', '(root)'].includes(group)) return ['layer:delivery', '项目构建、运行及交付支持'];
  throw new Error('无法确定节点职责：' + node.id + '，目录组：' + group);
}
for (const [group, summary] of Object.entries(results.groupSummaries)) {
  for (const node of summary.nodes) {
    const [layerId, reason] = selectLayer(group, node);
    byLayer.get(layerId).nodeIds.push(node.id);
    reasons.push({nodeId: node.id, layerId, reason});
  }
}
for (const layer of layers) layer.nodeIds.sort();
const expectedIds = Object.values(results.nodeTypeGroups).flat();
const known = new Set(expectedIds);
const assigned = layers.flatMap(layer => layer.nodeIds);
const frequencies = new Map();
for (const id of assigned) frequencies.set(id, (frequencies.get(id) || 0) + 1);
const duplicates = [...frequencies].filter(([, count]) => count !== 1).map(([id, count]) => ({id, count}));
const missing = expectedIds.filter(id => !frequencies.has(id));
const dangling = assigned.filter(id => !known.has(id));
const han = /\p{Script=Han}/u;
const invalidLayers = layers.filter(layer => !/^layer:[a-z0-9]+(?:-[a-z0-9]+)*$/.test(layer.id) || !han.test(layer.name) || !han.test(layer.description) || !layer.nodeIds.length || Object.keys(layer).sort().join(',') !== 'description,id,name,nodeIds');
const valid = layers.length >= 3 && layers.length <= 10 && expectedIds.length === 931 && assigned.length === expectedIds.length && known.size === expectedIds.length && duplicates.length === 0 && missing.length === 0 && dangling.length === 0 && invalidLayers.length === 0;
const warnings = [
  'allEdges 的 8929 条关系中有 6324 条未连接两个输入文件级节点；结构分析已排除这些子文件级或集合外关系，保留 2605 条文件级关系及全部 2214 条导入关系。',
  '前端跨架构目录的导入边仅见于独立测试，前端分层主要依据目录和节点摘要；这不代表前端架构层之间没有依赖。',
  '931 是节点数而非文件路径数；904 个不同路径包含同路径的表、容器构建阶段、编排服务、流水线任务和路由节点，均单独计入归属。'
];
const validation = {
  valid,
  layerCount: layers.length,
  knownNodeCount: expectedIds.length,
  assignedNodeCount: assigned.length,
  uniqueAssignedNodeCount: frequencies.size,
  uniqueFilePathCount: results.fileStats.uniqueFilePaths,
  duplicates, missing, dangling, invalidLayerIds: invalidLayers.map(layer => layer.id),
  chineseNamesAndDescriptions: layers.every(layer => han.test(layer.name) && han.test(layer.description)),
  allNodesAssignedExactlyOnce: valid,
  nodeTypeCounts: results.fileStats.nodeTypeCounts,
  layerCounts: layers.map(layer => ({id: layer.id, name: layer.name, count: layer.nodeIds.length})),
  warnings
};
fs.writeFileSync(root + '/tmp/ua-arch-validation.json', JSON.stringify(validation, null, 2) + '\n');
if (!valid) throw new Error('层级校验失败，请查看 ua-arch-validation.json');
fs.mkdirSync(root + '/intermediate', {recursive: true});
fs.writeFileSync(root + '/intermediate/layers.json', JSON.stringify(layers, null, 2) + '\n');
fs.writeFileSync(root + '/tmp/ua-arch-assignments.json', JSON.stringify(reasons, null, 2) + '\n');
process.stdout.write(JSON.stringify(validation, null, 2) + '\n');
