package com.api2api.infr.monitoring;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import static com.api2api.infr.monitoring.HostResourceReader.resource;
import static com.api2api.infr.monitoring.SystemSnapshot.*;

@Component
@Slf4j
public class SystemMetricsCollector {
    private static final long CACHE_NANOS = 5_000_000_000L;
    private final HostResourceReader reader;
    private final DataSource dataSource;
    private final Path proc;
    private final Path diskPath;
    private final boolean linux;
    private SystemSnapshot cached;
    private long lastSampleNanos;
    private HostResourceReader.CpuTicks previousCpu;

    public SystemMetricsCollector(HostResourceReader reader, DataSource dataSource,
            @Value("${api2api.operations.proc-path:/proc}") String procPath,
            @Value("${api2api.operations.disk-path:/}") String diskPath) {
        this.reader = reader;
        this.dataSource = dataSource;
        this.proc = Path.of(procPath);
        this.diskPath = Path.of(diskPath);
        this.linux = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("linux");
    }

    /** Cache shared across administrators; sampling never sleeps or launches shell commands. */
    public synchronized SystemSnapshot snapshot() {
        long now = System.nanoTime();
        if (cached != null && now - lastSampleNanos < CACHE_NANOS) {
            return cached;
        }
        List<String> notices = new ArrayList<>();
        var os = ManagementFactory.getOperatingSystemMXBean();
        var extended = os instanceof com.sun.management.OperatingSystemMXBean bean ? bean : null;
        Double cpu = null;
        Integer cores = null;
        List<Double> load = List.of();
        Resource memory = resource(-1, -1);
        if (linux) {
            try {
                var ticks = reader.readCpu(proc);
                cpu = HostResourceReader.cpuPercent(previousCpu, ticks);
                previousCpu = ticks;
                cores = ticks.cores() > 0 ? ticks.cores() : null;
                if (cpu == null) notices.add("CPU 使用率等待下一次有效采样");
            } catch (IOException | NumberFormatException exception) {
                previousCpu = null;
                log.warn("event=operations_cpu_unavailable", exception);
                notices.add("宿主机 CPU 采集不可用");
            }
            try {
                memory = reader.readMemory(proc);
            } catch (IOException | NumberFormatException exception) {
                log.warn("event=operations_memory_unavailable", exception);
                notices.add("宿主机内存采集不可用");
            }
            try {
                load = reader.readLoad(proc);
            } catch (IOException | NumberFormatException exception) {
                log.warn("event=operations_load_unavailable", exception);
                notices.add("宿主机负载采集不可用");
            }
        } else {
            cores = os.getAvailableProcessors();
            if (extended != null) {
                cpu = validPercent(extended.getCpuLoad());
                memory = resource(extended.getTotalMemorySize() - extended.getFreeMemorySize(), extended.getTotalMemorySize());
            }
            notices.add("非 Linux 环境：展示 JVM 可见系统资源，不宣称为物理宿主机指标；1/5/15 分钟负载不可用");
        }
        Resource disk = resource(-1, -1);
        try {
            FileStore store = Files.getFileStore(diskPath);
            disk = resource(store.getTotalSpace() - store.getUsableSpace(), store.getTotalSpace());
        } catch (IOException | NumberFormatException exception) {
            log.warn("event=operations_disk_unavailable", exception);
            notices.add("监控目录所在文件系统采集不可用");
        }
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        RuntimeMetrics runtime = new RuntimeMetrics(ManagementFactory.getRuntimeMXBean().getUptime(),
                resource(heap.getUsed(), heap.getMax()), ManagementFactory.getThreadMXBean().getThreadCount(),
                extended == null ? null : validPercent(extended.getProcessCpuLoad()));
        DatabasePool pool = databasePool();
        Double loadPercent = load.isEmpty() || cores == null ? null : load.get(0) / cores * 100;
        cached = new SystemSnapshot(Instant.now(), linux ? Scope.HOST_KERNEL : Scope.JVM_VISIBLE,
                os.getName() + " / " + os.getArch(), cores, cpu, load, loadPercent, memory, disk,
                diskPath.toString(), runtime, pool,
                health(cpu, memory.percent(), disk.percent(), loadPercent, runtime.heap().percent(), pool), List.copyOf(notices));
        lastSampleNanos = now;
        return cached;
    }

    private DatabasePool databasePool() {
        if (dataSource instanceof HikariDataSource hikari) {
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool != null) return new DatabasePool(pool.getActiveConnections(), pool.getIdleConnections(),
                    pool.getTotalConnections(), hikari.getMaximumPoolSize(), pool.getThreadsAwaitingConnection());
        }
        return null;
    }

    private static Double validPercent(double fraction) {
        return Double.isFinite(fraction) && fraction >= 0 && fraction <= 1 ? fraction * 100 : null;
    }

    public static Health health(Double cpu, Double memory, Double disk, Double load, Double heap, DatabasePool pool) {
        List<Double> percentages = java.util.stream.Stream.of(cpu, memory, disk, heap).filter(java.util.Objects::nonNull).toList();
        if (percentages.stream().anyMatch(value -> value >= 95) || (load != null && load >= 150)) return Health.CRITICAL;
        if (percentages.stream().anyMatch(value -> value >= 80) || (load != null && load >= 100)
                || (pool != null && pool.waiting() > 0)) return Health.WARNING;
        if (cpu == null || memory == null || disk == null || load == null || heap == null || pool == null) return Health.UNKNOWN;
        return Health.HEALTHY;
    }
}
