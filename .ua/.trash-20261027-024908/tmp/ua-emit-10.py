import runpy
s=runpy.run_path('/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-finalize-10.py');globals().update({k:v for k,v in s.items() if not k.startswith('__')})
extras={
7:{'addsClaudeVersionAndStreamingAcceptHeader':'验证 Messages 上游请求自动设置版本头与流式 Accept。'},
9:{'toolHistory':'构造指定数量的工具调用及成功结果历史。','repeatedToolHistory':'构造重复成功工具调用的对话历史以检查重复保护。'},
15:{'activationCases':'提供不同目标模型与会话配置的思考能力激活测试组合。','responsesRequest':'构造包含指定会话 effort 更新的 Responses 请求样本。','convert':'执行指定协议和模型的请求转换并解析目标 JSON。'},
24:{'allConverterDirections':'枚举全部已登记源协议与目标协议组合以驱动参数化测试。','stubConverter':'创建指定协议方向与字段映射的程序注册表测试替身。'},
26:{'shouldDetectAdvancedToolAndReasoningRequirementsFromConversationHistory':'验证从历史工具调用与思考块推导高级工具和推理能力要求。','shouldTreatOutputEffortAsReasoningCapabilityRequirement':'验证 output effort 被识别为路由所需的推理能力。'},
27:{'failedClaudeInvocation':'构造包含路由失败历史和上游响应信息的 Messages 调用失败样本。'},
30:{'setUp':'配置桥接依赖、模拟网关、固定请求上下文及计量采集环境。','rejectBridgedPlanner':'配置规划器以 HTTP 错误拒绝桥接函数，验证回退路径。','streamResponses':'构造可读取的 Responses 规划 SSE 样本。','streamImages':'构造图像生成进度与完成 SSE 样本。'}}
for i,m in extras.items():function_info.setdefault(i,{}).update(m)
nodes=[];edges=[];ids=set()
def edge(src,target,kind,w):
 if src!=target and not any(e['source']==src and e['target']==target and e['type']==kind for e in edges):edges.append(dict(source=src,target=target,type=kind,direction='forward',weight=w))
def add(n):
 assert n['id'] not in ids,n['id'];ids.add(n['id']);nodes.append(n)
for i,f in enumerate(d['results']):
 p=f['path'];fid='file:'+p;summary,tags=file_info[i];complexity='complex' if f['nonEmptyLines']>200 else 'moderate' if f['nonEmptyLines']>=50 else 'simple'
 add(dict(id=fid,type='file',name=pathlib.Path(p).name,filePath=p,summary=summary,tags=tags,complexity=complexity));exports={z['name'] for z in f.get('exports',[])}
 for c in f.get('classes',[]):
  if c['endLine']-c['startLine']+1<20 and len(c['methods'])<2 and c['name'] not in exports:continue
  cid='class:'+p+':'+c['name'];cs=summary
  if 'Stub' in c['name']:cs='提供可配置响应并记录网关调用与用量的测试替身。'
  add(dict(id=cid,type='class',name=c['name'],filePath=p,lineRange=[c['startLine'],c['endLine']],summary=cs,tags=tags,complexity=complexity));edge(fid,cid,'contains',1.)
  if c['name'] in exports:edge(fid,cid,'exports',.8)
 for z in f.get('functions',[]):
  ln=z['endLine']-z['startLine']+1
  if ln<10 and z['name'] not in exports:continue
  nid='function:'+p+':'+z['name']
  if nid in ids:continue
  text=function_info.get(i,{}).get(z['name']);assert text,(i,p,z['name'])
  is_test=z['name'].startswith(('test_','should','addsClaude'))
  ft=['测试','行为验证','回归测试'] if is_test else ['测试','样本构建','辅助方法'] if '/test/' in p else tags
  add(dict(id=nid,type='function',name=z['name'],filePath=p,lineRange=[z['startLine'],z['endLine']],summary=text,tags=ft,complexity='complex' if ln>200 else 'moderate' if ln>=40 else 'simple'));edge(fid,nid,'contains',1.)
  if z['name'] in exports:edge(fid,nid,'exports',.8)
 for t in b['batchImportData'][p]:edges.append(dict(source=fid,target='file:'+t,type='imports',direction='forward',weight=.7))
