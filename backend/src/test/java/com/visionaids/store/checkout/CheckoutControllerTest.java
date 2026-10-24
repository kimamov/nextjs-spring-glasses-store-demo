package com.visionaids.store.checkout;

import com.visionaids.store.catalog.CatalogService;
import com.visionaids.store.catalog.ProductDetailDto;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CheckoutController.class)
class CheckoutControllerTest {
    private static final UUID PRODUCT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private CatalogService catalog;

    @Test
    void validatesDetailsAndUsesTheCatalogPriceWithoutReturningPersonalData() throws Exception {
        when(catalog.getProduct(PRODUCT_ID)).thenReturn(new ProductDetailDto(PRODUCT_ID, "mitte", "Mitte",
                "Vision Aids", "eyeglasses", "Round", "Black", new BigDecimal("89.00"), "EUR",
                null, List.of(), List.of()));
        Map<String, Object> request = validRequest();
        request.put("total", 0.01);
        request.put("quantity", 99);
        mvc.perform(post("/api/v1/checkout/validate").contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.productName").value("Mitte"))
                .andExpect(jsonPath("$.total").value(89.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.street").doesNotExist());
        verify(catalog).getProduct(PRODUCT_ID);
    }

    @Test
    void returnsFieldErrorsForInvalidDetailsWithoutEchoingRejectedValues() throws Exception {
        Map<String, String> invalid = Map.of("firstName", " ", "lastName", "", "email", "not-an-email",
                "street", "", "postalCode", "1234", "city", "", "country", "US", "addressLine2", "x".repeat(201));
        for (var field : invalid.entrySet()) {
            Map<String, Object> request = validRequest();
            request.put(field.getKey(), field.getValue());
            mvc.perform(post("/api/v1/checkout/validate").contentType("application/json").content(json.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.errors." + field.getKey()).isString())
                    .andExpect(content().string(not(containsString("ada@example.com"))));
        }
        verifyNoInteractions(catalog);
    }

    @Test
    void rejectsMissingAndMalformedProductIds() throws Exception {
        for (Object id : new Object[] { null, "mitte" }) {
            Map<String, Object> request = validRequest();
            request.put("productId", id);
            mvc.perform(post("/api/v1/checkout/validate").contentType("application/json").content(json.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(catalog);
    }

    @Test
    void returnsNotFoundWhenTheSelectedProductWasRemoved() throws Exception {
        when(catalog.getProduct(PRODUCT_ID)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        mvc.perform(post("/api/v1/checkout/validate").contentType("application/json").content(json.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void keepsPersonalDetailsOutOfTheRequestStringUsedByFrameworkLogging() {
        CheckoutRequest request = new CheckoutRequest(PRODUCT_ID, "Ada", "Lovelace", "ada@example.com",
                "Example Street 1", "", "10115", "Berlin", "DE");
        assertThat(request.toString()).doesNotContain("Ada", "Lovelace", "ada@example.com", "Example Street", "Berlin");
    }

    private Map<String, Object> validRequest() {
        return new HashMap<>(Map.of("productId", PRODUCT_ID.toString(), "firstName", "Ada", "lastName", "Lovelace",
                "email", "ada@example.com", "street", "Example Street 1", "addressLine2", "",
                "postalCode", "10115", "city", "Berlin", "country", "DE"));
    }
}
