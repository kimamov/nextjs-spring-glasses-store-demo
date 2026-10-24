package com.visionaids.store.checkout;

import com.visionaids.store.catalog.ProductDetailDto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CheckoutOrderRepository extends JpaRepository<CheckoutOrder, UUID> {
    // REVIEW: Every read includes the secret's hash. Knowing an order/product ID
    // alone does not authorize reading the stored customer details.
    Optional<CheckoutOrder> findByCheckoutTokenHashAndProductId(String tokenHash, UUID productId);

    // REVIEW: PostgreSQL makes this one atomic operation. A concurrent retry waits
    // for the first insert and then updates that same row instead of creating another.
    // Only contact/address fields change on a retry; the price/name snapshot stays fixed.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO orders (id, checkout_token_hash, product_id, product_name, total, currency,
                first_name, last_name, email, street, address_line2, postal_code, city, country)
            VALUES (:id, :tokenHash, :#{#product.id()}, :#{#product.name()}, :#{#product.price()}, :#{#product.currency()},
                :#{#details.firstName()}, :#{#details.lastName()}, :#{#details.email()}, :#{#details.street()},
                :#{#details.addressLine2()}, :#{#details.postalCode()}, :#{#details.city()}, :#{#details.country()})
            ON CONFLICT (checkout_token_hash, product_id) DO UPDATE SET
                first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name,
                email = EXCLUDED.email, street = EXCLUDED.street, address_line2 = EXCLUDED.address_line2,
                postal_code = EXCLUDED.postal_code, city = EXCLUDED.city, country = EXCLUDED.country
            WHERE orders.status = 'PENDING_PAYMENT' AND orders.expires_at > CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int savePending(@Param("id") UUID id, @Param("tokenHash") String tokenHash,
            @Param("product") ProductDetailDto product, @Param("details") CheckoutRequest details);
}
