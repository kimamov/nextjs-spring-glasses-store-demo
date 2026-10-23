package com.visionaids.store.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
class ProductImage {
    @Column(nullable = false)
    private int position;
    @Column(nullable = false)
    private String url;
    @Column(nullable = false)
    private String altText;
    @Column(nullable = false)
    private int width;
    @Column(nullable = false)
    private int height;
    @Column(nullable = false)
    private boolean mainThumbnail;
    @Column(nullable = false)
    private boolean rotatingThumbnail;

    protected ProductImage() {
    }

    boolean isMainThumbnail() {
        return mainThumbnail;
    }

    boolean isRotatingThumbnail() {
        return rotatingThumbnail;
    }

    ProductImageDto toDto() {
        return new ProductImageDto(url, altText, width, height);
    }
}
