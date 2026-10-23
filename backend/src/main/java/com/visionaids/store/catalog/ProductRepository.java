package com.visionaids.store.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    @Override
    @EntityGraph(attributePaths = "images")
    List<Product> findAll(Specification<Product> filters, Sort sort);

    @Query("SELECT DISTINCT p.brand FROM Product p ORDER BY p.brand")
    List<String> findBrands();

    @Query("SELECT DISTINCT p.shape FROM Product p ORDER BY p.shape")
    List<String> findShapes();
}
