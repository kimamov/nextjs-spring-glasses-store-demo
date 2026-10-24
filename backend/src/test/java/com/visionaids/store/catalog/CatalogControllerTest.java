package com.visionaids.store.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogService catalog;

    @Test
    void exposesProductDetailsByIdIncludingTheCompleteGallery() throws Exception {
        UUID id = UUID.fromString("10000000-0000-0000-0000-000000000001");
        ProductImageDto front = new ProductImageDto("/front.jpg", "Front view", 1200, 800);
        ProductImageDto detail = new ProductImageDto("/detail.jpg", "Frame detail", 500, 332);
        when(catalog.getProduct(id)).thenReturn(new ProductDetailDto(id, "mitte", "Mitte", "Vision Aids",
                "eyeglasses", "Round", "Black", new BigDecimal("89.00"), "EUR",
                front, List.of(front), List.of(front, detail)));

        mvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Mitte"))
                .andExpect(jsonPath("$.price").value(89.00))
                .andExpect(jsonPath("$.mainThumbnail.url").value("/front.jpg"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[1].url").value("/detail.jpg"))
                .andExpect(jsonPath("$.images[1].altText").value("Frame detail"))
                .andExpect(jsonPath("$.images[1].width").value(500));
        verify(catalog).getProduct(id);
    }

    @Test
    void returnsNotFoundForUnknownProductIds() throws Exception {
        UUID id = UUID.randomUUID();
        when(catalog.getProduct(id)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        mvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found."));
    }

    @Test
    void rejectsSlugsAndMalformedProductIds() throws Exception {
        mvc.perform(get("/api/v1/products/mitte")).andExpect(status().isBadRequest());
    }

    @Test
    void exposesTheCatalogAsJsonWithPriceAndCurrency() throws Exception {
        when(catalog.listProducts(null, List.of(), List.of(), null, null)).thenReturn(List.of(new ProductSummaryDto(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "mitte", "Mitte", "Vision Aids", "eyeglasses", "Round", "Tortoiseshell",
                new BigDecimal("89.00"), "EUR",
                new ProductImageDto("/images/front.jpg", "Front view", 1200, 800),
                List.of(new ProductImageDto("/images/side.jpg", "Side view", 500, 332)))));

        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].slug").value("mitte"))
                .andExpect(jsonPath("$[0].price").value(89.00))
                .andExpect(jsonPath("$[0].currency").value("EUR"))
                .andExpect(jsonPath("$[0].mainThumbnail.url").value("/images/front.jpg"))
                .andExpect(jsonPath("$[0].mainThumbnail.altText").value("Front view"))
                .andExpect(jsonPath("$[0].mainThumbnail.width").value(1200))
                .andExpect(jsonPath("$[0].mainThumbnail.height").value(800))
                .andExpect(jsonPath("$[0].rotatingThumbnails.length()").value(1))
                .andExpect(jsonPath("$[0].rotatingThumbnails[0].url").value("/images/side.jpg"))
                .andExpect(jsonPath("$[0].rotatingThumbnails[0].width").value(500))
                .andExpect(jsonPath("$[0].rotatingThumbnails[0].height").value(332))
                .andExpect(jsonPath("$[0].images").doesNotExist())
                .andExpect(jsonPath("$[0].mainThumbnail.mainThumbnail").doesNotExist())
                .andExpect(jsonPath("$[0].rotatingThumbnails[0].rotatingThumbnail").doesNotExist());
    }

    @Test
    void exposesNullMainThumbnailAndEmptyRotationForProductsWithoutImages() throws Exception {
        when(catalog.listProducts(null, List.of(), List.of(), null, null)).thenReturn(List.of(new ProductSummaryDto(
                UUID.randomUUID(), "kreuzberg", "Kreuzberg", "Vision Aids", "eyeglasses",
                "Square", "Black", new BigDecimal("109.00"), "EUR", null, List.of())));

        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(org.hamcrest.Matchers.hasKey("mainThumbnail")))
                .andExpect(jsonPath("$[0].mainThumbnail").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[0].rotatingThumbnails").isEmpty());
    }

    @Test
    void returnsAnEmptyArrayWhenTheCatalogIsEmpty() throws Exception {
        when(catalog.listProducts(null, List.of(), List.of(), null, null)).thenReturn(List.of());
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void forwardsCombinedFiltersIncludingCommaSeparatedAndRepeatedSelections() throws Exception {
        mvc.perform(get("/api/v1/products")
                .param("cat", "sunglasses")
                .param("brand", "Northline,Studio Frame")
                .param("shape", "Round", "Oval")
                .param("minPrice", "49.99")
                .param("maxPrice", "100"))
                .andExpect(status().isOk());
        verify(catalog).listProducts("sunglasses", List.of("Northline", "Studio Frame"),
                List.of("Round", "Oval"), new BigDecimal("49.99"), new BigDecimal("100"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "-1", "NaN", "Infinity", "12oops", "1.234", "1e2", "" })
    void rejectsInvalidPricesWithAnActionableProblemDetail(String price) throws Exception {
        for (String parameter : List.of("minPrice", "maxPrice")) {
            mvc.perform(get("/api/v1/products").param(parameter, price))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Prices must be non-negative")));
        }
    }

    @Test
    void exposesFilterOptionsIndependentlyOfProductSelections() throws Exception {
        when(catalog.filterOptions()).thenReturn(new ProductFilterOptionsDto(
                List.of(new ProductFilterOptionsDto.TypeOption("eyeglasses", "Eyeglasses")),
                List.of("Northline"), List.of("Round", "Oval")));
        mvc.perform(get("/api/v1/products/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types[0].label").value("Eyeglasses"))
                .andExpect(jsonPath("$.brands[0]").value("Northline"))
                .andExpect(jsonPath("$.shapes.length()").value(2));
    }
}
