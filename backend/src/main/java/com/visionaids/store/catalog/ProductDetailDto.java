package com.visionaids.store.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailDto(
        UUID id, String slug, String name, String brand, String type, String shape,
        String color, BigDecimal price, String currency,
        ProductImageDto mainThumbnail, List<ProductImageDto> rotatingThumbnails,
        List<ProductImageDto> images) {
}
