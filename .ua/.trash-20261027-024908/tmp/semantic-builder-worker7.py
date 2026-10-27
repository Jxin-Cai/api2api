import json,pathlib,re,math,sys
ROOT=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api')
UA=ROOT/('.understand-anything' if (ROOT/'.understand-anything').is_dir() else '.ua')
KNOWN_IDS=set()
WEIGHTS={'contains':1.0,'imports':.7,'calls':.8,'inherits':.9,'implements':.9,'exports':.8,'depends_on':.6,'tested_by':.5,'configures':.6,'documents':.5,'deploys':.7,'migrates':.7,'triggers':.6,'defines_schema':.8,'serves':.7,'provisions':.7,'routes':.6,'related':.5}
def load(i):
 return json.loads((UA/f'tmp/batch-task-{i}.json').read_text()),json.loads((UA/f'tmp/ua-file-extract-results-{i}.json').read_text())
def write(i,nodes,edges):
 d,x=load(i);paths=sorted(f['path'] for f in d['files']);ids={n['id'] for n in nodes};assert len(ids)==len(nodes)
 assert {n.get('filePath') for n in nodes} >= set(paths)
 assert all(n['summary'] and 3<=len(n['tags'])<=5 and n['type'] in {'file','function','class','config','document','service','table','endpoint','pipeline','schema','resource'} for n in nodes)
 expected=sum(len(d['batchImportData'].get(f['path'],[])) for f in d['files'] if f['fileCategory']=='code')
 assert sum(e['type']=='imports' for e in edges)==expected,(i,expected)
 knownfiles=set(paths);symbols=set()
 for p,vs in d.get('batchImportData',{}).items():knownfiles.update(vs)
 for p,vs in d.get('neighborMap',{}).items():
  knownfiles.add(p)
  for v in vs:
   knownfiles.add(v['path'])
   for s in v.get('symbols',[]):
    for kind in ['function','class']:symbols.add(f"{kind}:{v['path']}:{s}")
 def known(s):return s in ids or s in KNOWN_IDS or s in symbols or s.startswith('file:') and s[5:] in knownfiles
 for e in edges:assert e['source']!=e['target'] and known(e['source']) and known(e['target']),(i,e)
 parts=math.ceil(max(len(nodes)/60,len(edges)/120,1));sz=math.ceil(len(paths)/parts)
 groups=[paths[j:j+sz] for j in range(0,len(paths),sz)]
 out=UA/'intermediate';out.mkdir(exist_ok=True)
 existing=list(out.glob(f'batch-{i}.json'))+list(out.glob(f'batch-{i}-part-*.json'))
 for p in existing:p.unlink()
 written=[]
 for k,group in enumerate(groups,1):
  nn=[n for n in nodes if n.get('filePath') in group];ii={n['id'] for n in nn};ee=[e for e in edges if e['source'] in ii]
  p=out/(f'batch-{i}.json' if len(groups)==1 else f'batch-{i}-part-{k}.json');p.write_text(json.dumps({'nodes':nn,'edges':ee},ensure_ascii=False,indent=2)+'\n');json.loads(p.read_text());written.append(str(p))
 print(json.dumps({'batch':i,'files':len(paths),'nodes':len(nodes),'edges':len(edges),'parts':len(written),'imports':expected,'skipped':x.get('filesSkipped',[]),'unreadable':x.get('filesUnreadable',[]),'outputs':written},ensure_ascii=False))
 return written

