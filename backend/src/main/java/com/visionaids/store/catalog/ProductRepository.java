package com.visionaids.store.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductRepository extends JpaRepository<Product, UUID> {
    @EntityGraph(attributePaths = "images")
    @Query("SELECT p FROM Product p WHERE (:type IS NULL OR p.type = :type)")
    List<Product> findFiltered(@Param("type") String type, Sort sort);
}
