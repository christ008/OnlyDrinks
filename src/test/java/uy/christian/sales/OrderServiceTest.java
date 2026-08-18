package uy.christian.sales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uy.christian.inventory.Product;
import uy.christian.inventory.ProductRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, productRepository);
    }

    private static Product product(long id, long stock, double price) {
        Product p = new Product();
        p.setId(id);
        p.setName("Beer");
        p.setStockQuantity(stock);
        p.setPrice(BigDecimal.valueOf(price));
        return p;
    }

    private static Order orderWith(Product product, long quantity) {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.getItems().add(item);
        return order;
    }

    @Test
    void placeOrderDecrementsFreshlyFetchedStock() {
        Product managed = product(1L, 10, 5.0);
        Order order = orderWith(managed, 3);

        when(productRepository.findById(1L)).thenReturn(Optional.of(managed));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order saved = orderService.placeOrder(order);

        assertEquals(7, managed.getStockQuantity());
        assertEquals(0, BigDecimal.valueOf(15.0).compareTo(saved.getTotalAmount()));
        verify(productRepository).save(managed);
    }

    @Test
    void placeOrderRejectsOversellAgainstStaleSessionStock() {
        // The item was built in the UI when stock looked plentiful...
        Product staleReference = product(1L, 99, 5.0);
        Order order = orderWith(staleReference, 3);

        // ...but the freshly fetched row shows the real, insufficient stock.
        Product dbState = product(1L, 1, 5.0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(dbState));

        assertThrows(IllegalStateException.class, () -> orderService.placeOrder(order));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
