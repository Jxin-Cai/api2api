import json
import re
from pathlib import Path

root = Path('/Users/jxin/Desktop/Project/products/2api/api2api')
ua = root / '.ua'
inter = ua / 'intermediate'

sql_summaries = {
    1: '初始化渠道、模型选项、协议转换、API 凭据与用量记录等核心持久化表及索引。',
    2: '对齐领域模型与持久化字段，调整标识符、渠道配置和用量记录的存储结构。',
    3: '写入开发环境使用的初始渠道、协议转换配置及演示数据。',
    4: '初始化并启用多协议请求和响应转换定义，为协议路由提供持久化配置。',
    5: '为用户账号增加密码哈希存储字段，支持账号身份认证。',
    6: '增加渠道优先级及首选模型相关字段，支持模型路由配置。',
    7: '为供应商渠道增加协议映射配置，使渠道能够保存多协议路由信息。',
    8: '增加 API 凭据加密材料字段，供持久化加密后的密钥内容使用。',
    9: '修复历史用量记录的 duration_millis 数据，校正耗时统计。',
    10: '加入 AWS Bedrock Converse 上游协议的六个方向转换定义。',
    11: '更新 Claude 与 Bedrock 转换能力声明，启用进阶能力配置。',
    12: '更新 OpenAI Responses 与 Bedrock 的流式推理转换能力声明。',
    13: '启用 Claude Messages 与 OpenAI Responses 之间的流式工具调用转换能力。',
    14: '更新 Responses 与 Claude 之间的响应特性转换声明。',
    15: '刷新 Claude 协议转换能力描述，使存储中的能力元数据反映新增支持。',
    16: '修复历史用量记录的令牌总量，使统计数据与输入、输出令牌记录一致。',
    17: '加入 Bedrock 原生 Claude Messages 协议定义，保留工具、推理、缓存及上下文管理语义。',
    18: '建立模型组及其与 API 凭据的关联，为历史已删除凭据分配归档分组。',
    19: '将速率限制隔离粒度调整为渠道与模型，并恢复旧逻辑隔离的渠道状态。',
    20: '在该历史迁移版本中移除 Bedrock Claude Messages 配置，保留 Converse 入口。',
    21: '重新加入 Bedrock 原生 Claude Messages 协议及工具、推理、缓存和压缩能力配置。',
    22: '从活动渠道与转换配置中移除已退役的 Bedrock Converse 协议，兼顾历史用量可查询性。',
    23: '启用 Responses 与 Claude 的完整能力桥接配置。',
    24: '创建渠道测评运行与结果数据结构，保存外部探测服务返回的测评信息。',
    25: '加入 OpenAI Images 入站协议及同协议透传定义，图像请求不转换到其他协议。',
    26: '为模型组增加按模型配置的每日实际令牌上限，供组内多个凭据共享。',
    27: '为用量记录增加延迟与客户端 IP 字段，支持请求观测与追踪。',
    28: '启用 Claude 与 Chat Completions 之间的完整能力桥接配置。',
}
css_summaries = {
    'global.css': '定义应用全局布局、基础视觉样式及通用组件外观，使页面共享统一的展示规则。',
    'reset.css': '重置浏览器默认样式，为后续设计令牌与组件样式提供一致的基础。',
    'tokens.css': '集中声明全局 CSS 设计变量，供页面和组件复用颜色、间距、圆角与阴影。',
    'DistributionPieChart.css': '定义分布饼图卡片及图例布局，使指标分布在仪表盘中保持一致的视觉呈现。',
    'MetricCard.css': '定义指标卡片的标题、数值、单位和趋势文字层级，使用设计变量统一颜色与排版。',
    'TopRankList.css': '定义排行榜列表的条目、排名和数据展示样式，配合仪表盘卡片布局。',
    'TrendChart.css': '约束趋势图容器、画布、可滚动图例与多列提示框，避免高密度多序列信息超出视口。',
    'AuroraBackground.module.css': '为极光背景组件提供局部样式，组织背景色层与动画展示。',
    'SpotlightCard.module.css': '为聚光卡片组件提供局部样式，控制卡片容器与光照效果的展示。',
    'TextEffects.module.css': '为文字动效组件提供局部样式，定义文字展示及动画效果。',
}


