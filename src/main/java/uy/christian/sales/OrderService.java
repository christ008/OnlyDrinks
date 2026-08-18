package uy.christian.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import uy.christian.inventory.Product;
import uy.christian.inventory.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MANAGER')")
    public List<Order> findAll() {
        log.debug("Listing all orders with details");
        return orderRepository.findAllWithDetails();
    }

    @Transactional
    @PreAuthorize("hasRole('BARMAN')")
    public Order placeOrder(@Valid @NotNull Order order) {
        order.setOrderDate(LocalDateTime.now());
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            item.setOrder(order);
            item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            total = total.add(item.getSubtotal());

            // Re-fetch the managed entity so the @Version check guards the decrement
            // against concurrent orders reading the same stale, session-held stock.
            Product product = productRepository.findById(requireProductId(item))
                    .orElseThrow(() -> new IllegalStateException("Product no longer exists"));
            if (product.getStockQuantity() < item.getQuantity()) {
                log.warn("Rejected order placement for productId={} due to insufficient stock. requested={} available={}",
                        product.getId(), item.getQuantity(), product.getStockQuantity());
                throw new IllegalStateException("Not enough stock for " + product.getName());
            }
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            item.setProduct(product);
            productRepository.save(product);
        }
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        log.info("Order persisted id={} items={} total={}", saved.getId(), saved.getItems().size(), saved.getTotalAmount());
        return saved;
    }

    private static Long requireProductId(OrderItem item) {
        Product product = item.getProduct();
        if (product == null || product.getId() == null) {
            throw new IllegalStateException("Order item is missing a persisted product reference");
        }
        return product.getId();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MANAGER')")
    public Map<String, BigDecimal> getSalesByProduct() {
        log.debug("Calculating sales by product");
        return orderRepository.findSalesByProduct().stream()
                .collect(Collectors.toMap(
                        ProductSalesProjection::getProductName,
                        row -> Optional.ofNullable(row.getTotalSales()).orElse(BigDecimal.ZERO),
                        BigDecimal::add,
                        LinkedHashMap::new
                ));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MANAGER')")
    public BigDecimal getTotalRevenue() {
        log.debug("Calculating total revenue");
        return orderRepository.getTotalRevenue();
    }
}
