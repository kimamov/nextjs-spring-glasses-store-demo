package com.visionaids.store.checkout;

import com.stripe.StripeClient;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeCheckoutTest {
    private final CheckoutOrderDto order = new CheckoutOrderDto(UUID.randomUUID(), "PENDING_PAYMENT", UUID.randomUUID(),
            "Test frame", new BigDecimal("89.00"), "EUR",
            new CheckoutRequest(UUID.randomUUID(), "Ada", "Lovelace", "ada@example.com", "Example Street 1", "", "10115", "Berlin", "DE"));

    @Test
    void sdkSendsSnapshotAmountsAndStableKeysWithoutMutableCustomerData() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<String> keys = new ArrayList<>();
        List<String> bodies = new ArrayList<>();
        server.createContext("/v1/checkout/sessions", exchange -> {
            keys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"id":"cs_test_one","object":"checkout.session","livemode":false,"status":"open","url":"https://checkout.stripe.com/c/pay/cs_test_one"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            StripeClient client = StripeClient.builder().setApiKey("sk_test_fake")
                    .setApiBase("http://127.0.0.1:" + server.getAddress().getPort()).build();
            StripeCheckout stripe = new StripeCheckout(client, StripeTestEvents.SECRET, "http://localhost:3001");
            Instant expires = Instant.now().plusSeconds(7200);
            stripe.create(order, 0, expires);
            CheckoutOrderDto edited = new CheckoutOrderDto(order.orderId(), order.status(), order.productId(), order.productName(), order.total(), order.currency(),
                    new CheckoutRequest(order.productId(), "Grace", "Hopper", "grace@example.com", "Other Street 2", "", "10115", "Berlin", "DE"));
            stripe.create(edited, 0, expires);
            stripe.create(order, 1, expires);
            assertThat(keys.get(0)).isEqualTo(keys.get(1)).isNotEqualTo(keys.get(2));
            assertThat(bodies.get(0)).isEqualTo(bodies.get(1));
            String decoded = java.net.URLDecoder.decode(bodies.getFirst(), StandardCharsets.UTF_8);
            assertThat(decoded).contains("unit_amount]=8900", "quantity]=1", "currency]=eur", "allowed_payment_method_types[0]=card",
                    "http://localhost:3001/checkout/return", "http://localhost:3001/checkout/cancel")
                    .doesNotContain("ada@example.com", "Example Street", "customer_email");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void onlyAuthenticRecentSandboxWebhooksAreAccepted() {
        StripeCheckout stripe = new StripeCheckout((StripeClient) null, StripeTestEvents.SECRET, "http://localhost:3000");
        String body = StripeTestEvents.payload(order, Map.of());
        long now = Instant.now().getEpochSecond();
        String signature = StripeTestEvents.signature(body, now);
        assertThat(stripe.verify(body, signature).getType()).isEqualTo("checkout.session.completed");
        for (String invalid : new String[] { null, "invalid", StripeTestEvents.signature(body, now - 1000) }) {
            assertThatThrownBy(() -> stripe.verify(body, invalid)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        }
        assertThatThrownBy(() -> stripe.verify(body.replace("Test frame", "tampered") + " ", signature))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        String live = body.replace("\"livemode\":false", "\"livemode\":true");
        assertThatThrownBy(() -> stripe.verify(live, StripeTestEvents.signature(live, now)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }

    @Test
    void missingAndLiveKeysCannotStartPaymentsAndAmountsUseExactCents() {
        for (String key : new String[] { "", "sk_live_not_allowed" }) {
            StripeCheckout stripe = new StripeCheckout(key, StripeTestEvents.SECRET, "http://localhost:3000");
            assertThatThrownBy(() -> stripe.create(order, 0, Instant.now().plusSeconds(7200)))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
        }
        assertThat(StripeCheckout.cents(new BigDecimal("49.99"))).isEqualTo(4999);
        for (String value : new String[] { "1.001", "999999999999999999999" }) {
            assertThatThrownBy(() -> StripeCheckout.cents(new BigDecimal(value))).isInstanceOf(ResponseStatusException.class);
        }
    }
}
