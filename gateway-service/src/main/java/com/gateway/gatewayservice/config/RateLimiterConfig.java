package com.gateway.gatewayservice.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate=10 (10 req/sec), burstCapacity=20, requestedTokens=1
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // JWT filter ne X-User-Id header add kiya hai
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");

            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }

            // Agar user ID nahi hai toh IP se limit karo
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress()
                              .getAddress().getHostAddress()
                    : "unknown";

            return Mono.just("ip:" + ip);
        };
    }
}