def edge(edges, source, target, kind, weight=0.5):
    if source != target and not any(e['source'] == source and e['target'] == target and e['type'] == kind for e in edges):
        edges.append({'source': source, 'target': target, 'type': kind, 'weight': weight, 'direction': 'forward'})


def node(identifier, kind, name, file_path, summary, tags, size, line_range=None):
    value = {'id': identifier, 'type': kind, 'name': name, 'filePath': file_path,
             'summary': summary, 'tags': tags,
             'complexity': 'complex' if size > 250 else 'moderate' if size > 80 else 'simple'}
    if line_range:
        value['lineRange'] = line_range
    return value


migration_tables = {}
sql_tables = {}
extracted_41 = json.loads((ua / 'tmp/ua-file-extract-results-41.json').read_text())
for result in extracted_41['results']:
    text = (root / result['path']).read_text()
    definitions = []
    for match in re.finditer(r'^\s*CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([A-Za-z_]\w*)\s*\(', text, re.I | re.M):
        ending = re.search(r'^\s*\);', text[match.end():], re.M)
        if ending is None:
            raise ValueError('无法确定建表语句结束位置：' + result['path'])
        end = match.end() + ending.end()
        name = match.group(1)
        columns = re.findall(r'^\s+([A-Za-z_]\w*)\s+[A-Za-z]', text[match.end():end], re.M)
        columns = [value for value in columns if value.upper() not in {'CONSTRAINT', 'PRIMARY', 'FOREIGN', 'UNIQUE', 'CHECK'}]
        definitions.append({'name': name, 'startLine': text.count('\n', 0, match.start()) + 1, 'endLine': text.count('\n', 0, end) + 1, 'columns': columns})
        migration_tables.setdefault(name, 'table:' + result['path'] + ':' + name)
    sql_tables[result['path']] = definitions

