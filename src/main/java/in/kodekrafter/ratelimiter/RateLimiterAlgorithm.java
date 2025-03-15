package in.kodekrafter.ratelimiter;

public enum RateLimiterAlgorithm {
    FIXED_WINDOW,
    SLIDING_WINDOW,
    TOKEN_BUCKET,
    LEAKY_BUCKET,
    ADAPTIVE
}
