package com.visionaids.store.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id, String slug, String name, String brand, String type, String shape,
        String color, BigDecimal price, String currency, List<ProductImageResponse> images) {
}
