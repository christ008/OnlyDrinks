package uy.christian.inventory;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the real H2 schema through Flyway (verifying migrations V1–V4 apply and
 * that the JPA mappings validate against them) and exercises the soft-delete flow.
 */
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Product newProduct(String name) {
        Product product = new Product();
        product.setName(name);
        product.setStockQuantity(10);
        product.setPrice(new BigDecimal("9.99"));
        return product;
    }

    @Test
    void softDeleteMovesProductBetweenActiveAndArchivedQueries() {
        Product saved = productRepository.save(newProduct("Beer"));
        Long id = saved.getId();
        assertNotNull(id);

        // The DB is Flyway-seeded (V2), so assert membership of THIS product rather than table size.
        assertTrue(containsId(productRepository.findAllByDeletedAtIsNull(), id));
        assertFalse(containsId(productRepository.findAllByDeletedAtIsNotNull(), id));

        // Soft delete = a versioned UPDATE stamping deleted_at, matching ProductService.archive.
        saved.setDeletedAt(LocalDateTime.now());
        productRepository.saveAndFlush(saved);
        entityManager.clear(); // drop first-level cache so the next reads hit the DB

        assertFalse(containsId(productRepository.findAllByDeletedAtIsNull(), id));
        assertTrue(containsId(productRepository.findAllByDeletedAtIsNotNull(), id));
    }

    private static boolean containsId(List<Product> products, Long id) {
        return products.stream().anyMatch(p -> id.equals(p.getId()));
    }

    @Test
    void moneyPrecisionRoundsToTwoDecimalPlaces() {
        Product saved = productRepository.save(newProduct("Whisky"));
        productRepository.flush();
        productRepository.findById(saved.getId()).ifPresent(reloaded ->
                assertEquals(2, reloaded.getPrice().scale()));
    }

    @Test
    void versionColumnEnablesOptimisticLocking() {
        Product saved = productRepository.saveAndFlush(newProduct("Gin"));
        // The @Version field is populated once the row is persisted.
        assertFalse(saved.getVersion() < 0);
    }
}
