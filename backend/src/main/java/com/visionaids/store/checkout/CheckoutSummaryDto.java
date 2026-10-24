package com.visionaids.store.checkout;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutSummaryDto(UUID productId, String productName, BigDecimal total, String currency) {
}
