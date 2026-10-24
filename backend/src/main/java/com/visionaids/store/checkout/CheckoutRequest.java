package com.visionaids.store.checkout;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CheckoutRequest(
        @NotNull(message = "Choose a frame.") UUID productId,
        @NotBlank(message = "Enter your first name.") @Size(max = 100) String firstName,
        @NotBlank(message = "Enter your last name.") @Size(max = 100) String lastName,
        @NotBlank(message = "Enter your email address.") @Email(message = "Enter a valid email address.")
        @Size(max = 254) String email,
        @NotBlank(message = "Enter your street and house number.") @Size(max = 200) String street,
        @Size(max = 200) String addressLine2,
        @NotBlank(message = "Enter your postal code.")
        @Pattern(regexp = "[0-9]{5}", message = "Enter a five-digit German postal code.") String postalCode,
        @NotBlank(message = "Enter your city.") @Size(max = 100) String city,
        @NotBlank(message = "Choose a country.")
        @Pattern(regexp = "DE", message = "Delivery is currently available only in Germany.") String country) {
    @Override
    public String toString() {
        return "CheckoutRequest[productId=" + productId + "]";
    }
}
