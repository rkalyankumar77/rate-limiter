# Rate Limiting Algorithms: Theory and Comparison

Rate limiting is a strategy used to control the amount of requests that clients can make to an API or service within a given timeframe. This document explains the theory behind popular rate limiting algorithms, their advantages, disadvantages, and best use cases.

## 1. Fixed Window Counter

### Theory

The Fixed Window Counter algorithm divides time into fixed windows (e.g., 1-minute intervals) and allows a maximum number of requests in each window. For example, if the limit is 100 requests per minute, the algorithm tracks requests from 1:00-1:59, 2:00-2:59, and so on.

### How It Works

1. Each window has a counter starting at zero
2. When a request arrives, increment the counter
3. If the counter exceeds the limit, reject the request
4. When a new window starts, reset the counter to zero

### Advantages

- Very simple to understand and implement
- Low memory usage (only need to store one counter per user/key)
- Works well for low-precision rate limiting

### Disadvantages

- Can cause "bursts" at window boundaries
- Example: If limit is 100 requests per minute, a user could make 100 requests at 1:59 and another 100 at 2:01 (200 requests in just two minutes)

### Best Use Cases

- Simple applications with low to moderate traffic
- When occasional traffic spikes are acceptable
- When implementation simplicity is a priority

## 2. Sliding Window Counter

### Theory

The Sliding Window Counter algorithm improves upon the Fixed Window by considering both the current window and the previous window, creating a weighted average that "slides" over time. This smooths out the boundary problem of the fixed window approach.

### How It Works

1. Track request count in the current fixed window
2. Also retain the count from the previous window
3. Calculate a weighted rolling average based on how far into the current window we are
4. Formula: count = current_count + previous_count * (1 - position_in_window)

### Advantages

- Smoother rate limiting without sharp window boundaries
- Still relatively simple to implement
- Moderate memory usage (only two counters per user/key)

### Disadvantages

- Not perfectly precise - approximates request distribution within windows
- Slightly more complex than fixed window

### Best Use Cases

- Applications with moderate traffic where smoother rate limiting is desired
- When you need a balance between implementation simplicity and smoothing out traffic

## 3. Token Bucket

### Theory

The Token Bucket algorithm models a bucket that continuously fills with tokens at a constant rate up to a maximum capacity. Each request consumes one token, and if no tokens are available, the request is rejected.

### How It Works

1. The bucket has a maximum capacity of tokens (burst capacity)
2. Tokens are added to the bucket at a constant rate (e.g., 10 tokens per second)
3. Each request consumes one token from the bucket
4. If the bucket is empty, the request is rejected
5. Tokens are capped at the maximum capacity

### Advantages

- Allows for "bursting" patterns (short spikes in traffic) up to bucket capacity
- Provides smooth rate limiting over time
- Models a constant long-term rate with flexibility for short bursts
- Natural fit for APIs with a steady supply of resources

### Disadvantages

- More complex to implement, especially in a distributed system
- Must track last refill time and current token count

### Best Use Cases

- APIs where allowing occasional bursts is acceptable or desirable
- When you have a system that regenerates capacity at a steady rate
- Most commercial APIs (like AWS, Google Cloud, etc.)

## 4. Leaky Bucket

### Theory

The Leaky Bucket algorithm models a bucket with a hole that leaks at a constant rate. Requests fill the bucket, and if adding a new request would cause overflow, it's rejected.

### How It Works

1. The bucket has a maximum capacity (for queuing requests)
2. Requests "leak" out of the bucket at a constant rate
3. New requests are added to the bucket
4. If the bucket would overflow, reject the request

### Advantages

- Ensures a perfectly constant outflow rate
- Good for traffic shaping and smoothing out bursts
- Can be implemented as a simple FIFO queue with a processor

### Disadvantages

- Can cause increased latency for requests that are queued
- In simple implementations, doesn't account for request size differences
- More complex to implement properly

### Best Use Cases

- Traffic shaping where you need a very constant processing rate
- Systems where request processing is the bottleneck
- When you need to completely smooth out any bursts

## 5. Sliding Window Log

### Theory

