import json, pathlib, math, re, sys
ROOT=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api')
UA=ROOT/('.understand-anything' if (ROOT/'.understand-anything').is_dir() else '.ua')
WEIGHTS={'contains':1.0,'imports':0.7,'calls':0.8,'inherits':0.9,'implements':0.9,'exports':0.8,'depends_on':0.6,'tested_by':0.5,'configures':0.6,'documents':0.5,'deploys':0.7,'migrates':0.7,'triggers':0.6,'defines_schema':0.8,'serves':0.7,'provisions':0.7,'routes':0.6,'related':0.5}
WORDS={'claude':'Claude','responses':'Responses','response':'响应','request':'请求','chat':'Chat Completions','open':'开启','close':'关闭','block':'内容块','reasoning':'推理','thinking':'思考状态','redacted':'脱敏','text':'文本','tool':'工具','tools':'工具定义','calls':'调用','call':'调用','caller':'调用方','callers':'调用方','arguments':'参数','input':'输入','output':'输出','message':'消息','messages':'消息列表','content':'内容','part':'片段','parts':'片段列表','user':'用户','assistant':'助手','system':'系统提示词','model':'模型','parameters':'参数','format':'格式','choice':'选择策略','cache':'缓存','breakpoint':'断点','control':'控制','service':'服务','tier':'等级','metadata':'元数据','usage':'用量','tokens':'令牌','token':'令牌','iteration':'迭代','iterations':'迭代列表','image':'图片','images':'图片列表','document':'文档','file':'文件','filename':'文件名','media':'媒体','type':'类型','mime':'MIME','extension':'扩展名','data':'数据','uri':'URI','base64':'Base64','programmatic':'程序化','program':'程序','code':'代码','execution':'执行','custom':'自定义','function':'函数','mcp':'MCP','server':'服务端','servers':'服务器列表','allowed':'允许的','deferred':'延迟加载','namespace':'命名空间','namespaces':'命名空间','names':'名称列表','name':'名称','schema':'结构','definitions':'定义','fields':'字段','state':'状态','stateful':'有状态','effort':'推理强度','config':'配置','configuration':'配置','budget':'预算','from':'来源','to':'目标','stop':'终止','finish':'完成','reason':'原因','history':'历史','pairing':'配对','consecutive':'相邻','compaction':'上下文压缩','opaque':'不透明','signature':'签名','encrypted':'加密的','item':'条目','items':'条目列表','result':'结果','results':'结果列表','stream':'流','streaming':'流式','sse':'SSE','event':'事件','events':'事件列表','delta':'增量','done':'完成','completed':'已完成','completion':'完成','fallback':'补全','summary':'摘要','visible':'可见','auxiliary':'辅助','indexed':'索引的','id':'标识','json':'JSON','raw':'原始','body':'正文','terminal':'终止标记','known':'已知','unknown':'未知','source':'源','target':'目标','protocol':'协议','direction':'方向','capability':'能力','conversion':'转换','mapping':'映射','mappings':'映射列表','registered':'已注册','converter':'转换器','contract':'契约','contracts':'契约列表','gateway':'网关','parsed':'解析结果','required':'必需','supported':'支持的','valid':'有效','blank':'空白','empty':'空','only':'仅','positive':'正值','default':'默认值','first':'首个','last':'最后','long':'长整数','node':'节点','detailed':'详细','rule':'规则','rules':'规则列表','single':'单条','short':'短','key':'键','prompt':'提示词','with':'包含','without':'去除','new':'新增','fresh':'新','search':'搜索','web':'网络','normalize':'规范化','normalized':'已规范化','repair':'修复','repaired':'已修复','non':'非','negative':'负','nonnegative':'非负','number':'数字','transfer':'传输','extractor':'提取器','bedrock':'Bedrock','invoke':'调用','structured':'结构化','thinkingstate':'思考状态','start':'开始','end':'结束','signed':'签名','persisted':'持久化','explicit':'显式','max':'最大','responseid':'响应标识','use':'使用','useid':'调用标识','toolset':'工具集','container':'容器','conversation':'会话','cacheable':'可缓存','native':'原生','inputtext':'输入文本','outputtext':'输出文本','formatting':'格式化','field':'字段','client':'客户端','public':'公共','stopreason':'停止原因','contenttype':'内容类型','inputschema':'输入结构','codeinterpreter':'代码解释器','multimodal':'多模态','lossiness':'有损级别','endpoint':'端点','line':'行','buffer':'缓冲区','buffers':'缓冲区','linked':'关联','link':'关联','value':'值','values':'值列表','options':'选项','boolean':'布尔值','enabled':'启用状态','properties':'属性','runtime':'运行时','payload':'载荷'}
VERBS={'handle':'处理','map':'映射','convert':'转换','apply':'应用','assemble':'组装','append':'追加','add':'添加','extract':'提取','normalize':'规范化','validate':'校验','resolve':'解析','ensure':'确保','collect':'收集','flush':'输出并清空','restore':'恢复','parse':'解析','encode':'编码','decode':'解码','sanitize':'清理','load':'加载','write':'写出','emit':'发送','finalize':'收尾','record':'记录','mark':'标记','remember':'保存','repair':'修复','merge':'合并','sum':'汇总','find':'查找','describe':'描述','build':'构建','create':'创建','execute':'执行','transfer':'传输','transform':'转换','announce':'通知','close':'关闭','open':'开启','finish':'完成','stop':'停止','throw':'抛出异常','require':'查找并验证'}
def chinese_name(name):
 name=name.replace('OpenAI','Openai').replace('ChatCompletions','Chatcompletions')
 words=re.findall(r'[A-Z]+(?=[A-Z][a-z]|$)|[A-Z]?[a-z]+|\d+',name)
 return ''.join(WORDS.get(w.lower(),w) for w in words)
