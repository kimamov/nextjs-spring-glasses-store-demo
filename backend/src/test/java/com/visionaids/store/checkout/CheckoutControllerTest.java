package com.visionaids.store.checkout;

import java.math.BigDecimal;
import java.util.HashMap;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CheckoutController.class)
class CheckoutControllerTest {
    private static final UUID PRODUCT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private CheckoutService checkout;
    private static final String TOKEN = "a".repeat(64);

    @Test
    void savesAuthenticatedCheckoutAndPreventsCaching() throws Exception {
        when(checkout.savePending(eq(TOKEN), any())).thenReturn(order());
        Map<String, Object> request = validRequest();
        request.put("total", 0.01);
        mvc.perform(post("/api/v1/checkout/orders").header("X-Checkout-Token", TOKEN)
                        .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.total").value(89.00))
                .andExpect(jsonPath("$.checkoutTokenHash").doesNotExist());
        verify(checkout).savePending(eq(TOKEN), any());
    }

    @Test
    void readsTheCurrentOrderOnlyThroughTheCheckoutIdentity() throws Exception {
        when(checkout.current(TOKEN, PRODUCT_ID)).thenReturn(order());
        mvc.perform(get("/api/v1/checkout/orders/current").param("productId", PRODUCT_ID.toString())
                        .header("X-Checkout-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.details.email").value("ada@example.com"));
        verify(checkout).current(TOKEN, PRODUCT_ID);
    }

    @Test
    void missingCheckoutIdentityIsUnauthorized() throws Exception {
        when(checkout.savePending(isNull(), any())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        mvc.perform(post("/api/v1/checkout/orders").contentType("application/json")
                        .content(json.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startsPaymentUsingOnlyTheOwnedProductAndReturnsAnUncachedLink() throws Exception {
        when(checkout.startPayment(TOKEN, PRODUCT_ID)).thenReturn("https://checkout.stripe.com/c/pay/cs_test_one");
        mvc.perform(post("/api/v1/checkout/sessions").param("productId", PRODUCT_ID.toString()).header("X-Checkout-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/c/pay/cs_test_one"));
        verify(checkout).startPayment(TOKEN, PRODUCT_ID);
    }

    @Test
    void returnsFieldErrorsForInvalidDetailsWithoutEchoingRejectedValues() throws Exception {
        Map<String, String> invalid = Map.of("firstName", " ", "lastName", "", "email", "not-an-email",
                "street", "", "postalCode", "1234", "city", "", "country", "US", "addressLine2", "x".repeat(201));
        for (var field : invalid.entrySet()) {
            Map<String, Object> request = validRequest();
            request.put(field.getKey(), field.getValue());
            mvc.perform(post("/api/v1/checkout/orders").contentType("application/json").content(json.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.errors." + field.getKey()).isString())
                    .andExpect(content().string(not(containsString("ada@example.com"))));
        }
        verifyNoInteractions(checkout);
    }

    @Test
    void rejectsMissingAndMalformedProductIds() throws Exception {
        for (Object id : new Object[] { null, "mitte" }) {
            Map<String, Object> request = validRequest();
            request.put("productId", id);
            mvc.perform(post("/api/v1/checkout/orders").contentType("application/json").content(json.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(checkout);
    }

    @Test
    void returnsNotFoundWhenTheSelectedProductWasRemoved() throws Exception {
        when(checkout.savePending(isNull(), any())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        mvc.perform(post("/api/v1/checkout/orders").contentType("application/json").content(json.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void keepsPersonalDetailsOutOfTheRequestStringUsedByFrameworkLogging() {
        CheckoutRequest request = new CheckoutRequest(PRODUCT_ID, "Ada", "Lovelace", "ada@example.com",
                "Example Street 1", "", "10115", "Berlin", "DE");
        assertThat(request.toString()).doesNotContain("Ada", "Lovelace", "ada@example.com", "Example Street", "Berlin");
    }

    private CheckoutOrderDto order() {
        return new CheckoutOrderDto(UUID.randomUUID(), "PENDING_PAYMENT", PRODUCT_ID, "Mitte", new BigDecimal("89.00"), "EUR",
                new CheckoutRequest(PRODUCT_ID, "Ada", "Lovelace", "ada@example.com", "Example Street 1", "", "10115", "Berlin", "DE"));
    }

    private Map<String, Object> validRequest() {
        return new HashMap<>(Map.of("productId", PRODUCT_ID.toString(), "firstName", "Ada", "lastName", "Lovelace",
                "email", "ada@example.com", "street", "Example Street 1", "addressLine2", "",
                "postalCode", "10115", "city", "Berlin", "country", "DE"));
    }
}
