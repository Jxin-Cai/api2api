import subprocess
from pathlib import Path

root = Path('/Users/jxin/Desktop/Project/products/2api/api2api')
inter = root / '.ua/intermediate'
script = '/Users/jxin/.claude/plugins/cache/understand-anything/understand-anything/2.9.8/skills/understand/merge-batch-graphs.py'
result = subprocess.run(['python3', script, str(root)], text=True, capture_output=True)
(inter / 'merge-stdout.log').write_text(result.stdout)
(inter / 'merge-stderr.log').write_text(result.stderr)
print(result.stdout)
if result.returncode:
    print(result.stderr)
    raise SystemExit(result.returncode)
lines = result.stderr.splitlines()
print('完整合并诊断已保存：', inter / 'merge-stderr.log')
print('诊断行数：', len(lines))
for line in lines[-15:]:
    print(line)
