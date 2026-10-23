package com.visionaids.store.catalog;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final ProductRepository products;

    public CatalogService(ProductRepository products) {
        this.products = products;
    }

    public List<ProductSummaryDto> listProducts(String cat) {
        return products.findFiltered(cat, Sort.by("name")).stream().map(Product::toSummary).toList();
    }
}
