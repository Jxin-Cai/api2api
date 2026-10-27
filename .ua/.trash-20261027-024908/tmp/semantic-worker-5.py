import json,math,pathlib,re,sys
ROOT=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api')
UA=ROOT/('.understand-anything' if (ROOT/'.understand-anything').is_dir() else '.ua')
# 此脚本只对已完成的官方结构提取结果进行语义装配和校验，不提取源码结构。
def build(index, descriptions, overrides=None):
 overrides=dict(overrides or {})
 extra=UA/f'tmp/semantic-refinements-{index}.json'
 if extra.exists():overrides.update(json.loads(extra.read_text()))
 task=json.loads((UA/f'tmp/batch-task-{index}.json').read_text())
 data=json.loads((UA/f'tmp/ua-file-extract-results-{index}.json').read_text())
 assert data['scriptCompleted'] and not data.get('filesUnreadable')
 nodes=[];edges=[];seen=set();funcs={};exported={}; file_ids={}
 def edge(s,t,k,w):
  if s!=t and (s,t,k) not in seen:
   seen.add((s,t,k));edges.append(dict(source=s,target=t,type=k,direction='forward',weight=w))
 def complexity(n): return 'simple' if n<50 else 'moderate' if n<=200 else 'complex'
 def node(id,kind,name,path,summary,tags,n,lines=None):
  d=dict(id=id,type=kind,name=name,filePath=path,summary=summary,tags=tags,complexity=complexity(n))
  if lines: d['lineRange']=lines
  nodes.append(d)
 for f in data['results']:
  p=f['path']; base=pathlib.Path(p).name; stem=pathlib.Path(p).stem; desc=descriptions.get(base)
  assert desc, f'缺少文件语义描述: {base}'
  text,tags=desc if isinstance(desc,tuple) else (desc,['领域模型','数据封装','不可变对象'])
  cat=f.get('fileCategory','code');typ={'code':'file','script':'file','markup':'file','config':'config','docs':'document','data':'schema','infra':'service'}[cat]
  fid=f'{typ}:{p}';file_ids[p]=fid; node(fid,typ,base,p,text,tags,f['nonEmptyLines'])
  ex={e['name'] for e in f.get('exports',[])};exported[p]=ex
  for c in f.get('classes',[]):
   if len(c.get('methods',[]))<2 and c['endLine']-c['startLine']+1<20 and c['name'] not in ex: continue
   cid=f"class:{p}:{c['name']}";node(cid,'class',c['name'],p,text,tags,c['endLine']-c['startLine']+1,[c['startLine'],c['endLine']]);edge(fid,cid,'contains',1.0)
   if c['name'] in ex:edge(fid,cid,'exports',0.8)
  classnames={c['name'] for c in f.get('classes',[])}
  properties={v for c in f.get('classes',[]) for v in c.get('properties',[])}
  accessors=properties|{'get'+v[:1].upper()+v[1:] for v in properties}|{'set'+v[:1].upper()+v[1:] for v in properties}
  selected={}
  for fn in f.get('functions',[]):
   n=fn['name'];length=fn['endLine']-fn['startLine']+1
   boiler=n in {'equals','hashCode','toString'} or (length<10 and n in accessors)
   if boiler:continue
   if length<10 and n not in ex:continue
   if n in classnames and length<10:continue
   if n not in selected or length>selected[n]['endLine']-selected[n]['startLine']+1:selected[n]=fn
  funcs[p]=selected
  for n,fn in selected.items():
   length=fn['endLine']-fn['startLine']+1
   summ=overrides.get(base+':'+n) or describe_method(n,stem,text,n in classnames)
   ftags=['行为验证','回归测试','测试用例'] if '/test/' in p or '.test.' in p or '.spec.' in p else ['业务方法',tags[0], '参数校验' if n.startswith(('validate','assert','ensure','require')) else '数据处理']
   funid=f'function:{p}:{n}';node(funid,'function',n,p,summ,ftags,length,[fn['startLine'],fn['endLine']]);edge(fid,funid,'contains',1.0)
   if n in ex and n not in classnames:edge(fid,funid,'exports',0.8)
  if cat=='code':
   for target in task['batchImportData'].get(p,[]):edge(fid,'file:'+target,'imports',0.7)
 # 只接受结构输出中有明确接收者或本文件函数名的调用。
 ids={n['id'] for n in nodes}
 for f in data['results']:
  p=f['path']; neighbors=task.get('neighborMap',{}).get(p,[])
  candidates={q:pathlib.Path(q).stem for q in task['batchImportData'].get(p,[])}
  for call in f.get('callGraph',[]):
   caller=call.get('caller');raw=call.get('callee','');src=f'function:{p}:{caller}'
   if src not in ids:continue
   target=None
   if '.' not in raw:
    t=f'function:{p}:{raw}'
    if t in ids:target=t
   else:
    receiver,method=raw.rsplit('.',1)
    if receiver in {'this','self'}:
     t=f'function:{p}:{method}'
     if t in ids:target=t
    for q,cls in candidates.items():
     if receiver not in {cls,cls[:1].lower()+cls[1:]}:continue
     t=f'function:{q}:{method}'
     if t in ids:target=t
     elif q not in funcs and not method.startswith(('get','set')) and method not in {'equals','hashCode','toString','of','value'} and any(x['path']==q and method in x.get('symbols',[]) for x in neighbors):target=t
   if target:edge(src,target,'calls',0.8)
 # 测试关系仅在解析器给出的内部导入确实指向生产文件时输出。
 for p in file_ids:
  if '/test/' in p or '.test.' in p or '.spec.' in p:
   for q in task['batchImportData'].get(p,[]):
    if '/test/' not in q and '.test.' not in q and '.spec.' not in q:edge('file:'+q,file_ids[p],'tested_by',0.5)
 for item in overrides.get('_edges',[]):edge(item[0],item[1],item[2],item[3])
 expected=sum(len(task['batchImportData'].get(f['path'],[])) for f in data['results'] if f.get('fileCategory')=='code')
 actual=sum(e['type']=='imports' for e in edges);assert expected==actual,(expected,actual)
 assert len(ids)==len(nodes),'节点标识重复'
 assert {x['path'] for x in task['files']}==set(file_ids),'文件覆盖不全'
 # 所有跨分片节点均属本批已知节点；其他文件/符号须由输入关系证实。
 knownfiles=set(task['batchImportData'])|set(file_ids)
 knownsymbols=set()
 for arr in task['batchImportData'].values():knownfiles.update(arr)
 for arr in task.get('neighborMap',{}).values():
  for nb in arr:
   knownfiles.add(nb['path'])
   for symbol in nb.get('symbols',[]):
    knownsymbols.update({f'function:{nb["path"]}:{symbol}',f'class:{nb["path"]}:{symbol}'})
 def valid(s):return s in ids or s in knownsymbols or (s.startswith('file:') and s[5:] in knownfiles)
 for e in edges:assert valid(e['source']) and valid(e['target']),f'无效引用 {e}'
 parts=math.ceil(max(len(nodes)/60,len(edges)/120)) if len(nodes)>60 or len(edges)>120 else 1
 paths=sorted(file_ids);size=math.ceil(len(paths)/parts); outputs=[]
 for k,start in enumerate(range(0,len(paths),size),1):
  owned=set(paths[start:start+size]);pn=[n for n in nodes if n['filePath'] in owned];pi={n['id'] for n in pn}
  pe=[e for e in edges if e['source'] in pi]
  # tested_by 的生产源可能在别批；将其放在当前测试所属分片，保留有证据的关系。
  if k==1:pe.extend(e for e in edges if e['source'] not in ids)
  out=UA/'intermediate'/f'batch-{index}{"-part-"+str(k) if parts>1 else ""}.json'
  out.parent.mkdir(exist_ok=True);out.write_text(json.dumps(dict(nodes=pn,edges=pe),ensure_ascii=False,indent=2)+'\n');json.loads(out.read_text());outputs.append(str(out))
 assert sum(len(json.loads(pathlib.Path(o).read_text())['edges']) for o in outputs)==len(edges)
 print(json.dumps(dict(batch=index,files=len(file_ids),nodes=len(nodes),edges=len(edges),imports=actual,parts=len(outputs),outputs=outputs,skipped=data.get('filesSkipped',[])),ensure_ascii=False))
 return nodes,edges

