package com.order.orderservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String userId;
    private String product;
    private Integer quantity;
    private Double totalPrice;
    private String status;
    private LocalDateTime createdAt;
}