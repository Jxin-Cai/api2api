package com.api2api.infr.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class HostResourceReaderTest {
    @TempDir Path proc;
    private final HostResourceReader reader = new HostResourceReader();

    @Test
    void test_excludesGuestTicks_when_readingCpu() throws IOException {
        // Arrange
        Files.writeString(proc.resolve("stat"), "cpu  10 20 30 40 50 60 70 80 900 900\ncpu0 0\ncpu1 0\n");
        // Act
        var result = reader.readCpu(proc);
        // Assert
        assertThat(result).isEqualTo(new HostResourceReader.CpuTicks(360, 90, 2));
    }

    @Test
    void test_usesAvailableMemory_when_linuxCountersExist() throws IOException {
        // Arrange
        Files.writeString(proc.resolve("meminfo"), "MemTotal: 1000 kB\nMemFree: 10 kB\nMemAvailable: 400 kB\n");
        // Act
        var result = reader.readMemory(proc);
        // Assert
        assertThat(result).isEqualTo(new SystemSnapshot.Resource(600L * 1024, 1000L * 1024, 60.0));
    }

    @Test
    void test_rejectsIncompleteMemory_when_availableCounterMissing() throws IOException {
        // Arrange
        Files.writeString(proc.resolve("meminfo"), "MemTotal: 1000 kB\n");
        // Act / Assert
        assertThatThrownBy(() -> reader.readMemory(proc)).isInstanceOf(IOException.class);
    }

    @Test
    void test_readsThreeLoadWindows_when_loadavgExists() throws IOException {
        // Arrange
        Files.writeString(proc.resolve("loadavg"), "1.50 2.25 3.00 1/99 100\n");
        // Act
        var result = reader.readLoad(proc);
        // Assert
        assertThat(result).containsExactly(1.5, 2.25, 3.0);
    }

    @Test
    void test_waitsForSecondSample_when_cpuHasNoBaseline() {
        // Arrange
        var current = new HostResourceReader.CpuTicks(100, 50, 2);
        // Act / Assert
        assertThat(HostResourceReader.cpuPercent(null, current)).isNull();
    }

    @Test
    void test_calculatesIntervalUsage_when_cpuCountersAdvance() {
        // Arrange
        var previous = new HostResourceReader.CpuTicks(100, 50, 2);
        var current = new HostResourceReader.CpuTicks(200, 75, 2);
        // Act / Assert
        assertThat(HostResourceReader.cpuPercent(previous, current)).isEqualTo(75.0);
    }

    @Test
    void test_returnsUnknown_when_cpuCountersReset() {
        // Arrange
        var previous = new HostResourceReader.CpuTicks(200, 75, 2);
        var current = new HostResourceReader.CpuTicks(100, 50, 2);
        // Act / Assert
        assertThat(HostResourceReader.cpuPercent(previous, current)).isNull();
    }

    @Test
    void test_returnsUnknown_when_resourceLimitIsUnavailable() {
        // Arrange / Act
        var result = HostResourceReader.resource(100, -1);
        // Assert
        assertThat(result.percent()).isNull();
    }
}
