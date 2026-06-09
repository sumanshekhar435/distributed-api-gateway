package com.order.orderservice.controller;

import com.order.orderservice.model.dto.request.OrderRequest;
import com.order.orderservice.model.dto.response.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final List<OrderResponse> orders = new ArrayList<>();
    private static final AtomicLong counter = new AtomicLong(1);

    static {
        orders.add(OrderResponse.builder()
                .id(1L).userId("1").product("iPhone 15")
                .quantity(1).totalPrice(79999.0)
                .status("DELIVERED")
                .createdAt(LocalDateTime.now().minusDays(5)).build());
        orders.add(OrderResponse.builder()
                .id(2L).userId("1").product("MacBook Air M2")
                .quantity(1).totalPrice(114900.0)
                .status("SHIPPED")
                .createdAt(LocalDateTime.now().minusDays(2)).build());
        orders.add(OrderResponse.builder()
                .id(3L).userId("2").product("AirPods Pro")
                .quantity(2).totalPrice(49800.0)
                .status("PROCESSING")
                .createdAt(LocalDateTime.now().minusHours(6)).build());
        counter.set(4L);
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders(
            @RequestHeader(value = "X-User-Id",        required = false) String userId,
            @RequestHeader(value = "X-Username",       required = false) String username,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("[{}] GET /api/orders - requested by: {}", correlationId, username);

        List<OrderResponse> result = (userId != null)
                ? orders.stream().filter(o -> o.getUserId().equals(userId)).toList()
                : orders;

        return ResponseEntity.ok(Map.of(
                "orders",      result,
                "total",       result.size(),
                "requestedBy", username != null ? username : "unknown"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("[{}] GET /api/orders/{}", correlationId, id);

        return orders.stream()
                .filter(o -> o.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "X-User-Id",        required = false) String userId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("[{}] POST /api/orders - user: {}", correlationId, userId);

        OrderResponse order = OrderResponse.builder()
                .id(counter.getAndIncrement())
                .userId(userId != null ? userId : "anonymous")
                .product(request.getProduct())
                .quantity(request.getQuantity())
                .totalPrice(request.getPrice() * request.getQuantity())
                .status("PROCESSING")
                .createdAt(LocalDateTime.now())
                .build();

        orders.add(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Order placed successfully",
                "order",   order
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status",      "UP",
                "service",     "order-service",
                "totalOrders", orders.size()
        ));
    }
}