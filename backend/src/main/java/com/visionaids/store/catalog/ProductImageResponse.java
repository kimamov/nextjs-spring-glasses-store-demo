package com.visionaids.store.catalog;

public record ProductImageResponse(
        String url, String altText, int width, int height,
        boolean mainThumbnail, boolean rotatingThumbnail) {
}