for index in range(38, 47):
    task = json.loads((ua / 'tmp' / f'batch-task-{index}.json').read_text())
    extracted = json.loads((ua / 'tmp' / f'ua-file-extract-results-{index}.json').read_text())
    structures = {item['path']: item for item in extracted['results']}
    nodes, edges = [], []
    for item in task['files']:
        file_path = item['path']
        filename = Path(file_path).name
        text = (root / file_path).read_text()
        structure = structures.get(file_path, {})
        size = item['sizeLines']
        if index in (38, 39):
            identifier = 'service:' + file_path
            backend = index == 38
            summary = ('使用 Maven 构建后端制品并以独立 JRE 镜像运行 Spring Boot 服务，对外暴露 8080 端口。'
                       if backend else '使用 Node 构建前端静态资源，再交由 Nginx 镜像托管并对外提供 80 端口。')
            nodes.append(node(identifier, 'service', filename, file_path, summary, ['容器构建', '多阶段构建', '后端部署' if backend else '前端部署'], size))
            for stage in structure.get('services', []):
                stage_id = identifier + ':' + stage['name']
                build = 'build' in stage['name']
                stage_summary = ('编译并打包后端应用。' if backend else '安装依赖并构建前端静态资源。') if build else ('运行已构建的后端服务。' if backend else '通过 Nginx 托管前端静态资源。')
                nodes.append(node(stage_id, 'service', stage['name'], file_path, stage_summary, ['容器阶段', '构建阶段' if build else '运行阶段'], size, [stage['startLine'], stage['endLine']]))
                edge(edges, identifier, stage_id, 'contains', 1.0)
            target = 'config:backend/pom.xml' if backend else 'config:frontend/package.json'
            edge(edges, identifier, target, 'depends_on', 0.6)
            if not backend:
                edge(edges, identifier, 'config:frontend/nginx.conf', 'depends_on', 0.6)
        elif index == 40:
            identifier = 'pipeline:' + file_path
            nodes.append(node(identifier, 'pipeline', filename, file_path, '通过 GitHub Actions 将仓库内容交付到远程服务器，校验部署配置并执行 Docker Compose 构建启动与服务检查。', ['持续集成', '自动部署', 'GitHub Actions'], size))
            for job in re.findall(r'^  ([a-zA-Z][\w-]*):\s*$', text.split('jobs:', 1)[1], re.M):
                job_id = identifier + ':' + job
                nodes.append(node(job_id, 'pipeline', job, file_path, '执行仓库工作流中声明的校验、构建和部署步骤，协调部署所需的配置及命令。', ['部署作业', '工作流'], size))
                edge(edges, identifier, job_id, 'contains', 1.0)
            for target in ('service:backend/Dockerfile', 'service:frontend/Dockerfile', 'service:docker-compose.yml'):
                edge(edges, identifier, target, 'deploys', 0.7)
        elif index == 41:
            number = int(re.match(r'V(\d+)__', filename).group(1))
            identifier = 'table:' + file_path
            nodes.append(node(identifier, 'table', filename, file_path, sql_summaries[number], ['数据库迁移', 'Flyway', 'PostgreSQL'], size))
            for table in sql_tables.get(file_path, []):
                table_id = 'table:' + file_path + ':' + table['name']
                columns = table.get('columns', [])
                column_names = [column['name'] if isinstance(column, dict) else str(column) for column in columns]
                description = '定义 ' + table['name'] + ' 数据表'
                if column_names:
                    description += '，保存 ' + '、'.join(column_names[:6]) + ' 等字段'
                nodes.append(node(table_id, 'table', table['name'], file_path, description + '。', ['关系数据表', '持久化'], size, [table['startLine'], table['endLine']]))
                edge(edges, identifier, table_id, 'contains', 1.0)
                edge(edges, identifier, table_id, 'migrates', 0.7)
            affected = re.findall(r'^\s*(?:ALTER\s+TABLE|UPDATE|INSERT\s+INTO|DELETE\s+FROM)\s+(?:IF\s+EXISTS\s+)?([A-Za-z_]\w*)', text, re.I | re.M)
            for name in affected:
                if name in migration_tables:
                    edge(edges, identifier, migration_tables[name], 'migrates', 0.7)
        elif index == 42:
            if filename == 'docker-compose.yml':
                identifier = 'service:' + file_path
                nodes.append(node(identifier, 'service', filename, file_path, '编排数据库、后端与前端容器，声明运行环境、端口、数据卷及服务依赖。', ['容器编排', '运行环境', 'Docker Compose'], size))
                for service in re.findall(r'^  ([a-zA-Z][\w-]*):\s*$', text.split('services:', 1)[1].split('\nvolumes:', 1)[0], re.M):
                    service_id = identifier + ':' + service
                    nodes.append(node(service_id, 'service', service, file_path, '声明 ' + service + ' 容器的镜像或构建配置、运行变量及服务依赖。', ['容器服务', '部署配置'], size))
                    edge(edges, identifier, service_id, 'contains', 1.0)
                for target in ('service:backend/Dockerfile', 'service:frontend/Dockerfile'):
                    edge(edges, identifier, target, 'deploys', 0.7)
            elif filename == '.env.example':
                identifier = 'config:' + file_path
                nodes.append(node(identifier, 'config', filename, file_path, '提供 Docker Compose 运行变量示例，用于配置镜像、服务和运行环境。', ['环境变量示例', '运行配置'], size))
                edge(edges, identifier, 'service:docker-compose.yml', 'configures', 0.6)
            else:
                identifier = 'document:' + file_path
                summary = ('介绍多协议 AI 网关的能力、前后端技术栈及开发部署方式，是理解项目用途与入口的概览。' if filename == 'README.md' else '记录仓库分支协作、异常处理、配置安全、日志、测试和代码质量规范，供开发协作参考。')
                nodes.append(node(identifier, 'document', filename, file_path, summary, ['项目概览', '使用文档'] if filename == 'README.md' else ['协作规范', '开发约定'], size))
                if filename == 'README.md':
                    edge(edges, identifier, 'service:docker-compose.yml', 'documents', 0.5)
                    edge(edges, identifier, 'file:backend/src/main/java/com/api2api/Api2ApiApplication.java', 'documents', 0.5)
                    edge(edges, identifier, 'file:frontend/src/main.tsx', 'documents', 0.5)
        elif index == 43:
            if filename == 'index.html':
                identifier = 'file:' + file_path
                nodes.append(node(identifier, 'file', filename, file_path, '声明前端页面的 HTML 外壳、根挂载元素和应用模块入口，由 Vite 加载 React 应用。', ['页面入口', '前端启动', 'HTML'], size))
                edge(edges, identifier, 'file:frontend/src/main.tsx', 'depends_on', 0.6)
            else:
                identifier = 'config:' + file_path
                if filename == '.env.example':
                    summary = '提供前端构建与开发环境变量的示例，供本地配置应用运行环境。'
                    tags = ['环境变量示例', '前端配置']
                elif filename == 'package.json':
                    summary = '声明 React 管理端的运行与开发依赖，以及 Vite 开发构建、TypeScript 检查和测试脚本。'
                    tags = ['依赖管理', '前端构建', '开发脚本']
                else:
                    scope = {'tsconfig.app.json': '浏览器应用源码', 'tsconfig.node.json': 'Node 环境中的构建工具代码', 'tsconfig.test.json': '前端测试代码', 'tsconfig.json': '前端工程及其引用的子项目'}[filename]
                    summary = '为' + scope + '定义 TypeScript 编译选项、类型环境和纳入范围。'
                    tags = ['类型检查', '编译配置', 'TypeScript']
                nodes.append(node(identifier, 'config', filename, file_path, summary, tags, size))
                for relative in re.findall(r'"(?:path|extends)"\s*:\s*"([^\"]+)"', text):
                    target = (root / file_path).parent / relative
                    target = target.resolve()
                    if target.is_file() and target.is_relative_to(root):
                        edge(edges, identifier, 'config:' + str(target.relative_to(root)), 'depends_on', 0.6)
                if filename in ('package.json', 'tsconfig.app.json'):
                    edge(edges, identifier, 'file:frontend/src/main.tsx', 'configures', 0.6)
        elif index in (44, 45, 46):
            identifier = 'file:' + file_path
            tags = ['样式', '设计变量'] if filename == 'tokens.css' else ['样式', '仪表盘组件' if 'dashboard-metric' in file_path else '全局基础' if '/app/styles/' in file_path else '视觉动效']
            nodes.append(node(identifier, 'file', filename, file_path, css_summaries[filename], tags, size))
            if filename == 'global.css':
                for dependency in ('reset.css', 'tokens.css'):
                    if dependency in text:
                        edge(edges, identifier, 'file:frontend/src/app/styles/' + dependency, 'imports', 0.7)
            if 'var(--' in text and filename != 'tokens.css':
                edge(edges, identifier, 'file:frontend/src/app/styles/tokens.css', 'depends_on', 0.6)
        else:
            raise ValueError(index)
    for source, targets in task['batchImportData'].items():
        for target in targets:
            target_path = target if isinstance(target, str) else target.get('path', target.get('target'))
            if target_path:
                edge(edges, 'file:' + source, 'file:' + target_path, 'imports', 0.7)
    expected = {item['path'] for item in task['files']}
    actual = {entry['filePath'] for entry in nodes}
    assert expected == actual, (index, expected - actual, actual - expected)
    assert len({entry['id'] for entry in nodes}) == len(nodes)
    output = inter / f'batch-{index}.json'
    output.write_text(json.dumps({'nodes': nodes, 'edges': edges}, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'batch': index, 'files': len(expected), 'nodes': len(nodes), 'edges': len(edges)}, ensure_ascii=False))
