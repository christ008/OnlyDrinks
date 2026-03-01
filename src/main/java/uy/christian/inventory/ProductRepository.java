package uy.christian.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByDeletedAtIsNull();
    List<Product> findAllByDeletedAtIsNotNull();
}
