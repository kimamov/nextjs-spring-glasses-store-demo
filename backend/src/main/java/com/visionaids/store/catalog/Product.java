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

    ProductResponse toResponse() {
        return new ProductResponse(id, slug, name, brand, type, shape, color, price, currency,
                images.stream().map(ProductImage::toResponse).toList());
    }
}
