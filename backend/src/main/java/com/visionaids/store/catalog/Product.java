package com.visionaids.store.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
class Product {
    @Id
    private UUID id;
    private String slug;
    private String name;
    private String brand;
    private String type;
    private String shape;
    private String color;
    private BigDecimal price;
    private String currency;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderBy("position ASC")
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
    }

    ProductSummaryDto toSummary() {
        return new ProductSummaryDto(id, slug, name, brand, type, shape, color, price, currency,
                mainThumbnail(), rotatingThumbnails());
    }

    ProductDetailDto toDetail() {
        return new ProductDetailDto(id, slug, name, brand, type, shape, color, price, currency,
                mainThumbnail(), rotatingThumbnails(),
                images.stream().map(ProductImage::toDto).toList());
    }

    private ProductImageDto mainThumbnail() {
        return images.stream().filter(ProductImage::isMainThumbnail)
                .findFirst().map(ProductImage::toDto).orElse(null);
    }

    private List<ProductImageDto> rotatingThumbnails() {
        return images.stream().filter(ProductImage::isRotatingThumbnail)
                .map(ProductImage::toDto).toList();
    }
}
