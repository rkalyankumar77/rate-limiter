package in.kodekrafter.ratelimiter;

public interface RateLimiter {
    public boolean tryAcquire(String key);
}
