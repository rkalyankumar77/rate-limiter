package in.kodekrafter.ratelimiter.impl;

import in.kodekrafter.ratelimiter.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketRateLimiterTest {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiterTest.class);

    @Test
    @DisplayName("Should allow first N requests up to capacity")
    void shouldAllowFirstNRequestsUpToCapacity() {
        // Given a token bucket with capacity 3 and a slow refill rate
        RateLimiter limiter = new TokenBucketRateLimiter(3, 1.0); // 1 token per second
        String key = "test-client";

        // When making requests up to capacity
        boolean first = limiter.tryAcquire(key);
        boolean second = limiter.tryAcquire(key);
        boolean third = limiter.tryAcquire(key);
        boolean fourth = limiter.tryAcquire(key);

        // Then first 3 should succeed and 4th should fail
        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(third).isTrue();
        assertThat(fourth).isFalse();
    }

    @Test
    @DisplayName("Should refill tokens over time")
    void shouldRefillTokensOverTime() throws InterruptedException {
        // Given a token bucket with capacity 1 and refill rate of 10 per second (one token every 100ms)
        RateLimiter limiter = new TokenBucketRateLimiter(1, 10.0);
        String key = "test-client";

        // When consuming the only token
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isFalse(); // No tokens left

        // And waiting for a refill
        Thread.sleep(110); // Wait a bit more than 100ms to ensure refill

        // Then the new token should be available
        assertThat(limiter.tryAcquire(key)).isTrue();
    }

    @Test
    @DisplayName("Should respect maximum capacity during refills")
    void shouldRespectMaximumCapacity() throws InterruptedException {
        // Given a token bucket with capacity 2 and refill rate
        RateLimiter limiter = new TokenBucketRateLimiter(2, 10.0); // 10 tokens per second
        String key = "test-client";

        // When consuming all tokens
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isFalse();

        // And waiting for more than enough time to refill beyond capacity
        Thread.sleep(500); // Should refill 5 tokens, but max is 2

        // Then we should only get 2 tokens max
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple clients separately")
    void shouldHandleMultipleClientsSeparately() {
        // Given a token bucket limiter
        RateLimiter limiter = new TokenBucketRateLimiter(2, 1.0);
        String client1 = "client-1";
        String client2 = "client-2";

        // When client1 uses their tokens
        assertThat(limiter.tryAcquire(client1)).isTrue();
        assertThat(limiter.tryAcquire(client1)).isTrue();
        assertThat(limiter.tryAcquire(client1)).isFalse();

        // Then client2 should still have their tokens
        assertThat(limiter.tryAcquire(client2)).isTrue();
        assertThat(limiter.tryAcquire(client2)).isTrue();
        assertThat(limiter.tryAcquire(client2)).isFalse();
    }

    @Test
    @DisplayName("Should handle partial token refills")
    void shouldHandlePartialTokenRefills() throws InterruptedException {
        // Given a token bucket with slow refill rate (1 token every 200ms)
        RateLimiter limiter = new TokenBucketRateLimiter(3, 5.0);
        String key = "test-client";

        // When consuming all tokens
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isTrue();
        assertThat(limiter.tryAcquire(key)).isFalse();

        // And waiting for partial refill (should refill only 0.5 tokens in 100ms)
        Thread.sleep(110);

        // Then request should still fail
        assertThat(limiter.tryAcquire(key)).isFalse();

        // But after waiting again (total ~220ms, should have 1+ tokens)
        Thread.sleep(110);

        // Then request should succeed
        assertThat(limiter.tryAcquire(key)).isTrue();
        // But not the next one
        assertThat(limiter.tryAcquire(key)).isFalse();
    }

    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() throws InterruptedException {
        // Given a token bucket with fixed capacity
        int capacity = 100;
        RateLimiter limiter = new TokenBucketRateLimiter(capacity, 1.0);
        String key = "concurrent-client";

        int threadCount = 10;
        int requestsPerThread = 20;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When making concurrent requests
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (limiter.tryAcquire(key)) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then only capacity number of requests should succeed
        assertThat(successCount.get()).isEqualTo(capacity);
    }

    @Test
    @DisplayName("Should handle zero capacity")
    void shouldHandleZeroCapacity() {
        // Given a token bucket with zero capacity
        RateLimiter limiter = new TokenBucketRateLimiter(0, 1.0);
        String key = "test-client";

        // When trying to acquire
        // Then all requests should be denied
        assertThat(limiter.tryAcquire(key)).isFalse();
    }
}