package in.kodekrafter.ratelimiter;

public enum RateLimiterAlgorithm {
    FIXED_WINDOW,
    SLIDING_WINDOW,
    SLIDING_WINDOW_LOG,
    TOKEN_BUCKET,
    LEAKY_BUCKET,
    ADAPTIVE
}
