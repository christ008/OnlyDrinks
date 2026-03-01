package uy.christian.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import uy.christian.inventory.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

            var product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
        }
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        log.info("Order persisted id={} items={} total={}", saved.getId(), saved.getItems().size(), saved.getTotalAmount());
        return saved;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MANAGER')")
    public Map<String, BigDecimal> getSalesByProduct() {
        log.debug("Calculating sales by product");
        return orderRepository.findAllWithDetails().stream()
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> Optional.ofNullable(item.getProduct())
                                .map(product -> product.getName())
                                .orElse("Unknown"),
                        Collectors.reducing(BigDecimal.ZERO, OrderItem::getSubtotal, BigDecimal::add)
                ));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MANAGER')")
    public BigDecimal getTotalRevenue() {
        log.debug("Calculating total revenue");
        return orderRepository.getTotalRevenue();
    }
}
