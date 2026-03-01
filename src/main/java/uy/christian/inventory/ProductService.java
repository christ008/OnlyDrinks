package uy.christian.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAllByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public List<Product> findAllArchived() {
        return productRepository.findAllByDeletedAtIsNotNull();
    }

    @Transactional(readOnly = true)
    public Optional<Product> findById(@NotNull Long id) {
        return productRepository.findById(id);
    }

    public Product save(@Valid @NotNull Product product) {
        return productRepository.save(product);
    }

    public void archive(@NotNull Long id) {
        productRepository.deleteById(id);
    }

    public void restore(@NotNull Long id) {
        productRepository.findById(id).ifPresent(product -> {
            product.setDeletedAt(null);
            productRepository.save(product);
        });
    }

    @Transactional
    public void delete(@NotNull Long id) {
        productRepository.deleteById(id);
    }
}
