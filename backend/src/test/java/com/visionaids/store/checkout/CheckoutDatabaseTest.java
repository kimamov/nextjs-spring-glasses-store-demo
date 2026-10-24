package com.visionaids.store.checkout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.stripe.model.checkout.Session;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "CATALOG_DB_TESTS", matches = "true")
class CheckoutDatabaseTest {
    @Autowired private CheckoutService checkout;
    @Autowired private JdbcTemplate sql;
    @MockitoBean private StripeCheckout stripe;
    private UUID productId;
    private final String token = "a".repeat(64);

    @BeforeEach
    void insertPrivateFixture() {
        productId = UUID.randomUUID();
        sql.update("""
                INSERT INTO products (id, slug, name, brand, type, shape, color, price, currency)
                VALUES (?, ?, 'Checkout test frame', 'Test brand', 'eyeglasses', 'Round', 'Black', 89, 'EUR')
                """, productId, "checkout-test-" + productId);
    }

    @AfterEach
    void removeOnlyThisTestsData() {
        // REVIEW: Concurrent requests need real commits, so this class cannot rely
        // on a rollback around each test. Cleanup is scoped to its random product.
        sql.update("DELETE FROM orders WHERE product_id = ?", productId);
        sql.update("DELETE FROM products WHERE id = ?", productId);
    }

    @Test
    void persistsAnOrderWithAPriceSnapshotAndOnlyAHashOfTheGuestSecret() {
        CheckoutOrderDto order = checkout.savePending(token, details("Berlin"));
        assertThat(order.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.total()).isEqualByComparingTo("89.00");
        assertThat(checkout.current(token, productId).details().email()).isEqualTo("ada@example.com");
        String stored = sql.queryForObject("SELECT checkout_token_hash FROM orders WHERE id = ?", String.class, order.orderId());
        assertThat(stored).hasSize(64).isNotEqualTo(token);
    }

    @Test
    void retriesUpdateTheSameOrderWithoutRepricingItsSnapshot() {
        CheckoutOrderDto first = checkout.savePending(token, details("Berlin"));
        sql.update("UPDATE products SET price = 199, name = 'Renamed frame' WHERE id = ?", productId);
        CheckoutOrderDto retry = checkout.savePending(token, details("Hamburg"));
        assertThat(retry.orderId()).isEqualTo(first.orderId());
        assertThat(retry.total()).isEqualByComparingTo(new BigDecimal("89.00"));
        assertThat(retry.productName()).isEqualTo("Checkout test frame");
        assertThat(retry.details().city()).isEqualTo("Hamburg");
        // REVIEW: A different guest starts a different order and gets today's price.
        assertThat(checkout.savePending("b".repeat(64), details("Bonn")).total()).isEqualByComparingTo("199.00");
    }

