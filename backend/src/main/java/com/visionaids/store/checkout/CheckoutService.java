package com.visionaids.store.checkout;

import com.visionaids.store.catalog.CatalogService;
import com.visionaids.store.catalog.ProductDetailDto;
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

    public CheckoutService(CheckoutOrderRepository orders, CatalogService catalog) {
        this.orders = orders;
        this.catalog = catalog;
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
