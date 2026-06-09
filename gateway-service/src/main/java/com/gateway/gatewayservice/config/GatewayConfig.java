package com.gateway.gatewayservice.config;

import com.gateway.gatewayservice.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    @Autowired
    private org.springframework.cloud.gateway.filter.ratelimit.KeyResolver userKeyResolver;

    @Value("${services.auth-url:http://localhost:8083}")
    private String authServiceUrl;

    @Value("${services.order-url:http://localhost:8081}")
    private String orderServiceUrl;

    @Value("${services.product-url:http://localhost:8082}")
    private String productServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── Auth Service: PUBLIC (no JWT needed) ──────────────
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .uri(authServiceUrl))

                // ── Order Service: JWT + Rate Limit + Circuit Breaker ──
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter);
                                    c.setKeyResolver(userKeyResolver);
                                })
                                .circuitBreaker(c -> c
                                        .setName("orderCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/orders")))
                        .uri(orderServiceUrl))

                // ── Product Service: JWT + Rate Limit + Circuit Breaker ──
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter);
                                    c.setKeyResolver(userKeyResolver);
                                })
                                .circuitBreaker(c -> c
                                        .setName("productCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/products")))
                        .uri(productServiceUrl))

                .build();
    }
}