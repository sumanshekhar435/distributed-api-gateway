package com.gateway.gatewayservice.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {
        log.warn("Circuit OPEN - Order Service unavailable, returning fallback");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",     "SERVICE_UNAVAILABLE");
        body.put("message",    "Order service is temporarily unavailable. Please try again later.");
        body.put("retryAfter", "10 seconds");
        body.put("timestamp",  LocalDateTime.now().toString());

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body));
    }

    @GetMapping("/products")
    public Mono<ResponseEntity<Map<String, Object>>> productFallback() {
        log.warn("Circuit OPEN - Product Service unavailable, returning fallback");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",     "SERVICE_UNAVAILABLE");
        body.put("message",    "Product service is temporarily unavailable. Please try again later.");
        body.put("retryAfter", "15 seconds");
        body.put("timestamp",  LocalDateTime.now().toString());

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body));
    }
}