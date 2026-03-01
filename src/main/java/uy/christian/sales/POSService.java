package uy.christian.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import uy.christian.inventory.Product;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Validated
public class POSService {

    private final OrderService orderService;

    public Order createNewOrder() {
        return new Order();
    }

    public void addItemToOrder(@NotNull Order order, @NotNull Product product, @Positive long quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (product.getStockQuantity() < quantity) {
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
    }

    public void removeItemFromOrder(@NotNull Order order, @NotNull OrderItem item) {
        order.getItems().remove(item);
        calculateTotal(order);
    }

    public void calculateTotal(@NotNull Order order) {
        BigDecimal total = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
    }

    public void placeOrder(@Valid @NotNull Order order) {
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Order is empty");
        }
        orderService.placeOrder(order);
    }
}