    @Test
    void concurrentSubmissionsCreateOnlyOneOrder() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(4)) {
            List<CompletableFuture<CheckoutOrderDto>> requests = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                requests.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(error);
                    }
                    return checkout.savePending(token, details("Berlin"));
                }, workers));
            }
            start.countDown();
            List<UUID> ids = new ArrayList<>();
            for (var request : requests) ids.add(request.get().orderId());
            assertThat(ids).containsOnly(ids.getFirst());
            assertThat(sql.queryForObject("SELECT count(*) FROM orders WHERE product_id = ?", Integer.class, productId)).isEqualTo(1);
        }
    }

    @Test
    void anotherGuestCannotReadTheAddressAndMissingTokensAreUnauthorized() {
        checkout.savePending(token, details("Berlin"));
        assertThatThrownBy(() -> checkout.current("b".repeat(64), productId))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        for (String invalid : new String[] { null, "", "guess" }) {
            assertThatThrownBy(() -> checkout.current(invalid, productId))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("401");
            assertThatThrownBy(() -> checkout.savePending(invalid, details("Berlin")))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("401");
        }
    }

    @Test
    void expiredTokensCannotReadOrModifyStoredAddresses() {
        checkout.savePending(token, details("Berlin"));
        sql.update("UPDATE orders SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE product_id = ?", productId);
        assertThatThrownBy(() -> checkout.current(token, productId))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("410");
        assertThatThrownBy(() -> checkout.savePending(token, details("Hamburg")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("410");
        assertThat(sql.queryForObject("SELECT city FROM orders WHERE product_id = ?", String.class, productId)).isEqualTo("Berlin");
    }

    @Test
    void unavailableProductsDoNotCreateOrders() {
        CheckoutRequest missing = new CheckoutRequest(UUID.randomUUID(), "Ada", "Lovelace", "ada@example.com",
                "Example Street 1", "", "10115", "Berlin", "DE");
        assertThatThrownBy(() -> checkout.savePending(token, missing))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        assertThat(sql.queryForObject("SELECT count(*) FROM orders WHERE product_id = ?", Integer.class, productId)).isZero();
    }

    @Test
    void simultaneousPaymentStartsReuseOneStripeSession() throws Exception {
        checkout.savePending(token, details("Berlin"));
        Session open = session("cs_test_one", "open");
        when(stripe.create(any(), eq(0), any())).thenReturn(open);
        when(stripe.retrieve("cs_test_one")).thenReturn(open);
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(3)) {
            List<CompletableFuture<String>> requests = new ArrayList<>();
            for (int i = 0; i < 3; i++) requests.add(CompletableFuture.supplyAsync(() -> {
                try { start.await(); } catch (InterruptedException error) { throw new RuntimeException(error); }
                return checkout.startPayment(token, productId);
            }, workers));
            start.countDown();
            for (var request : requests) assertThat(request.get(10, TimeUnit.SECONDS)).isEqualTo(open.getUrl());
        }
        verify(stripe, times(1)).create(any(), eq(0), any());
        assertThatThrownBy(() -> checkout.startPayment("b".repeat(64), productId)).hasMessageContaining("404");
    }

    @Test
    void failedCreationCanRetryAndExpiredSessionsGetANewAttempt() {
        checkout.savePending(token, details("Berlin"));
        when(stripe.create(any(), eq(0), any())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY))
                .thenReturn(session("cs_test_one", "open"));
        assertThatThrownBy(() -> checkout.startPayment(token, productId)).hasMessageContaining("502");
        checkout.startPayment(token, productId);
        when(stripe.retrieve("cs_test_one")).thenReturn(session("cs_test_one", "expired"));
        when(stripe.create(any(), eq(1), any())).thenReturn(session("cs_test_two", "open"));
        assertThat(checkout.startPayment(token, productId)).endsWith("cs_test_two");
        assertThat(sql.queryForObject("SELECT stripe_attempt FROM orders WHERE product_id = ?", Integer.class, productId)).isEqualTo(1);
    }

    @Test
    void onlyConfirmedMatchingPaymentsCanMarkAnOrderPaidAndDuplicatesDoNothing() {
        CheckoutOrderDto order = checkout.savePending(token, details("Berlin"));
        when(stripe.create(any(), eq(0), any())).thenReturn(session("cs_test_one", "open"));
        checkout.startPayment(token, productId);
        checkout.acceptPayment(StripeTestEvents.event(order, Map.of("payment_status", "unpaid")));
        assertThat(checkout.current(token, productId).status()).isEqualTo("PENDING_PAYMENT");
        for (Map<String, Object> wrong : List.<Map<String, Object>>of(Map.of("amount_total", 1), Map.of("currency", "usd"),
                Map.of("id", "cs_other"), Map.of("mode", "subscription"), Map.of("livemode", true),
                Map.of("client_reference_id", UUID.randomUUID().toString()), Map.of("status", "open"))) {
            assertThatThrownBy(() -> checkout.acceptPayment(StripeTestEvents.event(order, wrong))).isInstanceOf(ResponseStatusException.class);
        }
        assertThat(checkout.current(token, productId).status()).isEqualTo("PENDING_PAYMENT");
        checkout.acceptPayment(StripeTestEvents.event(order, Map.of()));
        Instant paidAt = sql.queryForObject("SELECT paid_at FROM orders WHERE id = ?", java.sql.Timestamp.class, order.orderId()).toInstant();
        checkout.acceptPayment(StripeTestEvents.event(order, Map.of()));
        assertThat(sql.queryForObject("SELECT paid_at FROM orders WHERE id = ?", java.sql.Timestamp.class, order.orderId()).toInstant()).isEqualTo(paidAt);
        assertThat(checkout.current(token, productId).status()).isEqualTo("PAID");
        assertThatThrownBy(() -> checkout.savePending(token, details("Hamburg"))).hasMessageContaining("409");
        assertThatThrownBy(() -> checkout.startPayment(token, productId)).hasMessageContaining("409");
    }

    @Test
    void browserCompletionIsNotPaymentProofAndLateWebhooksStillRecordPayments() {
        CheckoutOrderDto order = checkout.savePending(token, details("Berlin"));
        when(stripe.create(any(), eq(0), any())).thenReturn(session("cs_test_one", "open"));
        checkout.startPayment(token, productId);
        when(stripe.retrieve("cs_test_one")).thenReturn(session("cs_test_one", "complete"));
        assertThatThrownBy(() -> checkout.startPayment(token, productId)).hasMessageContaining("409");
        assertThat(checkout.current(token, productId).status()).isEqualTo("PENDING_PAYMENT");
        sql.update("UPDATE orders SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE id = ?", order.orderId());
        checkout.acceptPayment(StripeTestEvents.event(order, Map.of()));
        assertThat(sql.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, order.orderId())).isEqualTo("PAID");
    }

    @Test
    void noPaymentRequiredIsAcceptedOnlyForAnActuallyFreeOrder() {
        sql.update("UPDATE products SET price = 0 WHERE id = ?", productId);
        CheckoutOrderDto order = checkout.savePending(token, details("Berlin"));
        when(stripe.create(any(), eq(0), any())).thenReturn(session("cs_test_one", "open"));
        checkout.startPayment(token, productId);
        checkout.acceptPayment(StripeTestEvents.event(order, Map.of("payment_status", "no_payment_required")));
        assertThat(checkout.current(token, productId).status()).isEqualTo("PAID");
    }

    private Session session(String id, String status) {
        Session session = new Session();
        session.setId(id);
        session.setStatus(status);
        session.setUrl("https://checkout.stripe.com/c/pay/" + id);
        session.setLivemode(false);
        return session;
    }

    private CheckoutRequest details(String city) {
        return new CheckoutRequest(productId, "Ada", "Lovelace", "ada@example.com", "Example Street 1", "", "10115", city, "DE");
    }
}
