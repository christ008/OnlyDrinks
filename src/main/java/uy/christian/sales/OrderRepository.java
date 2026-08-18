package uy.christian.sales;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            select distinct o
            from Order o
            left join fetch o.customer
            left join fetch o.items i
            left join fetch i.product
            order by o.orderDate desc
            """)
    List<Order> findAllWithDetails();

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o")
    BigDecimal getTotalRevenue();

    @Query("""
            select coalesce(p.name, 'Unknown') as productName,
                   sum(i.subtotal) as totalSales
            from OrderItem i
            left join i.product p
            group by p.name
            order by sum(i.subtotal) desc
            """)
    List<ProductSalesProjection> findSalesByProduct();
}
