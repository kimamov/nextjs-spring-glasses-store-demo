package com.visionaids.store.catalog;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/products")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<ProductSummaryDto> listProducts(
            @RequestParam(required = false) String cat,
            @RequestParam(defaultValue = "") List<String> brand,
            @RequestParam(defaultValue = "") List<String> shape,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice) {
        return catalog.listProducts(cat, brand, shape, parsePrice(minPrice), parsePrice(maxPrice));
    }

    @GetMapping("/filters")
    public ProductFilterOptionsDto filterOptions() {
        return catalog.filterOptions();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail invalidFilters(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }

    private BigDecimal parsePrice(String value) {
        if (value == null) return null;
        if (!value.matches("(?:\\d+(?:\\.\\d{1,2})?|\\.\\d{1,2})")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Some filters in the URL are invalid. Prices must be non-negative numbers with at most two decimal places.");
        }
        return new BigDecimal(value);
    }
}
