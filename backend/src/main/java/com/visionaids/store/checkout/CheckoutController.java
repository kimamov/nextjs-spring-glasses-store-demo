package com.visionaids.store.checkout;

import com.visionaids.store.catalog.CatalogService;
import com.visionaids.store.catalog.ProductDetailDto;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {
    private final CatalogService catalog;

    public CheckoutController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @PostMapping("/validate")
    public CheckoutSummaryDto validate(@Valid @RequestBody CheckoutRequest request) {
        ProductDetailDto product = catalog.getProduct(request.productId());
        return new CheckoutSummaryDto(product.id(), product.name(), product.price(), product.currency());
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
