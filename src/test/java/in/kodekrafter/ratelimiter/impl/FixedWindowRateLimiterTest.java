package in.kodekrafter.ratelimiter.impl;

import in.kodekrafter.ratelimiter.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


import static org.assertj.core.api.Assertions.*;

@Slf4j
class FixedWindowRateLimiterTest {
    @Test
    void shouldAllowRequestsWithinLimit() {
        RateLimiter rateLimiter = new FixedWindowRateLimiter(5, Duration.ofMinutes(1));
        String key = "test-client";

        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire(key)).isTrue();
        }

        assertThat(rateLimiter.tryAcquire(key)).isFalse();
    }

    @Test
    void shouldHandleConcurrentRequests() throws InterruptedException {
        RateLimiter rateLimiter = new FixedWindowRateLimiter(100, Duration.ofMinutes(1));
        String key = "concurrent-client";
        int threadCount = 10;
        int requestsPerThread = 20;

        CountDownLatch latch;
        AtomicInteger acceptedCount;
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            latch = new CountDownLatch(threadCount);
            acceptedCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        for (int j = 0; j < requestsPerThread; j++) {
                            if (rateLimiter.tryAcquire(key)) {
                                acceptedCount.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();
        log.info("latch count: {}", latch.getCount());
        assertThat(latch.getCount()).isZero();
        assertThat(acceptedCount.get()).isEqualTo(100); // Only 100 should succeed
    }

    @Test
    void shouldRejectAllRequestsWhenLimitIsZero() {
        RateLimiter rateLimiter = new FixedWindowRateLimiter(0, Duration.ofSeconds(1));
        assertThat(rateLimiter.tryAcquire("any-client")).isFalse();
    }

    @Test
    void shouldTrackRateLimitsSeparatelyForDifferentKeys() {
        RateLimiter rateLimiter = new FixedWindowRateLimiter(2, Duration.ofSeconds(1));
        String client1 = "client-1";
        String client2 = "client-2";

        assertThat(rateLimiter.tryAcquire(client1)).isTrue();
        assertThat(rateLimiter.tryAcquire(client1)).isTrue();
        assertThat(rateLimiter.tryAcquire(client1)).isFalse();

        // Different client should have separate counter
        assertThat(rateLimiter.tryAcquire(client2)).isTrue();
        assertThat(rateLimiter.tryAcquire(client2)).isTrue();
        assertThat(rateLimiter.tryAcquire(client2)).isFalse();
    }

    @Test
    void shouldAllowRequestsAfterWindowExpires() throws InterruptedException {
        Duration windowSize = Duration.ofMillis(100);
        RateLimiter rateLimiter = new FixedWindowRateLimiter(2, windowSize);
        String key = "test-client";

        // Use up the limit
        assertThat(rateLimiter.tryAcquire(key)).isTrue();
        assertThat(rateLimiter.tryAcquire(key)).isTrue();
        assertThat(rateLimiter.tryAcquire(key)).isFalse();

        // Wait for window to expire
        Thread.sleep(100);

        // Should be allowed again
        assertThat(rateLimiter.tryAcquire(key)).isTrue();
    }
}
