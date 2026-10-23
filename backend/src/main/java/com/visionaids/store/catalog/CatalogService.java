package com.visionaids.store.catalog;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private static final List<ProductFilterOptionsDto.TypeOption> PRODUCT_TYPES = List.of(
            new ProductFilterOptionsDto.TypeOption("eyeglasses", "Eyeglasses"),
            new ProductFilterOptionsDto.TypeOption("sunglasses", "Sunglasses"),
            new ProductFilterOptionsDto.TypeOption("reading-glasses", "Reading glasses"),
            new ProductFilterOptionsDto.TypeOption("sports-glasses", "Sports glasses"));

    private final ProductRepository products;

    public CatalogService(ProductRepository products) {
        this.products = products;
    }

    public ProductFilterOptionsDto filterOptions() {
        return new ProductFilterOptionsDto(PRODUCT_TYPES, products.findBrands(), products.findShapes());
    }

    public List<ProductSummaryDto> listProducts(String cat, List<String> brands, List<String> shapes,
            BigDecimal minPrice, BigDecimal maxPrice) {
        if (cat != null && PRODUCT_TYPES.stream().noneMatch(type -> type.value().equals(cat))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This glasses type is not supported.");
        }
        if ((!brands.isEmpty() && !products.findBrands().containsAll(brands))
                || (!shapes.isEmpty() && !products.findShapes().containsAll(shapes))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Some selected brands or frame shapes are unavailable. Clear the filters to start again.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum price must not exceed maximum price.");
        }
        Specification<Product> filters = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (cat != null) predicates.add(builder.equal(root.get("type"), cat));
            if (!brands.isEmpty()) predicates.add(root.get("brand").in(brands));
            if (!shapes.isEmpty()) predicates.add(root.get("shape").in(shapes));
            if (minPrice != null) predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return products.findAll(filters, Sort.by("name")).stream().map(Product::toSummary).toList();
    }
}
