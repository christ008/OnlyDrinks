package uy.christian.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import uy.christian.inventory.Product;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class POSService {

    private final OrderService orderService;

    @PreAuthorize("hasRole('BARMAN')")
    public Order createNewOrder() {
        log.debug("Creating new in-memory order draft");
        return new Order();
    }

    @PreAuthorize("hasRole('BARMAN')")
    public void addItemToOrder(@NotNull Order order, @NotNull Product product, @Positive long quantity) {

        if (quantity <= 0) {
            log.warn("Rejected item add due to non-positive quantity={}", quantity);
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (product.getStockQuantity() < quantity) {
            log.warn("Rejected item add for productId={} due to insufficient stock. requested={} available={}",
                    product.getId(), quantity, product.getStockQuantity());
            throw new IllegalStateException("Not enough stock!");
        }

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        item.setOrder(order);

        order.getItems().add(item);
        calculateTotal(order);
        log.info("Item added to order productId={} quantity={}", product.getId(), quantity);
    }

    @PreAuthorize("hasRole('BARMAN')")
    public void removeItemFromOrder(@NotNull Order order, @NotNull OrderItem item) {
        order.getItems().remove(item);
        calculateTotal(order);
        log.info("Item removed from order productId={} quantity={}", item.getProduct().getId(), item.getQuantity());
    }

    public void calculateTotal(@NotNull Order order) {
        BigDecimal total = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
    }

    @PreAuthorize("hasRole('BARMAN')")
    public void placeOrder(@Valid @NotNull Order order) {
        if (order.getItems().isEmpty()) {
            log.warn("Rejected order placement because order has no items");
            throw new IllegalStateException("Order is empty");
        }
        orderService.placeOrder(order);
        log.info("Order placed with {} items and total={}", order.getItems().size(), order.getTotalAmount());
    }
}
