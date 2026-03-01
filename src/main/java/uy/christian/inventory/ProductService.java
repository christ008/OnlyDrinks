package uy.christian.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('BARMAN')")
    public List<Product> findAll() {
        log.debug("Listing active products");
        return productRepository.findAllByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MANAGER')")
    public List<Product> findAllArchived() {
        log.debug("Listing archived products");
        return productRepository.findAllByDeletedAtIsNotNull();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('BARMAN')")
    public Optional<Product> findById(@NotNull Long id) {
        log.debug("Looking up product by id={}", id);
        return productRepository.findById(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    public Product save(@Valid @NotNull Product product) {
        Product saved = productRepository.save(product);
        log.info("Product saved id={} name={} stock={}", saved.getId(), saved.getName(), saved.getStockQuantity());
        return saved;
    }

    @PreAuthorize("hasRole('MANAGER')")
    public void archive(@NotNull Long id) {
        productRepository.deleteById(id);
        log.info("Product archived id={}", id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    public void restore(@NotNull Long id) {
        productRepository.findById(id).ifPresent(product -> {
            product.setDeletedAt(null);
            productRepository.save(product);
            log.info("Product restored id={}", id);
        });
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@NotNull Long id) {
        productRepository.deleteById(id);
        log.warn("Product hard-deleted id={}", id);
    }
}
