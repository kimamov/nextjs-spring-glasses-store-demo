package com.visionaids.store.checkout;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StripeWebhookController {
    private final StripeCheckout stripe;
    private final CheckoutService checkout;

    public StripeWebhookController(StripeCheckout stripe, CheckoutService checkout) {
        this.stripe = stripe;
        this.checkout = checkout;
    }

    @PostMapping("/api/v1/stripe/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        checkout.acceptPayment(stripe.verify(payload, signature));
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail requestFailed(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }
}
