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
        when(catalog.listProducts(null)).thenReturn(List.of(new ProductResponse(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "mitte", "Mitte", "Vision Aids", "eyeglasses", "Round", "Tortoiseshell",
                new BigDecimal("89.00"), "EUR", List.of(
                        new ProductImageResponse("/images/side.jpg", "Side view", 500, 332, false, true),
                        new ProductImageResponse("/images/front.jpg", "Front view", 1200, 800, true, false)))));

        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].slug").value("mitte"))
                .andExpect(jsonPath("$[0].price").value(89.00))
                .andExpect(jsonPath("$[0].currency").value("EUR"))
                .andExpect(jsonPath("$[0].images.length()").value(2))
                .andExpect(jsonPath("$[0].images[0].url").value("/images/side.jpg"))
                .andExpect(jsonPath("$[0].images[0].altText").value("Side view"))
                .andExpect(jsonPath("$[0].images[0].width").value(500))
                .andExpect(jsonPath("$[0].images[0].height").value(332))
                .andExpect(jsonPath("$[0].images[0].mainThumbnail").value(false))
                .andExpect(jsonPath("$[0].images[0].rotatingThumbnail").value(true))
                .andExpect(jsonPath("$[0].images[1].mainThumbnail").value(true))
                .andExpect(jsonPath("$[0].images[1].width").value(1200))
                .andExpect(jsonPath("$[0].images[1].height").value(800))
                .andExpect(jsonPath("$[0].images[1].rotatingThumbnail").value(false));
    }

    @Test
    void exposesAnEmptyImageListForProductsWithoutImages() throws Exception {
        when(catalog.listProducts(null)).thenReturn(List.of(new ProductResponse(
                UUID.randomUUID(), "kreuzberg", "Kreuzberg", "Vision Aids", "eyeglasses",
                "Square", "Black", new BigDecimal("109.00"), "EUR", List.of())));

        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].images").isEmpty());
    }

    @Test
    void returnsAnEmptyArrayWhenTheCatalogIsEmpty() throws Exception {
        when(catalog.listProducts(null)).thenReturn(List.of());
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
