package uy.christian.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByDeletedAtIsNull();
    List<Product> findAllByDeletedAtIsNotNull();

    @Query("""
            select p from Product p
            where p.deletedAt is null and p.stockQuantity <= :threshold
            order by p.stockQuantity asc, p.name asc
            """)
    List<Product> findActiveWithStockAtOrBelow(@Param("threshold") long threshold);
}