def describe_method(name,stem,context,constructor=False):
 if constructor:return f'初始化 {stem}，设置构造参数并建立对象状态。'
 special={'of':'由传入参数创建对象，封装对应的领域数据。','known':'创建用量已知的令牌明细并计算令牌合计。','unknown':'创建用量未知的令牌明细，保留未知状态。','zeroKnown':'创建已知为零的令牌明细。','plus':'合并两组令牌明细并保留用量是否已知的信息。','fromUnified':'将统一协议用量转换为业务令牌明细。','snapshot':'采样并缓存系统资源、运行时与数据库连接池指标。','parseExternal':'解析外部协议标识并转换为协议枚举。','isClientFacing':'判断协议是否支持作为客户端入口。','defaultEndpointPath':'返回协议对应的默认请求端点路径。','isSameAs':'判断给定协议是否与当前协议相同。'}
 if name in special:return f'{stem} 的 {name} 方法'+special[name]
 # 名称中的技术标识保留，中文描述操作类别并结合所属组件说明用途。
 actions=[('test','验证'),('should','验证'),('assert','校验'),('validate','校验'),('ensure','保证'),('require','检查'),('normalize','归一化'),('calculate','计算'),('build','构建'),('create','创建'),('rehydrate','恢复'),('update','更新'),('change','变更'),('find','查询'),('list','列出'),('load','加载'),('save','保存'),('delete','删除'),('remove','移除'),('cancel','取消'),('resolve','解析'),('bind','绑定'),('clear','清理'),('count','统计'),('sum','汇总'),('query','查询'),('get','读取'),('to','转换'),('from','转换'),('parse','解析'),('encode','编码'),('decode','解码'),('write','写入'),('read','读取'),('map','映射'),('convert','转换'),('handle','处理'),('execute','执行'),('invoke','调用'),('stream','流式处理'),('complete','完成'),('enable','启用'),('disable','禁用'),('reset','重置'),('refresh','刷新'),('filter','筛选'),('sort','排序'),('append','追加'),('merge','合并'),('select','选择'),('supports','判断支持情况'),('success','封装成功结果'),('error','封装错误结果')]
 action=next((v for k,v in actions if name.lower().startswith(k.lower())),'处理')
 return f'在 {stem} 中{action} {name} 对应的数据或状态，支撑该组件的业务职责。'
