import json,pathlib,sys
R=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api');i=int(sys.argv[1]);d=json.loads((R/f'.ua/tmp/batch-task-{i}.json').read_text());print('DISPATCH',d['batchIndex'],len(d['files']),list(d));e=json.loads((R/f'.ua/tmp/ua-file-extract-results-{i}.json').read_text());print('EXTRACT',e['scriptCompleted'],e['filesAnalyzed'],e['filesSkipped'],e.get('filesUnreadable'))
for r in e['results']:
 print('\n###',r['path'],r['nonEmptyLines']);print('classes',[(c['name'],c.get('properties',[])) for c in r.get('classes',[])]);lines=(R/r['path']).read_text().splitlines();comment=[l.strip() for l in lines if l.strip().startswith(('*','//'))];print('DOC', ' '.join(comment)[:500]);fs=r.get('functions',[])
 if len(lines)<38:print('\n'.join(l for l in lines if not l.startswith('import ')))
 for f in fs:
  if f['endLine']-f['startLine']>=8:
   block=lines[f['startLine']-1:f['endLine']];print(f['name'],[f['startLine'],f['endLine']],' | '.join(block[:2]+[x.strip() for x in block if any(z in x for z in ['return ','throw ','Repository.','repository.','Port.','Client.','client.'])])[:750])