def summary_func(name,context):
 if name.startswith(('is','has','contains','supports','can')):
  for prefix in ('supports','contains','has','is','can'):
   if name.startswith(prefix): return '判断'+chinese_name(name[len(prefix):])+'，用于'+context+'。'
 for prefix,verb in VERBS.items():
  if name.startswith(prefix) and len(name)>len(prefix): return verb+chinese_name(name[len(prefix):])+'，支撑'+context+'。'
 if re.search(r'To(?=[A-Z])',name):
  a,b=re.split(r'To(?=[A-Z])',name,maxsplit=1);return '将'+chinese_name(a)+'转换为'+chinese_name(b)+'，用于'+context+'。'
 if re.match(r'to[A-Z]',name):return '转换为'+chinese_name(name[2:])+'，用于'+context+'。'
 return '提供'+chinese_name(name)+'，用于'+context+'。'
def complexity(n):return 'simple' if n<50 else 'moderate' if n<=200 else 'complex'
def run(index,meta,overrides=None,extra_edges=None):
 task=json.loads((UA/f'tmp/batch-task-{index}.json').read_text());x=json.loads((UA/f'tmp/ua-file-extract-results-{index}.json').read_text()); overrides=overrides or {};extra_edges=extra_edges or []
 if x.get('filesUnreadable'):raise RuntimeError(x['filesUnreadable'])
 nodes=[];edges=[];seen=set();funcs={};classes={};paths=set();symbol_owners={};results={r['path']:r for r in x['results']}
 def edge(a,b,kind):
  if a!=b and (a,b,kind) not in seen: edges.append({'source':a,'target':b,'type':kind,'direction':'forward','weight':WEIGHTS[kind]});seen.add((a,b,kind))
 for fi in task.get('files',task.get('batchFiles',[])):
  p=fi['path'];r=results.get(p)
  if r is None:
   if p not in x.get('filesSkipped',[]):raise RuntimeError('未提取文件：'+p)
   text=(ROOT/p).read_text()
   r={'path':p,'nonEmptyLines':sum(bool(line.strip()) for line in text.splitlines()),'functions':[],'classes':[],'exports':[]}
   results[p]=r
  paths.add(p);base=pathlib.Path(p).name
  cfg=meta.get(base,meta.get(p))
  if not cfg: raise RuntimeError('缺少语义：'+p)
  summary,tags,context=cfg[:3];category=fi['fileCategory'];typ={'code':'file','config':'config','docs':'document','infra':'service','data':'schema','script':'file','markup':'file'}[category]
  if category=='infra':typ='pipeline' if any(s in p for s in ('.github/workflows','.gitlab-ci','Jenkinsfile','.circleci')) else 'resource' if p.endswith(('.tf','.tfvars')) else 'service'
  if category=='data' and p.endswith('.sql'):typ='table'
  nid=f'{typ}:{p}'
  if typ=='table':nid+=':'+pathlib.Path(p).stem
  n={'id':nid,'type':typ,'name':base,'filePath':p,'summary':summary,'tags':tags,'complexity':complexity(r['nonEmptyLines'])}
  if len(cfg)>3:n['languageNotes']=cfg[3]
  nodes.append(n)
  exports={e['name'] for e in r.get('exports',[])};cnames={c['name'] for c in r.get('classes',[])}
  for c in r.get('classes',[]):
   if len(c.get('methods',[]))<2 and c['endLine']-c['startLine']+1<20 and c['name'] not in exports:continue
   cid=f'class:{p}:{c["name"]}';nodes.append({'id':cid,'type':'class','name':c['name'],'filePath':p,'lineRange':[c['startLine'],c['endLine']],'summary':overrides.get(base+':'+c['name'],summary),'tags':tags,'complexity':complexity(c['endLine']-c['startLine']+1)})
   classes[c['name']]=cid;edge(nid,cid,'contains')
   if c['name'] in exports:edge(nid,cid,'exports')
  selected={}
  for f in r.get('functions',[]):
   name=f['name'];length=f['endLine']-f['startLine']+1
   if length<10 and (name not in exports or name in cnames):continue
   if name not in selected or length>selected[name]['endLine']-selected[name]['startLine']+1:selected[name]=f
  for name,f in selected.items():
   fid=f'function:{p}:{name}';funcs[(p,name)]=fid;symbol_owners.setdefault(name,[]).append(p)
   summ=overrides.get(base+':'+name,overrides.get(name,summary_func(name,context)))
   if name in cnames:summ='初始化'+name+'的依赖与运行配置，用于'+context+'。'
   nodes.append({'id':fid,'type':'function','name':name,'filePath':p,'lineRange':[f['startLine'],f['endLine']],'summary':summ,'tags':tags[:3],'complexity':complexity(f['endLine']-f['startLine']+1)})
   edge(nid,fid,'contains')
   if name in exports and name not in cnames:edge(nid,fid,'exports')
  if category=='code':
   for imp in task['batchImportData'].get(p,[]):edges.append({'source':nid,'target':'file:'+imp,'type':'imports','direction':'forward','weight':0.7})
  # Specialized non-code structures are emitted only if allowed by the graph schema.
  for arr,prefix in [('services','service'),('endpoints','endpoint'),('resources','resource'),('definitions','schema')]:
   for obj in r.get(arr,[]):
    name=obj.get('name') or obj.get('path')
    if not name or (arr=='definitions' and fi['path'].endswith('.env')):continue
    sid=f'{prefix}:{p}:{name}'
    if sid==nid:continue
    nodes.append({'id':sid,'type':prefix,'name':name,'filePath':p,'summary':f'{summary}定义{obj.get("kind","")} {name}。','tags':tags,'complexity':'simple'});edge(nid,sid,'contains')
 for r in x['results']:
  p=r['path'];neighbors=task.get('neighborMap',{}).get(p,[])
  for call in r.get('callGraph',[]):
   src=funcs.get((p,call['caller']));callee=call['callee']
   if not src:continue
   if '.' in callee:
    owner,name=callee.rsplit('.',1);owner=owner.removeprefix('new ')
    dst=next((funcs[(f,name)] for f in paths if pathlib.Path(f).stem==owner and (f,name) in funcs),None)
    if dst is None:
     for n in neighbors:
      if pathlib.Path(n['path']).stem==owner and name in n.get('symbols',[]):dst=f'function:{n["path"]}:{name}';break
    if dst and not dst.startswith('function:'+p+':'):edge(src,dst,'calls')
  for c in r.get('classes',[]):
   cid=classes.get(c['name'])
   if not cid:continue
   # Source inspection only resolves inheritance patterns absent from structural output.
   decl=' '.join((ROOT/p).read_text().splitlines()[max(c['startLine']-1,0):c['startLine']+4])
   for keyword,kind in [('extends','inherits'),('implements','implements')]:
    match=re.search(r'\b'+keyword+r'\s+([A-Za-z_][A-Za-z_0-9]*)',decl)
    if not match:continue
    name=match.group(1);dst=classes.get(name)
    if dst is None:
     for n in neighbors:
      if pathlib.Path(n['path']).stem==name and name in n.get('symbols',[]):dst=f'class:{n["path"]}:{name}';break
    if dst:edge(cid,dst,kind)
 for a,b,k in extra_edges:edge(a,b,k)
 expected=sum(len(task['batchImportData'].get(f['path'],[])) for f in task.get('files',task.get('batchFiles',[])) if f['fileCategory']=='code')
 assert sum(e['type']=='imports' for e in edges)==expected
 ids={n['id'] for n in nodes};assert len(ids)==len(nodes)
 allowedfiles=set(paths)
 external_symbols=set()
 for p,imports in task['batchImportData'].items():allowedfiles.update(imports)
 for ns in task.get('neighborMap',{}).values():
  for neighbor in ns:
   allowedfiles.add(neighbor['path'])
   for sym in neighbor.get('symbols',[]):
    external_symbols.add('function:'+neighbor['path']+':'+sym);external_symbols.add('class:'+neighbor['path']+':'+sym)
 for e in edges:
  for k in ['source','target']:
   target=e[k]
   if target not in ids and target not in external_symbols and not (target.startswith('file:') and target[5:] in allowedfiles):raise RuntimeError('非法边：'+str(e))
 n_parts=math.ceil(max(len(nodes)/60,len(edges)/120))
 groups=[sorted(paths)] if n_parts<=1 else [sorted(paths)[i:i+math.ceil(len(paths)/n_parts)] for i in range(0,len(paths),math.ceil(len(paths)/n_parts))]
 outs=[]
 for i,group in enumerate(groups,1):
  ns=[n for n in nodes if n['filePath'] in group];own={n['id'] for n in ns};es=[e for e in edges if e['source'] in own]
  for e in es:
   if e['target'] not in ids and e['target'] not in external_symbols and not(e['target'].startswith('file:') and e['target'][5:] in allowedfiles):raise RuntimeError(f'分片{i}验证失败：{e}')
  out=UA/'intermediate'/f'batch-{index}{"-part-"+str(i) if len(groups)>1 else ""}.json';out.parent.mkdir(exist_ok=True)
  out.write_text(json.dumps({'nodes':ns,'edges':es},ensure_ascii=False,indent=2)+'\n');json.loads(out.read_text());outs.append(str(out))
 print(json.dumps({'batchIndex':index,'files':len(paths),'nodes':len(nodes),'edges':len(edges),'imports':expected,'parts':len(outs),'outputFiles':outs,'skipped':x.get('filesSkipped',[]),'warnings':[]},ensure_ascii=False))
 return nodes,edges
