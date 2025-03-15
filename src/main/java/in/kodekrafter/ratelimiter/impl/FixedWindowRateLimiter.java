package in.kodekrafter.ratelimiter.impl;

import in.kodekrafter.ratelimiter.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class FixedWindowRateLimiter implements RateLimiter {

    private final long maxRequestsPerWindow;
    private final Duration windowSize;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(long maxRequestsPerWindow, Duration windowSize) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowSize = windowSize;
    }

    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Window window = windows.compute(key, (k, existingWindow) -> {
            if (existingWindow == null || now.isAfter(existingWindow.startTime.plus(windowSize))) {
                return new Window(now, 1L);
            }
            long val = existingWindow.counter.getAndIncrement();
            log.info("Window counter: {} ", val);
            return existingWindow;
        });
        return window.counter.get() <= maxRequestsPerWindow;
    }


    record Window(Instant startTime, AtomicLong counter) {
        public Window(Instant startTime, long counter) {
            this(startTime, new AtomicLong(counter));
        }
    }
}

