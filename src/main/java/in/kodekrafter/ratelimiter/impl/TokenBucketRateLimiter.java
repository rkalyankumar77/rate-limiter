package in.kodekrafter.ratelimiter.impl;

import in.kodekrafter.ratelimiter.RateLimiter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TokenBucketRateLimiter implements RateLimiter {
    private final long capacity;
    private final double refillRate;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(long capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
    }

    @Override
    public boolean tryAcquire(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, refillRate));
        return bucket.tryConsume();
    }

    private static class Bucket {
        private final long capacity;
        private final double refillRate;
        private final AtomicLong tokens;
        private volatile Instant lastRefillTime;

        public Bucket(long capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTime = Instant.now();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            Instant now = Instant.now();
            long elapsedTime = now.toEpochMilli() - lastRefillTime.toEpochMilli();
            long newTokens = (long) (elapsedTime * refillRate / 1000);
            if (newTokens > 0) {
                tokens.getAndUpdate(current -> Math.min(capacity, current + newTokens));
                lastRefillTime = now;
            }
        }
    }
}