def code(i,specs,descriptions=None,extras=None):
 d,x=load(i);nodes=[];edges=[];seen=set();descriptions=descriptions or {};sources={}
 for skipped in x.get('filesSkipped',[]):
  bf=next(z for z in d['files'] if z['path']==skipped)
  assert bf['fileCategory']!='code' or bf['language']=='conf',('Unsupported code requires explicit supplement',skipped)
  lines=(ROOT/skipped).read_text().splitlines()
  x['results'].append(dict(bf,nonEmptyLines=sum(bool(z.strip()) for z in lines),functions=[],classes=[],exports=[]))
 def edge(a,b,t):
  if a==b or (a,b,t) in seen:return
  seen.add((a,b,t));edges.append({'source':a,'target':b,'type':t,'direction':'forward','weight':WEIGHTS[t]})
 def complexity(n):return 'complex' if n>200 else 'moderate' if n>=50 else 'simple'
 for f in x['results']:
  p=f['path'];fn=pathlib.Path(p).name;spec=specs.get(p,specs.get(fn));assert spec,fn
  summary,tags=spec[:2];ntype={'config':'config','docs':'document'}.get(f['fileCategory'],'file');fid=ntype+':'+p;lines=(ROOT/p).read_text().splitlines();sources[p]=lines
  nodes.append({'id':fid,'type':ntype,'name':fn,'filePath':p,'summary':summary,'tags':tags,'complexity':complexity(f['nonEmptyLines'])})
  exports={e['name'] for e in f.get('exports',[])}
  for kind,array in [('class',f.get('classes',[])),('function',f.get('functions',[]))]:
   byname={}
   for z in array:
    name=z['name'];length=z['endLine']-z['startLine']+1
    significant=(name in exports or (length>=10 if kind=='function' else length>=20 or len(z.get('methods',[]))>=2))
    if not significant:continue
    if name not in byname or length>byname[name]['endLine']-byname[name]['startLine']+1:byname[name]=z
   for name,z in byname.items():
    sid=f'{kind}:{p}:{name}';length=z['endLine']-z['startLine']+1
    desc=descriptions.get(fn+':'+name,descriptions.get(pathlib.Path(fn).stem+':'+name,descriptions.get(name)))
    if kind=='class':desc=desc or summary
    elif not desc:
     if name=='equals':desc='比较'+fn[:-5]+'的领域属性，判断两个实例是否具有相同值。'
     elif name=='hashCode':desc='根据'+fn[:-5]+'的领域属性计算与值相等规则一致的哈希值。'
     elif name=='toString':desc='返回'+fn[:-5]+'的文本表示。'
     elif name in [c['name'] for c in f.get('classes',[])]:desc='初始化'+name+'的依赖和状态，并按构造规则保存传入参数。'
     elif name.startswith('get'):desc='读取'+fn[:-5]+'配置中的'+name[3:]+'属性。'
     elif name.startswith('set'):desc='设置'+fn[:-5]+'配置中的'+name[3:]+'属性。'
     elif name in ['of','create','rehydrate','start','failed','succeeded','success','failure','retryNext','stop']:desc='依据传入参数构造'+fn[:-5]+'，封装对应的领域状态。'
     elif length<10:desc='返回或判断'+fn[:-5]+'的'+name+'属性，供调用方访问封装状态。'
     else:raise ValueError('Missing semantic description '+fn+':'+name)
    nodes.append({'id':sid,'type':kind,'name':name,'filePath':p,'lineRange':[z['startLine'],z['endLine']],'summary':desc,'tags':list(dict.fromkeys(tags[:2]+(['领域对象'] if kind=='class' else ['方法'])))[:5],'complexity':complexity(length)})
    edge(fid,sid,'contains')
    if name in exports:edge(fid,sid,'exports')
  for target in d['batchImportData'].get(p,[]):edge(fid,'file:'+target,'imports')
 ids={n['id'] for n in nodes}
 for f in x['results']:
  p=f['path'];lines=sources[p];classlookup={c['name']:f"class:{p}:{c['name']}" for c in f.get('classes',[])}
  neighbor=d.get('neighborMap',{}).get(p,[])
  for n in neighbor:
   name=pathlib.Path(n['path']).stem
   if name in n.get('symbols',[]):classlookup.setdefault(name,f"class:{n['path']}:{name}")
  for n in nodes:
   if n['type']=='class':classlookup.setdefault(n['name'],n['id'])
  text='\n'.join(lines)
  for c in f.get('classes',[]):
   cid=f"class:{p}:{c['name']}"
   if cid not in ids:continue
   m=re.search(r'\b(?:class|interface|enum|record)\s+'+re.escape(c['name'])+r'\b[^\{]*',text)
   if not m:continue
   decl=m.group()
   for rel,typ in [('extends','inherits'),('implements','implements')]:
    mm=re.search(r'\b'+rel+r'\s+([^\{]+?)(?=\bimplements\b|$)',decl)
    if mm:
     for s in re.findall(r'\b[A-Z]\w*',mm.group(1)):
      if s in classlookup:edge(cid,classlookup[s],typ)
  for call in f.get('callGraph',[]):
   caller=f"function:{p}:{call['caller']}"
   if caller not in ids:continue
   callee=call['callee'];local=f'function:{p}:{callee}'
   if local in ids:edge(caller,local,'calls');continue
   mm=re.fullmatch(r'([A-Z]\w*)\.([a-zA-Z_]\w*)',callee)
   if mm:
    owner,name=mm.groups()
    local=f'function:{p}:{name}'
    if owner in classlookup:
     cp=classlookup[owner][6:].rsplit(':',1)[0];target=f'function:{cp}:{name}'
     if target in ids or any(nn['path']==cp and name in nn.get('symbols',[]) for nn in neighbor):edge(caller,target,'calls')
 if extras:
  for e in extras:edge(*e)
 return write(i,nodes,edges)
