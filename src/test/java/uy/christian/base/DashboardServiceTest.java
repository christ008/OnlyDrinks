package uy.christian.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uy.christian.base.DashboardService.DashboardStats;
import uy.christian.inventory.Product;
import uy.christian.inventory.ProductRepository;
import uy.christian.sales.OrderRepository;
import uy.christian.sales.ProductSalesProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dashboardService = new DashboardService(orderRepository, productRepository);
    }

    private static ProductSalesProjection sales(String name, String total) {
        return new ProductSalesProjection() {
            @Override
            public String getProductName() {
                return name;
            }

            @Override
            public BigDecimal getTotalSales() {
                return new BigDecimal(total);
            }
        };
    }

    @Test
    void loadStatsAggregatesRepositoryResults() {
        Product low = new Product();
        low.setName("Gin");
        low.setStockQuantity(2);

        when(orderRepository.getRevenueSince(any(LocalDateTime.class))).thenReturn(new BigDecimal("125.50"));
        when(orderRepository.countByOrderDateGreaterThanEqual(any(LocalDateTime.class))).thenReturn(7L);
        when(productRepository.findActiveWithStockAtOrBelow(DashboardService.LOW_STOCK_THRESHOLD)).thenReturn(List.of(low));
        when(orderRepository.findSalesByProduct()).thenReturn(List.of(sales("Beer", "300"), sales("Gin", "120")));

        DashboardStats stats = dashboardService.loadStats();

        assertEquals(new BigDecimal("125.50"), stats.revenueToday());
        assertEquals(7L, stats.ordersToday());
        assertEquals(List.of(low), stats.lowStockProducts());
        assertIterableEquals(List.of("Beer", "Gin"), stats.topProducts().keySet());
    }

    @Test
    void topProductsAreLimitedToFive() {
        when(orderRepository.getRevenueSince(any(LocalDateTime.class))).thenReturn(BigDecimal.ZERO);
        when(orderRepository.findSalesByProduct()).thenReturn(List.of(
                sales("A", "6"), sales("B", "5"), sales("C", "4"), sales("D", "3"), sales("E", "2"), sales("F", "1")));

        DashboardStats stats = dashboardService.loadStats();

        assertEquals(5, stats.topProducts().size());
        assertIterableEquals(List.of("A", "B", "C", "D", "E"), stats.topProducts().keySet());
    }
}
