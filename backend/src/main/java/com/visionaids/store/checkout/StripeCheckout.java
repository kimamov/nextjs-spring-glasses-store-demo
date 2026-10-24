package com.visionaids.store.checkout;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StripeCheckout {
    private final StripeClient client;
    private final String webhookSecret;
    private final String frontendUrl;

    @Autowired
    public StripeCheckout(@Value("${checkout.stripe.secret-key:}") String key,
            @Value("${checkout.stripe.webhook-secret:}") String webhookSecret,
            @Value("${checkout.frontend-url:http://localhost:3000}") String frontendUrl) {
        // REVIEW: This demo deliberately refuses live keys. Missing configuration
        // disables payment requests without preventing catalog/database startup.
        this(key.startsWith("sk_test_")
                ? StripeClient.builder().setApiKey(key).setConnectTimeout(3000).setReadTimeout(5000).setMaxNetworkRetries(1).build()
                : null, webhookSecret, frontendUrl);
    }

    StripeCheckout(StripeClient client, String webhookSecret, String frontendUrl) {
        this.client = client;
        this.webhookSecret = webhookSecret;
        URI origin = URI.create(frontendUrl);
        if (!("http".equals(origin.getScheme()) || "https".equals(origin.getScheme()))
                || origin.getHost() == null || origin.getRawQuery() != null || origin.getRawFragment() != null
                || origin.getUserInfo() != null || !(origin.getPath().isEmpty() || "/".equals(origin.getPath()))) {
            throw new IllegalArgumentException("CHECKOUT_FRONTEND_URL must be an HTTP(S) origin");
        }
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    Session create(CheckoutOrderDto order, int attempt, Instant expiresAt) {
        requireConfigured();
        if (!"EUR".equals(order.currency())) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "This demo supports EUR payments only.");
        if (expiresAt.getEpochSecond() < Instant.now().getEpochSecond() + 1800) {
            throw new ResponseStatusException(HttpStatus.GONE, "This checkout is too close to expiry. Start again.");
        }
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addAllowedPaymentMethodType(SessionCreateParams.AllowedPaymentMethodType.CARD)
                .setClientReferenceId(order.orderId().toString())
                .putMetadata("vision_aids_order_id", order.orderId().toString())
                .putMetadata("attempt", Integer.toString(attempt))
                .setExpiresAt(expiresAt.getEpochSecond())
                .setSuccessUrl(frontendUrl + "/checkout/return?productId=" + order.productId())
                .setCancelUrl(frontendUrl + "/checkout/cancel?productId=" + order.productId())
                .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(order.currency().toLowerCase(Locale.ROOT)).setUnitAmount(cents(order.total()))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(order.productName()).build())
                                .build()).build())
                .build();
        // REVIEW: Only immutable snapshots go into this request. Mutable contact
        // details would change the payload and break retries using the same key.
        // Stripe collects its billing email; our saved delivery details stay in Java.
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("checkout-" + order.orderId() + "-" + attempt).build();
        try {
            return sandbox(client.v1().checkout().sessions().create(params, options));
        } catch (StripeException error) {
            // REVIEW: Stripe errors can include customer/request data. Do not expose
            // or log the raw exception through the public checkout response.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment is temporarily unavailable. Please retry.");
        }
    }

    Session retrieve(String id) {
        requireConfigured();
        try {
            return sandbox(client.v1().checkout().sessions().retrieve(id));
        } catch (StripeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment is temporarily unavailable. Please retry.");
        }
    }

    public Event verify(String payload, String signature) {
        if (webhookSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe webhook is not configured.");
        if (signature == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe signature.");
        try {
            // REVIEW: Verification uses the exact raw body and timestamp. Parsing
            // and re-serializing JSON before this step invalidates the signature.
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            if (!Boolean.FALSE.equals(event.getLivemode())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sandbox events only.");
            return event;
        } catch (SignatureVerificationException | RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook.");
        }
    }

    static long cents(BigDecimal amount) {
        try {
            // REVIEW: BigDecimal avoids binary rounding; exact conversion rejects
            // fractional cents or amounts too large for Stripe's integer field.
            return amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException error) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid payment amount.");
        }
    }

    private void requireConfigured() {
        if (client == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe sandbox is not configured.");
        }
    }

    private Session sandbox(Session session) {
        if (!Boolean.FALSE.equals(session.getLivemode())) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sandbox session required.");
        return session;
    }
}
