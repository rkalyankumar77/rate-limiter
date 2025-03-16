package in.kodekrafter.ratelimiter.impl;

import in.kodekrafter.ratelimiter.RateLimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class TokenBucketRateLimiter implements RateLimiter {

    private final long capacity;
    private final double refillRate;

    private final Map<String, Bucket> buckets = new HashMap<>();

    public TokenBucketRateLimiter(long capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
    }

    @Override
    public boolean tryAcquire(String key) {
        Bucket bucket = buckets.compute(key, (k, existingBucket) -> {
           if (existingBucket == null) {
               return new Bucket(capacity - 1.0, Instant.now());
           }
           Instant now = Instant.now();
           Duration elapsed = Duration.between(existingBucket.lastRefill, now);
           double tokensToAdd = elapsed.toMillis() / refillRate * 1000.0;
           double newTokens = Math.min(existingBucket.tokens + tokensToAdd, capacity);
           if (newTokens >= 1){
               return new Bucket(newTokens - 1, now);
           }
           return existingBucket;
        });
        return bucket.tokens >= 0;
    }

    record Bucket(double tokens, Instant lastRefill) {}
}
