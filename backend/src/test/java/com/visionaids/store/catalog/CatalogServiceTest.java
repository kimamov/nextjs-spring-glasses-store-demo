package com.visionaids.store.catalog;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogServiceTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final CatalogService catalog = new CatalogService(products);

    @Test
    void findsProductsByIdIncludingThoseWithoutImages() {
        UUID id = UUID.randomUUID();
        when(products.findById(id)).thenReturn(Optional.of(new Product()));
        ProductDetailDto product = catalog.getProduct(id);
        assertThat(product.images()).isEmpty();
        assertThat(product.mainThumbnail()).isNull();
    }

    @Test
    void reportsMissingProductsAsNotFound() {
        UUID id = UUID.randomUUID();
        when(products.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> catalog.getProduct(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
