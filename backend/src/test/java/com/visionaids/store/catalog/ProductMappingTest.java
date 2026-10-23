package com.visionaids.store.catalog;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMappingTest {
    @Test
    void selectsTheMainThumbnailIndependentlyAndKeepsOnlyRotationImagesInOrder() {
        ProductImage side = image("/side.jpg", false, true);
        ProductImage detail = image("/detail.jpg", false, false);
        ProductImage front = image("/front.jpg", true, false);
        ProductImage back = image("/back.jpg", false, true);
        Product product = new Product();
        ReflectionTestUtils.setField(product, "images", List.of(side, detail, front, back));

        ProductSummaryDto item = product.toSummary();
        assertThat(item.mainThumbnail()).isEqualTo(front.toDto());
        assertThat(item.rotatingThumbnails()).containsExactly(side.toDto(), back.toDto());

        ProductDetailDto full = product.toDetail();
        assertThat(full.mainThumbnail()).isEqualTo(item.mainThumbnail());
        assertThat(full.rotatingThumbnails()).isEqualTo(item.rotatingThumbnails());
        assertThat(full.images()).containsExactly(
                side.toDto(), detail.toDto(), front.toDto(), back.toDto());
        assertThat(full.images().getFirst().width()).isEqualTo(500);
        assertThat(full.images().getFirst().height()).isEqualTo(332);
    }

    @Test
    void includesTheMainThumbnailInRotationOnlyWhenSelected() {
        ProductImage main = image("/front.jpg", true, true);
        Product product = new Product();
        ReflectionTestUtils.setField(product, "images", List.of(main));
        assertThat(product.toSummary().rotatingThumbnails()).containsExactly(main.toDto());

        ReflectionTestUtils.setField(main, "rotatingThumbnail", false);
        assertThat(product.toSummary().mainThumbnail()).isEqualTo(main.toDto());
        assertThat(product.toSummary().rotatingThumbnails()).isEmpty();
    }

    @Test
    void handlesProductsWithoutImages() {
        Product product = new Product();
        assertThat(product.toSummary().mainThumbnail()).isNull();
        assertThat(product.toSummary().rotatingThumbnails()).isEmpty();
        assertThat(product.toDetail().mainThumbnail()).isNull();
        assertThat(product.toDetail().rotatingThumbnails()).isEmpty();
        assertThat(product.toDetail().images()).isEmpty();
    }

    private ProductImage image(String url, boolean main, boolean rotating) {
        ProductImage image = new ProductImage();
        ReflectionTestUtils.setField(image, "url", url);
        ReflectionTestUtils.setField(image, "altText", "Sample frame");
        ReflectionTestUtils.setField(image, "width", 500);
        ReflectionTestUtils.setField(image, "height", 332);
        ReflectionTestUtils.setField(image, "mainThumbnail", main);
        ReflectionTestUtils.setField(image, "rotatingThumbnail", rotating);
        return image;
    }
}
