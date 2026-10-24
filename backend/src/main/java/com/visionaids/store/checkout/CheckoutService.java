package com.visionaids.store.checkout;

import com.visionaids.store.catalog.CatalogService;
import com.visionaids.store.catalog.ProductDetailDto;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutService {
    private final CheckoutOrderRepository orders;
    private final CatalogService catalog;
    private final StripeCheckout stripe;

    public CheckoutService(CheckoutOrderRepository orders, CatalogService catalog, StripeCheckout stripe) {
        this.orders = orders;
        this.catalog = catalog;
        this.stripe = stripe;
    }

    @Transactional
    public CheckoutOrderDto savePending(String token, CheckoutRequest details) {
        String tokenHash = hashToken(token);
        // REVIEW: The browser supplies a product ID, never the amount to charge.
        // The catalog provides the initial snapshot; retries preserve that snapshot.
        ProductDetailDto product = catalog.getProduct(details.productId());
        int changed = orders.savePending(UUID.randomUUID(), tokenHash, product, details);
        CheckoutOrder order = ownedOrder(tokenHash, details.productId());
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This checkout can no longer be changed.");
        }
        // REVIEW: The transaction commits before the controller sends a success.
        // If it rolls back, the browser receives an error and can safely retry.
        return order.toDto();
    }

    @Transactional(readOnly = true)
    public CheckoutOrderDto current(String token, UUID productId) {
        return ownedOrder(hashToken(token), productId).toDto();
    }

    @Transactional
    public String startPayment(String token, UUID productId) {
        CheckoutOrder order = orders.lockOwned(hashToken(token), productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No checkout found."));
        if (order.expired()) throw new ResponseStatusException(HttpStatus.GONE, "Checkout expired.");
        if (order.paid()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment already confirmed.");

        // shortcut: Stripe calls hold this one order's lock, split preparation and
        // finalization into separate transactions if checkout traffic warrants it.
        // REVIEW: The lock serializes concurrent starts/webhooks. Stripe's stable
        // idempotency key also protects the external request if our transaction fails.
        Session session = order.sessionId() == null ? null : stripe.retrieve(order.sessionId());
        if (session != null && "complete".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is being confirmed.");
        }
        if (session != null && "expired".equals(session.getStatus())) {
            order.nextAttempt();
            session = null;
        }
        if (session == null) {
            session = stripe.create(order.toDto(), order.attempt(), order.expiresAt());
            order.attachSession(session.getId());
        }
        if (!"open".equals(session.getStatus()) || session.getUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Checkout session is unavailable.");
        }
        return session.getUrl();
    }

    @Transactional
    public void acceptPayment(Event event) {
        if (!("checkout.session.completed".equals(event.getType())
                || "checkout.session.async_payment_succeeded".equals(event.getType()))) return;
        Object object = event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook API version does not match the Stripe SDK."));
        if (!(object instanceof Session session)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checkout session required.");
        if (session.getMetadata() == null || !session.getMetadata().containsKey("vision_aids_order_id")) return;
        if (!("paid".equals(session.getPaymentStatus()) || "no_payment_required".equals(session.getPaymentStatus()))) return;

        UUID id;
        try {
            id = UUID.fromString(session.getMetadata().get("vision_aids_order_id"));
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order reference.");
        }
        CheckoutOrder order = orders.lockById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown checkout order."));
        CheckoutOrderDto snapshot = order.toDto();
        // REVIEW: A signed event is genuine, but it must still belong to THIS order
        // and session. Check the amount/currency/mode before granting paid status.
        if (!Boolean.FALSE.equals(event.getLivemode()) || !Boolean.FALSE.equals(session.getLivemode())
                || !"payment".equals(session.getMode()) || !"complete".equals(session.getStatus()) || !id.toString().equals(session.getClientReferenceId())
                || !Integer.toString(order.attempt()).equals(session.getMetadata().get("attempt"))
                || session.getAmountTotal() == null || session.getAmountTotal() != StripeCheckout.cents(snapshot.total())
                || !snapshot.currency().equalsIgnoreCase(session.getCurrency())
                || ("no_payment_required".equals(session.getPaymentStatus()) && snapshot.total().signum() != 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment does not match the order.");
        }
        if (order.sessionId() == null) {
            // REVIEW: If a DB commit failed after Stripe created the session, ask
            // Stripe to retry delivery after our idempotent checkout retry attaches it.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Checkout session is not attached yet.");
        }
        if (!order.sessionId().equals(session.getId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong checkout session.");
        // REVIEW: We intentionally do not reject a late webhook just because guest
        // access expired. A real confirmed payment must still be recorded.
        order.markPaid();
    }

    private CheckoutOrder ownedOrder(String tokenHash, UUID productId) {
        CheckoutOrder order = orders.findByCheckoutTokenHashAndProductId(tokenHash, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending checkout found."));
        // REVIEW: Expiration is checked on the server too. Deleting a browser
        // cookie alone cannot invalidate a copied token that is replayed later.
        if (order.expired()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This checkout has expired. Start again.");
        }
        return order;
    }

    private String hashToken(String token) {
        if (token == null || !token.matches("[a-f0-9]{64}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A guest checkout cookie is required.");
        }
        try {
            // REVIEW: A random 256-bit token is already unguessable. SHA-256 lets
            // us match it without retaining the usable secret in the orders table.
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", error);
        }
    }
}
