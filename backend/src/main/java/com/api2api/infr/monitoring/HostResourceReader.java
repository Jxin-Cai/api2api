package com.api2api.infr.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.stereotype.Component;

/** Linux kernel metrics, deliberately independent of container cgroup limits. */
@Component
public class HostResourceReader {
    public record CpuTicks(long total, long idle, int cores) { }

    public CpuTicks readCpu(Path proc) throws IOException {
        List<String> lines = Files.readAllLines(proc.resolve("stat"));
        String[] fields = lines.stream().filter(line -> line.startsWith("cpu ")).findFirst()
                .orElseThrow(() -> new IOException("Missing aggregate CPU counters")).trim().split("\\s+");
        if (fields.length < 5) {
            throw new IOException("Incomplete CPU counters");
        }
        // guest and guest_nice are already included in user/nice; do not double count them.
        long[] ticks = Arrays.stream(fields).skip(1).limit(8).mapToLong(Long::parseLong).toArray();
        long idle = ticks[3] + (ticks.length > 4 ? ticks[4] : 0);
        int cores = (int) lines.stream().filter(line -> line.matches("cpu\\d+\\s.*")).count();
        return new CpuTicks(Arrays.stream(ticks).sum(), idle, cores);
    }

    public SystemSnapshot.Resource readMemory(Path proc) throws IOException {
        Map<String, Long> memory = new HashMap<>();
        for (String line : Files.readAllLines(proc.resolve("meminfo"))) {
            if (!line.startsWith("MemTotal:") && !line.startsWith("MemAvailable:")) continue;
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 3 || !"kB".equals(parts[2])) throw new IOException("Invalid memory counter format");
            memory.put(parts[0], Long.parseLong(parts[1]) * 1024);
        }
        Long total = memory.get("MemTotal:");
        Long available = memory.get("MemAvailable:");
        if (total == null || available == null || total <= 0 || available < 0 || available > total) {
            throw new IOException("Missing or invalid Linux memory counters");
        }
        return resource(total - available, total);
    }

    public List<Double> readLoad(Path proc) throws IOException {
        String[] fields = Files.readString(proc.resolve("loadavg")).trim().split("\\s+");
        if (fields.length < 3) {
            throw new IOException("Incomplete load averages");
        }
        List<Double> load = Arrays.stream(fields).limit(3).map(Double::valueOf).toList();
        if (load.stream().anyMatch(value -> !Double.isFinite(value) || value < 0)) {
            throw new IOException("Invalid load average");
        }
        return load;
    }

    public static Double cpuPercent(CpuTicks previous, CpuTicks current) {
        if (previous == null || current.cores() != previous.cores()) {
            return null;
        }
        long total = current.total() - previous.total();
        long idle = current.idle() - previous.idle();
        return total <= 0 || idle < 0 || idle > total ? null : 100.0 * (total - idle) / total;
    }

    public static SystemSnapshot.Resource resource(long used, long total) {
        return total <= 0 || used < 0 || used > total
                ? new SystemSnapshot.Resource(null, null, null)
                : new SystemSnapshot.Resource(used, total, 100.0 * used / total);
    }
}
