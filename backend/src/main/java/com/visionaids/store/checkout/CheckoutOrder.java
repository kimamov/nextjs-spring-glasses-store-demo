package com.visionaids.store.checkout;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
class CheckoutOrder {
    @Id
    private UUID id;
    private String checkoutTokenHash;
    private UUID productId;
    private String productName;
    private BigDecimal total;
    private String currency;
    private String status;
    private String firstName;
    private String lastName;
    private String email;
    private String street;
    private String addressLine2;
    private String postalCode;
    private String city;
    private String country;
    private Instant createdAt;
    private Instant expiresAt;

    protected CheckoutOrder() {
    }

    boolean expired() {
        return !expiresAt.isAfter(Instant.now());
    }

    CheckoutOrderDto toDto() {
        // REVIEW: The token hash is intentionally absent from the HTTP response.
        // Address details are returned only after the service checks ownership.
        return new CheckoutOrderDto(id, status, productId, productName, total, currency,
                new CheckoutRequest(productId, firstName, lastName, email, street,
                        addressLine2, postalCode, city, country));
    }
}
