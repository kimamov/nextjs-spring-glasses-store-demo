package com.visionaids.store.catalog;

import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/products")
public class CatalogController {
    private static final Set<String> PRODUCT_TYPES = Set.of(
            "eyeglasses", "sunglasses", "reading-glasses", "sports-glasses");

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<ProductResponse> listProducts(@RequestParam(required = false) String cat) {
        if (cat != null && !PRODUCT_TYPES.contains(cat)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported product type");
        }
        return catalog.listProducts(cat);
    }
}
