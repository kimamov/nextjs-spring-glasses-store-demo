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

    ProductImageResponse toResponse() {
        return new ProductImageResponse(url, altText, width, height, mainThumbnail, rotatingThumbnail);
    }
}
