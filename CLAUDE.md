## 公共规范

### 分支开发模式

- 采用主干分支开发模式，所有开发、提交和推送均直接在 `master` 分支完成
- 禁止创建、拉取或切换到功能分支、修复分支等临时开发分支
- 开始开发前应确认本地处于 `master`，并仅从 `origin/master` 同步最新代码
- 完成并验证变更后直接提交到 `master`，推送至 `origin/master`
- 仅当用户明确要求使用独立分支时，才允许例外

### 异常处理

- 禁止吞掉异常（空异常捕获块）；非预期异常必须记录完整堆栈
- 兜底异常处理仅在全局异常处理器中，业务代码只捕获可预期的具体异常类型

### 配置管理

- 敏感配置禁止明文硬编码，必须通过环境变量注入

### 日志规范

- 禁止使用控制台输出调试，使用结构化日志
- 日志中禁止输出敏感信息（密码、Token、密钥等）

### 测试规范

- 命名：`test_{预期行为}_when_{条件}`
- 结构：AAA 模式（Arrange-Act-Assert）
- 每个测试只验证一个行为，禁止在一个测试中断言多个不相关行为

### 代码质量

- 识别并规避代码坏味道：重复代码、过长方法、过大类、过长参数列表、发散式变化、霰弹式修改
- 优先使用枚举而非字符串常量或魔法数字
- 最小化可变性，优先使用不可变数据结构
- 使用类型注解提高代码可读性

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **api2api** (8560 symbols, 23996 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "master"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/api2api/context` | Codebase overview, check index freshness |
| `gitnexus://repo/api2api/clusters` | All functional areas |
| `gitnexus://repo/api2api/processes` | All execution flows |
| `gitnexus://repo/api2api/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
