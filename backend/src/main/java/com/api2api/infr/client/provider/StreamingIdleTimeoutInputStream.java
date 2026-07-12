package com.api2api.infr.client.provider;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Closes an upstream streaming body when the first event or a subsequent event
 * does not arrive within its configured idle window.
 */
final class StreamingIdleTimeoutInputStream extends InputStream {

    private static final ScheduledExecutorService TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(
            daemonThreadFactory()
    );

    private final InputStream delegate;
    private final Duration idleTimeout;
    private final Object timeoutLock = new Object();
    private ScheduledFuture<?> timeoutTask;
    private volatile boolean timedOut;
    private volatile boolean closed;
    private volatile IOException timeoutCloseFailure;

    StreamingIdleTimeoutInputStream(InputStream delegate, Duration firstByteTimeout, Duration idleTimeout) {
        this.delegate = Objects.requireNonNull(delegate, "Streaming body must not be null");
        this.idleTimeout = positiveDuration(idleTimeout, "Streaming idle timeout must be positive");
        scheduleTimeout(positiveDuration(firstByteTimeout, "Streaming first byte timeout must be positive"));
    }

    @Override
    public int read() throws IOException {
        try {
            int value = delegate.read();
            afterRead(value < 0 ? -1 : 1);
            return value;
        } catch (IOException exception) {
            throw timeoutOrOriginal(exception);
        }
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        if (length == 0) {
            return 0;
        }
        try {
            int count = delegate.read(bytes, offset, length);
            afterRead(count);
            return count;
        } catch (IOException exception) {
            throw timeoutOrOriginal(exception);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (timeoutLock) {
            if (closed) {
                return;
            }
            closed = true;
            cancelTimeout();
        }
        delegate.close();
    }

    private void afterRead(int count) throws SocketTimeoutException {
        if (timedOut) {
            throw timeoutException();
        }
        synchronized (timeoutLock) {
            if (count < 0 || closed) {
                cancelTimeout();
                return;
            }
            scheduleTimeout(idleTimeout);
        }
    }

    private IOException timeoutOrOriginal(IOException exception) {
        if (!timedOut) {
            return exception;
        }
        SocketTimeoutException timeoutException = timeoutException();
        timeoutException.initCause(exception);
        return timeoutException;
    }

    private void scheduleTimeout(Duration timeout) {
        synchronized (timeoutLock) {
            cancelTimeout();
            timeoutTask = TIMEOUT_EXECUTOR.schedule(this::expire, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void expire() {
        synchronized (timeoutLock) {
            if (closed) {
                return;
            }
            timedOut = true;
        }
        try {
            delegate.close();
        } catch (IOException exception) {
            timeoutCloseFailure = exception;
        }
    }

    private void cancelTimeout() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
    }

    private SocketTimeoutException timeoutException() {
        SocketTimeoutException exception = new SocketTimeoutException("Upstream streaming body exceeded its idle timeout");
        if (timeoutCloseFailure != null) {
            exception.addSuppressed(timeoutCloseFailure);
        }
        return exception;
    }

    private static Duration positiveDuration(Duration duration, String message) {
        Objects.requireNonNull(duration, message);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(message);
        }
        return duration;
    }

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "api2api-stream-idle-timeout");
            thread.setDaemon(true);
            return thread;
        };
    }
}
