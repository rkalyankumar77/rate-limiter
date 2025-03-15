package in.kodekrafter.ratelimiter.impl;

import in.kodekrafter.ratelimiter.RateLimiter;

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
        return false;
    }

    record Bucket(double tokens, Instant lastRefill) {}
}
