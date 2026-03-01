package uy.christian.sales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uy.christian.inventory.Product;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class POSServiceTest {

    private POSService posService;

    @Mock
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        posService = new POSService(orderService);
    }

    @Test
    void testCreateNewOrder() {
        Order order = posService.createNewOrder();
        assertNotNull(order);
        assertNotNull(order.getItems());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void testAddItemToOrder() {
        Order order = posService.createNewOrder();
        Product product = new Product();
        product.setName("Beer");
        product.setPrice(BigDecimal.valueOf(10.0));
        product.setStockQuantity(5);

        posService.addItemToOrder(order, product, 2);

        assertEquals(1, order.getItems().size());
        assertEquals(0, BigDecimal.valueOf(20.0).compareTo(order.getTotalAmount()));
        assertEquals(2, order.getItems().get(0).getQuantity());
        assertEquals(product, order.getItems().get(0).getProduct());
    }

    @Test
    void testAddItemToOrderInsufficientStock() {
        Order order = posService.createNewOrder();
        Product product = new Product();
        product.setName("Beer");
        product.setPrice(BigDecimal.valueOf(10.0));
        product.setStockQuantity(1);

        assertThrows(IllegalStateException.class, () -> posService.addItemToOrder(order, product, 2));
    }

    @Test
    void testAddItemToOrderWithNullOrder() {
        Product product = new Product();
        product.setName("Beer");
        product.setPrice(BigDecimal.valueOf(10.0));
        product.setStockQuantity(5);
        // Currently this throws NPE because order is used without check
        assertThrows(NullPointerException.class, () -> posService.addItemToOrder(null, product, 1));
    }

    @Test
    void testAddItemToOrderWithNullProduct() {
        Order order = posService.createNewOrder();
        assertThrows(NullPointerException.class, () -> posService.addItemToOrder(order, null, 1));
    }

    @Test
    void testAddItemToOrderWithZeroQuantity() {
        Order order = posService.createNewOrder();
        Product product = new Product();
        product.setName("Beer");
        product.setPrice(BigDecimal.valueOf(10.0));
        product.setStockQuantity(5);
        assertThrows(IllegalArgumentException.class, () -> posService.addItemToOrder(order, product, 0));
    }

    @Test
    void testRemoveItemFromOrder() {
        Order order = posService.createNewOrder();
        Product product = new Product();
        product.setName("Beer");
        product.setPrice(BigDecimal.valueOf(10.0));
        product.setStockQuantity(5);

        posService.addItemToOrder(order, product, 2);
        OrderItem item = order.getItems().get(0);
        
        posService.removeItemFromOrder(order, item);

        assertTrue(order.getItems().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getTotalAmount()));
    }

    @Test
    void testPlaceOrder() {
        Order order = posService.createNewOrder();
        Product product = new Product();
        product.setName("Beer");
        product.setPrice(BigDecimal.valueOf(10.0));
        product.setStockQuantity(5);
        posService.addItemToOrder(order, product, 1);

        posService.placeOrder(order);

        verify(orderService, times(1)).placeOrder(order);
    }

    @Test
    void testPlaceEmptyOrder() {
        Order order = posService.createNewOrder();
        assertThrows(IllegalStateException.class, () -> posService.placeOrder(order));
    }
}
