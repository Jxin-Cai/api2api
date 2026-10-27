import json,pathlib,subprocess,sys
r=pathlib.Path('/Users/jxin/Desktop/Project/products/2api/api2api');u=r/('.understand-anything' if (r/'.understand-anything').is_dir() else '.ua');n=int(sys.argv[1]);d=json.loads((u/f'tmp/batch-task-{n}.json').read_text());i=u/f'tmp/ua-file-analyzer-input-{n}.json';o=u/f'tmp/ua-file-extract-results-{n}.json'
i.write_text(json.dumps({'projectRoot':str(r),'batchFiles':d['files'],'batchImportData':d['batchImportData']}));subprocess.run(['node','/Users/jxin/.claude/plugins/cache/understand-anything/understand-anything/2.9.8/skills/understand/extract-structure.mjs',str(i),str(o)],check=True);assert o.is_file() and o.stat().st_size
x=json.loads(o.read_text());assert not x.get('filesUnreadable');print('完成',x['filesAnalyzed'],'跳过',x.get('filesSkipped',[]),'不可读',x.get('filesUnreadable',[]))
for f in x['results']:
 print(json.dumps({k:v for k,v in f.items() if k not in ['callGraph','sections']},ensure_ascii=False))
 print('章节',f.get('sections',[]))
