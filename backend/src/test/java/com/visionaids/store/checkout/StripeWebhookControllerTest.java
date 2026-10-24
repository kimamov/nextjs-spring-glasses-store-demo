package com.visionaids.store.checkout;

import com.stripe.model.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StripeWebhookController.class)
class StripeWebhookControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private StripeCheckout stripe;
    @MockitoBean private CheckoutService checkout;

    @Test
    void verifiesTheUnmodifiedBodyBeforeHandlingAnEvent() throws Exception {
        String body = "{ \"object\": \"event\" }\n";
        Event event = new Event();
        when(stripe.verify(body, "signed-header")).thenReturn(event);
        mvc.perform(post("/api/v1/stripe/webhook").contentType("application/json")
                        .header("Stripe-Signature", "signed-header").content(body))
                .andExpect(status().isOk());
        verify(stripe).verify(body, "signed-header");
        verify(checkout).acceptPayment(event);
    }

    @Test
    void invalidSignaturesCannotReachPaymentHandling() throws Exception {
        when(stripe.verify("{}", null)).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));
        mvc.perform(post("/api/v1/stripe/webhook").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(checkout);
    }
}
