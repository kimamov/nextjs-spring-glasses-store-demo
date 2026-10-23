package com.visionaids.store.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogService catalog;

    @Test
    void exposesTheCatalogAsJsonWithPriceAndCurrency() throws Exception {
        when(catalog.listProducts(null)).thenReturn(List.of(new ProductSummaryDto(
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
        when(catalog.listProducts(null)).thenReturn(List.of(new ProductSummaryDto(
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
        when(catalog.listProducts(null)).thenReturn(List.of());
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
