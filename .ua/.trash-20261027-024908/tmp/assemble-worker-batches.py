import json, pathlib, math, sys, re
ROOT=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api')
UA=ROOT/('.understand-anything' if (ROOT/'.understand-anything').is_dir() else '.ua')

def assemble(idx):
 d=json.loads((UA/f'tmp/batch-task-{idx}.json').read_text()); ext=json.loads((UA/f'tmp/ua-file-extract-results-{idx}.json').read_text()); sem=json.loads((UA/f'tmp/semantic-{idx}.json').read_text())
 assert ext['scriptCompleted'] and not ext.get('filesUnreadable'),ext.get('filesUnreadable')
 nodes=[];edges=[];warnings=[];ids=set();byfile={}; funcs={}
 def edge(s,t,k,w):
  if s!=t and not any(x['source']==s and x['target']==t and x['type']==k for x in edges):edges.append(dict(source=s,target=t,type=k,direction='forward',weight=w))
 def add(n):
  assert n['id'] not in ids,n['id'];ids.add(n['id']);nodes.append(n)
 def complexity(lines):return 'simple' if lines<50 else 'moderate' if lines<=200 else 'complex'
 for r in ext['results']:
  p=r['path'];base=pathlib.Path(p).name;s=sem.get(p,sem.get(base));assert s,('missing semantics',p)
  if isinstance(s,str):
   tags=['领域模型','类型定义','数据校验']
   if '/command/' in p:tags=['命令对象','应用输入','不可变']
   elif '/dto/' in p:tags=['数据传输','类型定义','协议适配']
   elif '/repository/' in p:tags=['仓储接口','持久化','依赖倒置']
   elif '/test/' in p or '/tests/' in p or '.test.' in p:tags=['测试','行为验证','回归保障']
   elif '/infr/' in p:tags=['基础设施','适配器','后端服务']
   s={'summary':s,'tags':tags}
  cat=r['fileCategory'];kind={'code':'file','script':'file','markup':'file','config':'config','docs':'document','data':'schema','infra':'service'}[cat]
  if cat=='infra' and ('workflows/' in p or '.gitlab-ci' in p):kind='pipeline'
  if cat=='data' and p.endswith('.sql'):kind='table'
  fid=f'{kind}:{p}';byfile[p]=fid
  add(dict(id=fid,type=kind,name=base,filePath=p,summary=s['summary'],tags=s['tags'],complexity=complexity(r['nonEmptyLines'])))
  exp={e['name'] for e in r.get('exports',[])}
  for c in r.get('classes',[]):
   if not(len(c.get('methods',[]))>=2 or c['endLine']-c['startLine']+1>=20 or c['name'] in exp):continue
   cid=f"class:{p}:{c['name']}";add(dict(id=cid,type='class',name=c['name'],filePath=p,lineRange=[c['startLine'],c['endLine']],summary=s.get('classSummary',s['summary']),tags=s['tags'],complexity=complexity(c['endLine']-c['startLine']+1)))
   edge(fid,cid,'contains',1.0)
   if c['name'] in exp:edge(fid,cid,'exports',0.8)
  grouped={}
  for f in r.get('functions',[]):
   if f['endLine']-f['startLine']+1>=10 or f['name'] in exp:grouped.setdefault(f['name'],[]).append(f)
  funcs[p]={}
  for name,fs in grouped.items():
   f=fs[0];nid=f'function:{p}:{name}';funcs[p][name]=nid
   summary=s.get('functions',{}).get(name)
   if not summary:
    # 从实际代码块中的文档、语句及确定性调用图补足方法职责；结构完全来自 bundled extractor。
    lines=(ROOT/p).read_text().splitlines();body='\n'.join(lines[f['startLine']-1:f['endLine']]);params=f.get('params',[])
    prop=name[3:4].lower()+name[4:] if name.startswith('get') and len(name)>3 else name
    access=re.search(r'return\s+(?:this\.)?([\w.]+)\s*;',body)
    if name in ('equals','hashCode','toString','compareTo'):
     summary={'equals':'按内部值判断对象相等性。','hashCode':'根据内部值计算哈希值。','toString':'返回对象的字符串表示。','compareTo':'比较两个优先级值的顺序。'}[name]
    elif access and len(body.splitlines())<=7:summary=f'返回 {access.group(1)} 的当前值，供调用方读取。'
    elif name=='of':summary=f'通过静态工厂构造 {base.rsplit(".",1)[0]}，封装传入参数。'
    elif name==base.rsplit('.',1)[0]:summary=f'初始化 {name} 的状态并建立构造约束。'
    elif name=='copyNotEmpty':summary='校验集合非空并防御性复制，避免命令对象被外部修改。'
    elif name=='rehydrate':summary=f'根据持久化状态重建 {base.rsplit(".",1)[0]}。'
    elif name=='create':summary=f'创建 {base.rsplit(".",1)[0]} 并初始化业务状态。'
    elif name.startswith('test') or '/test/' in p:summary=f'验证 {name} 描述的行为和边界条件，使用测试夹具检查结果。'
    else:
     calls=[]
     for c in r.get('callGraph',[]):
      if c.get('caller')==name and c.get('callee') not in calls and c.get('callee')!=name:calls.append(c['callee'])
     summary=f'在{base.rsplit(".",1)[0]}中执行 {name} 操作。'+(('协调 '+ '、'.join(calls[:5])+' 等调用处理输入与状态。') if calls else ('处理 '+ '、'.join(params)+' 参数。' if params else '封装相应业务状态的计算或更新。'))
   if len(fs)>1:summary+=' 同名重载接收不同参数形式。';warnings.append(f'{base}:{name} 的 {len(fs)} 个重载合并为单一规范 ID，行范围取首个定义')
   add(dict(id=nid,type='function',name=name,filePath=p,lineRange=[f['startLine'],f['endLine']],summary=summary,tags=s.get('functionTags',s['tags']),complexity=complexity(f['endLine']-f['startLine']+1)))
   edge(fid,nid,'contains',1.0)
   if name in exp:edge(fid,nid,'exports',0.8)
  for target in d['batchImportData'].get(p,[]):
   if cat=='code':
    edge(fid,f'file:{target}','imports',0.7)
    if '/test/' in p and '/test/' not in target:edge(fid,f'file:{target}','tested_by',0.5)
  for field,typ in [('services','service'),('definitions','schema'),('endpoints','endpoint'),('resources','resource')]:
   for item in r.get(field,[]):
    name=item.get('name')
    if not name:continue
    nid=f'{typ}:{p}:{name}';add(dict(id=nid,type=typ,name=name,filePath=p,summary=f'{s["summary"]} 其中 {name} 定义{item.get("kind","")}结构。',tags=s['tags'],complexity='simple'));edge(fid,nid,'contains',1.0)
 for item in sem.get('_edges',[]):edge(item['source'],item['target'],item['type'],item['weight'])
 # 仅建立确定性同文件调用；跨文件调用必须由人工提供。
 for r in ext['results']:
  p=r['path']
  for c in r.get('callGraph',[]):
   a=funcs.get(p,{}).get(c.get('caller'));b=funcs.get(p,{}).get(c.get('callee'))
   if a and b:edge(a,b,'calls',0.8)
 expected=sum(len(d['batchImportData'].get(f['path'],[])) for f in d['files'] if f['fileCategory']=='code')
 assert sum(e['type']=='imports' for e in edges)==expected
 assert set(byfile)=={f['path'] for f in d['files']},'file coverage mismatch'
 allids={n['id'] for n in nodes};external={f'file:{p}' for ps in d['batchImportData'].values() for p in ps}
 for ns in d.get('neighborMap',{}).values():
  for n in ns:
   external.add('file:'+n['path'])
   for sym in n.get('symbols',[]):
    external.add(f'function:{n["path"]}:{sym}');external.add(f'class:{n["path"]}:{sym}')
 for e in edges:assert e['source'] in allids|external and e['target'] in allids|external,e
 parts=math.ceil(max(len(nodes)/60,len(edges)/120));files=sorted(byfile);chunk=math.ceil(len(files)/parts);groups=[files[i:i+chunk] for i in range(0,len(files),chunk)]
 if len(nodes)<=60 and len(edges)<=120:groups=[files]
 out=[]
 for i,ps in enumerate(groups,1):
  ns=[n for n in nodes if n['filePath'] in ps];nids={n['id'] for n in ns};es=[e for e in edges if e['source'] in nids or (e['source'] not in allids and e['target'] in nids)]
  for e in es:assert e['target'] in allids|external
  path=UA/'intermediate'/ (f'batch-{idx}.json' if len(groups)==1 else f'batch-{idx}-part-{i}.json')
  path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(dict(nodes=ns,edges=es),ensure_ascii=False,indent=2));assert json.loads(path.read_text())['nodes'];out.append(str(path))
 print(json.dumps({'batch':idx,'files':len(byfile),'nodes':len(nodes),'edges':len(edges),'imports':expected,'parts':out,'skipped':ext.get('filesSkipped',[]),'warnings':warnings},ensure_ascii=False))
if __name__=='__main__':assemble(int(sys.argv[1]))
