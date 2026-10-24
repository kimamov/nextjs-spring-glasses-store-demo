package com.visionaids.store.checkout;

import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.model.Event;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.ObjectMapper;

final class StripeTestEvents {
    static final String SECRET = "whsec_test_signature_secret";

    static String payload(CheckoutOrderDto order, Map<String, Object> overrides) {
        Map<String, Object> session = new HashMap<>(Map.of(
                "object", "checkout.session", "id", "cs_test_one", "livemode", false,
                "mode", "payment", "status", "complete", "payment_status", "paid",
                "amount_total", StripeCheckout.cents(order.total()), "currency", "eur",
                "client_reference_id", order.orderId().toString(),
                "metadata", Map.of("vision_aids_order_id", order.orderId().toString(), "attempt", "0")));
        session.putAll(overrides);
        return new ObjectMapper().writeValueAsString(Map.of("id", "evt_test", "object", "event",
                "api_version", Stripe.API_VERSION, "livemode", false, "type", "checkout.session.completed",
                "data", Map.of("object", session)));
    }

    static String signature(String payload, long timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    static Event event(CheckoutOrderDto order, Map<String, Object> overrides) {
        String body = payload(order, overrides);
        return new StripeCheckout((StripeClient) null, SECRET, "http://localhost:3000")
                .verify(body, signature(body, Instant.now().getEpochSecond()));
    }
}
