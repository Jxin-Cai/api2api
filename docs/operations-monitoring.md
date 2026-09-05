# 运维监控

管理后台新增「运维监控」入口：`/admin/operations`。该页面仅允许启用的管理员访问；两个后端接口独立检查会话和管理员权限。

## 观测内容

- CPU 使用率、逻辑核心数、内存、文件系统水位、1/5/15 分钟负载及每核负载。
- JVM 运行时间、堆内存、线程数、进程 CPU，以及 Hikari 数据库连接池活跃/空闲/上限/等待线程数。
- 今日并发曲线（5 分钟峰值）和今日单次响应最慢渠道 Top5，从后台仪表盘迁移，沿用现有使用记录统计口径。
- 资源趋势仅保留当前页面最近 60 个采样点，不持久化；并发/耗时数据来自已有数据库记录。

## 采样与范围

- `GET /api/admin/operations/system`：所有管理员共享 5 秒采样缓存；页面每 10 秒刷新。
- `GET /api/admin/operations/traffic?zoneId=Asia/Shanghai`：按指定时区计算今日窗口；页面每 30 秒刷新。不重复查询 Token 排行和业务仪表盘统计。
- 页面支持暂停自动刷新、手动刷新；后台页签不持续轮询。资源与请求查询独立，单个接口失败不隐藏其他指标。失败时标注旧数据，不显示为健康。
- Linux 使用 `/proc/stat`、`/proc/meminfo` 和 `/proc/loadavg`。CPU 按两次有效采样的差值计算，首次采样显示待采集。CPU、内存和负载均为内核视图，不与 cgroup 配额混算。内存使用 `MemTotal - MemAvailable`。
- 非 Linux 使用 JVM 可见的系统指标，明确标注范围；不提供 1/5/15 分钟负载。内存为总量减空闲量，包含缓存等占用，不能等同于操作系统的内存压力。
- 磁盘指配置目录所在的文件系统，不枚举所有挂载点。Docker 默认看到容器根文件系统，不承诺物理宿主机所有磁盘；Docker Desktop 的 Linux 内核资源属于 Linux 虚拟机。
- CPU、内存、磁盘、JVM 堆 ≥80% 预警、≥95% 高危；1 分钟负载除以核心数 ≥1 预警、≥1.5 高危；连接池等待触发预警。缺失指标不能产生“健康”结论，但其他已知高危信号仍保留。
- “资源健康”仅是容量观察，不代表所有上游可用、数据库查询成功率或端到端 SLA。并发来自已有使用记录的在途区间，可能受异常退出遗留记录影响。

## 配置

| 环境变量 | 默认值 | 含义 |
| --- | --- | --- |
| `API2API_OPERATIONS_PROC_PATH` | `/proc` | Linux 内核指标目录 |
| `API2API_OPERATIONS_DISK_PATH` | `/` | 文件系统水位采集目录 |

需要观察其他磁盘时，将目标文件系统的一个目录只读挂载到容器，并将 `API2API_OPERATIONS_DISK_PATH` 指向该目录。不要开启 privileged、挂载 Docker socket，或将这些路径暴露为用户可选参数。指标接口不返回环境变量、命令行、连接字符串和密钥。

## 实现参考

研究了相邻 `sub2api` 仓库的 `ops_metrics_collector.go`、`ops_metrics_collector_memory_test.go` 和独立运维页面组织方式。这里没有照搬其容器优先的统计策略：本需求以宿主机为主，因此明确使用 Linux 内核视图，并保留“分子分母同源、不可用不等于零”的原则。

## 验证

新增测试覆盖内核指标解析、CPU 差分/计数回退、采样缓存、健康阈值、缺失指标、管理员鉴权、无会话请求、时区校验及前端数据契约。

当前仓库全量 `mvn test` 的既有测试编译错误涉及 `UsageRecord.rehydrate` 调用参数未同步（包括 DashboardResponseConverterTest、UsageRecordHttpConverterTest 等）。本次未修改这些无关用例；新增监控测试与可编译的 DashboardAnalyticsServiceTest 使用临时 testIncludes 配置隔离执行。
