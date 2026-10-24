package com.visionaids.store.checkout;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutOrderDto(UUID orderId, String status, UUID productId, String productName,
        BigDecimal total, String currency, CheckoutRequest details) {
}