# 明确的同批解析调用。
request=d['results'][4]['path'];bridge=d['results'][3]['path']
for src,dst in [('tryHandle','parse'),('execute','prompt'),('execute','inputImages'),('plan','plannerRequest')]:
 a='function:'+bridge+':'+src;t='function:'+request+':'+dst
 if a in ids and t in ids:edge(a,t,'calls',.8)
# 对已解析导入中的实际生产类建立测试关系。同包类显式验证磁盘存在。
extra_production=[]
for i,f in enumerate(d['results'][5:],5):
 p=f['path'];exact=p.replace('/src/test/','/src/main/').replace('Test.java','.java');targets=[]
 if (ROOT/exact).is_file():targets.append(exact)
 base=pathlib.Path(p).stem.removesuffix('Test')
 for t in b['batchImportData'][p]:
  stem=pathlib.Path(t).stem
  if stem==base or (base.startswith(stem) and len(stem)>12):targets.append(t)
 # 专项职责测试明确调用的生产目标。
 for needle in {22:['ResponsesRequestConverter.java'],23:['ResponsesUsageExtractor.java'],24:['ProtocolConverterRegistry.java'],25:['StreamingPassthroughAdapter.java'],26:['ClaudeMessagesGatewayRequest.java'],27:['GatewayInvocationResponseMapper.java'],28:['GatewayProtocolErrorBodyBuilder.java'],29:['GatewayStreamingResponseMapper.java'],30:['ResponsesImageBridge.java']}.get(i,[]):
  targets.extend(t for t in b['batchImportData'][p] if t.endswith(needle))
 for t in set(targets):
  production='file:'+t;test='file:'+p;edge(production if production in ids else test,test if production in ids else production,'tested_by',.5);extra_production.append(t)
assert sum(e['type']=='imports' for e in edges)==sum(len(b['batchImportData'][f['path']]) for f in b['files'])
assert len([n for n in nodes if n['type']=='file'])==len(b['files'])
known_files={f['path'] for f in b['files']}|set(extra_production)
for a in b['batchImportData'].values():known_files.update(a)
known_symbols=set()
for a in b['neighborMap'].values():
 for n in a:
  known_files.add(n['path'])
  for name in n['symbols']:known_symbols.update(['function:'+n['path']+':'+name,'class:'+n['path']+':'+name])
for e in edges:
 for k in ['source','target']:
  ref=e[k];assert ref in ids or ref in known_symbols or (ref.startswith('file:') and ref[5:] in known_files),e
paths=sorted(f['path'] for f in b['files']);parts=math.ceil(max(len(nodes)/60,len(edges)/120));size=math.ceil(len(paths)/parts);written=[]
for k,start in enumerate(range(0,len(paths),size),1):
 group=set(paths[start:start+size]);ns=[n for n in nodes if n['filePath'] in group];local={n['id'] for n in ns};es=[e for e in edges if e['source'] in local]
 p=UA/'intermediate'/(f'batch-10-part-{k}.json' if parts>1 else 'batch-10.json');p.write_text(json.dumps(dict(nodes=ns,edges=es),ensure_ascii=False,indent=2)+'\n');json.loads(p.read_text());written.append(str(p))
assert sum(len(json.loads(pathlib.Path(p).read_text())['edges']) for p in written)==len(edges)
print(json.dumps(dict(batch=10,files=len(paths),nodes=len(nodes),edges=len(edges),imports=sum(e['type']=='imports' for e in edges),parts=len(written),paths=written,skipped=d['filesSkipped']),ensure_ascii=False))
