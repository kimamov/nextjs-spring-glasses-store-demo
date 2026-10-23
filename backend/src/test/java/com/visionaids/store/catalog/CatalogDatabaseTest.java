package com.visionaids.store.catalog;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@EnabledIfEnvironmentVariable(named = "CATALOG_DB_TESTS", matches = "true")
class CatalogDatabaseTest {
    @Autowired
    private EntityManager entities;

    @Autowired
    private CatalogService catalog;

    private static final List<String> BRANDS = List.of("Filter test A", "Filter test B", "Filter test C");

    @BeforeEach
    void insertTransactionalFixtures() {
        product("A", "eyeglasses", BRANDS.get(0), "Filter test Round", "49.99");
        product("B", "eyeglasses", BRANDS.get(1), "Filter test Oval", "80.00");
        product("C", "sunglasses", BRANDS.get(0), "Filter test Round", "80.00");
        product("D", "eyeglasses", BRANDS.get(2), "Filter test Square", "120.00");
        entities.flush();
        entities.clear();
    }

    @Test
    void databaseCombinesGroupsWithAndAndCheckboxSelectionsWithOr() {
        assertThat(catalog.listProducts("eyeglasses", BRANDS.subList(0, 2),
                List.of("Filter test Round", "Filter test Oval"), new BigDecimal("49.99"), new BigDecimal("80")))
                .extracting(ProductSummaryDto::name).containsExactly("A", "B");
    }

    @Test
    void databaseAppliesInclusiveAndOptionalPriceBounds() {
        assertThat(catalog.listProducts(null, BRANDS, List.of(), new BigDecimal("80"), new BigDecimal("80")))
                .extracting(ProductSummaryDto::name).containsExactly("B", "C");
        assertThat(catalog.listProducts(null, BRANDS, List.of(), null, new BigDecimal("49.99")))
                .extracting(ProductSummaryDto::name).containsExactly("A");
        assertThat(catalog.listProducts(null, BRANDS, List.of(), new BigDecimal("120"), null))
                .extracting(ProductSummaryDto::name).containsExactly("D");
        assertThat(catalog.listProducts(null, BRANDS, List.of(), null, BigDecimal.ZERO)).isEmpty();
    }

    @Test
    void rejectsUnavailableChoicesAndReversedPriceRanges() {
        assertThatThrownBy(() -> catalog.listProducts("unknown", List.of(), List.of(), null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        assertThatThrownBy(() -> catalog.listProducts(null, List.of("unknown"), List.of(), null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        assertThatThrownBy(() -> catalog.listProducts(null, List.of(), List.of("unknown"), null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        assertThatThrownBy(() -> catalog.listProducts(null, List.of(), List.of(), BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }

    @Test
    void optionsComeFromTheDatabaseAndRemainAvailableAfterEmptyResults() {
        assertThat(catalog.listProducts(null, BRANDS, List.of(), new BigDecimal("1000"), null)).isEmpty();
        ProductFilterOptionsDto options = catalog.filterOptions();
        assertThat(options.brands()).containsAll(BRANDS).isSorted().doesNotHaveDuplicates();
        assertThat(options.shapes()).contains("Filter test Round", "Filter test Oval", "Filter test Square");
        assertThat(options.types()).extracting(ProductFilterOptionsDto.TypeOption::value).contains("eyeglasses", "sunglasses");
    }

    private void product(String name, String type, String brand, String shape, String price) {
        Product product = new Product();
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "slug", "filter-test-" + id);
        ReflectionTestUtils.setField(product, "name", name);
        ReflectionTestUtils.setField(product, "type", type);
        ReflectionTestUtils.setField(product, "brand", brand);
        ReflectionTestUtils.setField(product, "shape", shape);
        ReflectionTestUtils.setField(product, "color", "Black");
        ReflectionTestUtils.setField(product, "price", new BigDecimal(price));
        ReflectionTestUtils.setField(product, "currency", "EUR");
        entities.persist(product);
    }
}