The Sliding Window Log algorithm keeps a timestamped log of all requests within the sliding window timeframe. This provides the most accurate rate limiting but at the cost of higher memory usage.

### How It Works

1. Store a timestamp for each request in a log/queue
2. When a new request arrives, remove all timestamps from the log that are outside the current window
3. Count the remaining timestamps in the log
4. If the count is less than the limit, accept the request and add its timestamp to the log
5. Otherwise, reject the request

### Advantages

- Most accurate rate limiting across a sliding time window
- Provides perfect sliding window behavior
- No approximations or rough edges

### Disadvantages

- High memory usage (need to store a timestamp for every request in the window)
- More complex to implement
- Can be inefficient for high-volume APIs

### Best Use Cases

- When precision is critical
- Lower-volume APIs where memory usage isn't a concern
- When you need perfectly smooth boundaries

## 6. Adaptive Rate Limiter

### Theory

The Adaptive Rate Limiter dynamically adjusts rate limits based on current system conditions or business rules. Rather than having fixed limits, the system can respond to load, time of day, user tier, or other factors.

### How It Works

1. Implement one of the above algorithms as the base
2. Periodically measure relevant system metrics (CPU load, queue depth, error rates)
3. Adjust rate limits dynamically based on these metrics
4. Optionally apply different rules for different users or services

### Advantages

- Can maximize system utilization under varying conditions
- Provides better user experience during off-peak times
- Can prioritize traffic based on business importance

### Disadvantages

- Much more complex to implement and test
- Requires monitoring and adjustment mechanisms
- Harder to predict system behavior

### Best Use Cases

- High-scale systems with varying load patterns
- When maximizing resource utilization is critical
- Systems with different tiers of service

## 7. Distributed Rate Limiting (Redis-Based)

### Theory

Distributed rate limiting extends the above algorithms to work across multiple servers or instances by using a shared data store (like Redis) to maintain rate limiting state.

### How It Works

1. Use a centralized data store (Redis) to track counters, tokens, or timestamps
2. Use atomic operations or Lua scripts to ensure consistency
3. Apply the same algorithms as above, but with distributed state
4. Use appropriate TTLs (time-to-live) for keys to manage memory

### Advantages

- Works across multiple application servers
- Provides consistent rate limiting in scaled environments
- Can leverage Redis's performance and atomicity

### Disadvantages

- Introduces network latency for rate limiting checks
- Creates a dependency on Redis
- More complex to implement and test
- Single point of failure without Redis clustering

### Best Use Cases

- Microservices architectures
- Applications scaling across multiple instances
- When consistent rate limiting is required across a distributed system

## Comparison Table

|Algorithm|Precision|Memory Usage|Complexity|Burst Handling|Distributed Friendly|
|---|---|---|---|---|---|
|Fixed Window|Low|Very Low|Simple|Poor|Yes|
|Sliding Window Counter|Medium|Low|Medium|Medium|Yes|
|Token Bucket|High|Low|Medium|Good|With care|
|Leaky Bucket|High|Medium|Medium|Poor|With care|
|Sliding Window Log|Very High|High|Complex|Medium|Yes, but costly|
|Adaptive|Varies|Varies|Very Complex|Configurable|Yes, with complexity|

## Choosing the Right Algorithm

When selecting a rate limiting algorithm, consider:

1. **Traffic patterns**: Do you need to handle bursts (Token Bucket) or enforce smooth traffic (Leaky Bucket)?
2. **Resource constraints**: How much memory can you afford to use for rate limiting?
3. **Accuracy requirements**: How precise does your rate limiting need to be?
4. **Implementation complexity**: How complex of an algorithm can you maintain?
5. **Distribution needs**: Will this run on a single server or across many?

For most applications, the Sliding Window Counter provides a good balance between precision and complexity. For APIs that need to allow bursts, the Token Bucket is often the best choice. Fixed Window is suitable for the simplest use cases, while Sliding Window Log is best when you need perfect accuracy.

In distributed environments, consider using Redis with Sliding Window Counter or Token Bucket algorithms for the best balance of performance and accuracy. 