package com.visionaids.store.checkout;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {
    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @PostMapping("/orders")
    public ResponseEntity<CheckoutOrderDto> savePending(@Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Checkout-Token", required = false) String token) {
        // REVIEW: Next.js reads the HttpOnly cookie and forwards it server-to-server.
        // The response can contain an address, so it must never enter a shared cache.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(checkout.savePending(token, request));
    }

    @GetMapping("/orders/current")
    public ResponseEntity<CheckoutOrderDto> current(@RequestParam UUID productId,
            @RequestHeader(value = "X-Checkout-Token", required = false) String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(checkout.current(token, productId));
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionLink> startPayment(@RequestParam UUID productId,
            @RequestHeader(value = "X-Checkout-Token", required = false) String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new SessionLink(checkout.startPayment(token, productId)));
    }

    public record SessionLink(String url) {
        // REVIEW: The hosted URL is a bearer link. Keep it out of debug response logs.
        @Override
        public String toString() { return "SessionLink[redacted]"; }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidDetails(MethodArgumentNotValidException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field ->
                fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Check your checkout details.");
        problem.setProperty("errors", fields);
        return problem;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail requestFailed(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }
}
