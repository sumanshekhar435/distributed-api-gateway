package com.product.productservice.controller;

import com.product.productservice.model.dto.response.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final List<ProductResponse> products = new ArrayList<>();

    static {
        products.add(ProductResponse.builder()
                .id(1L).name("iPhone 15").category("Electronics")
                .price(79999.0).stock(50)
                .description("Apple iPhone 15 128GB").build());
        products.add(ProductResponse.builder()
                .id(2L).name("MacBook Air M2").category("Laptops")
                .price(114900.0).stock(20)
                .description("Apple MacBook Air M2 chip").build());
        products.add(ProductResponse.builder()
                .id(3L).name("AirPods Pro").category("Audio")
                .price(24900.0).stock(100)
                .description("Apple AirPods Pro 2nd Gen").build());
        products.add(ProductResponse.builder()
                .id(4L).name("Samsung Galaxy S24").category("Electronics")
                .price(74999.0).stock(75)
                .description("Samsung Galaxy S24 256GB").build());
        products.add(ProductResponse.builder()
                .id(5L).name("Sony WH-1000XM5").category("Audio")
                .price(29990.0).stock(40)
                .description("Sony Noise Cancelling Headphones").build());
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestHeader(value = "X-Username",       required = false) String username,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("[{}] GET /api/products - user: {}, category: {}",
                correlationId, username, category);

        List<ProductResponse> result = (category != null)
                ? products.stream()
                    .filter(p -> p.getCategory().equalsIgnoreCase(category))
                    .toList()
                : products;

        return ResponseEntity.ok(Map.of(
                "products", result,
                "total",    result.size()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("[{}] GET /api/products/{}", correlationId, id);

        return products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        List<String> categories = products.stream()
                .map(ProductResponse::getCategory)
                .distinct()
                .sorted()
                .toList();
        return ResponseEntity.ok(Map.of("categories", categories));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status",        "UP",
                "service",       "product-service",
                "totalProducts", products.size()
        ));
    }
}