package uy.christian.base;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.christian.inventory.Product;
import uy.christian.inventory.ProductRepository;
import uy.christian.sales.OrderRepository;
import uy.christian.sales.ProductSalesProjection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates the headline numbers shown on the dashboard. Everything is computed in SQL.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardService {

    public static final long LOW_STOCK_THRESHOLD = 5;
    private static final int TOP_PRODUCTS_LIMIT = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public record DashboardStats(
            BigDecimal revenueToday,
            long ordersToday,
            List<Product> lowStockProducts,
            Map<String, BigDecimal> topProducts) {
    }

    @PreAuthorize("hasRole('MANAGER')")
    public DashboardStats loadStats() {
        log.debug("Loading dashboard statistics");
        var startOfToday = LocalDate.now().atStartOfDay();

        Map<String, BigDecimal> topProducts = new LinkedHashMap<>();
        orderRepository.findSalesByProduct().stream()
                .limit(TOP_PRODUCTS_LIMIT)
                .forEach((ProductSalesProjection row) -> topProducts.put(row.getProductName(), row.getTotalSales()));

        return new DashboardStats(
                orderRepository.getRevenueSince(startOfToday),
                orderRepository.countByOrderDateGreaterThanEqual(startOfToday),
                productRepository.findActiveWithStockAtOrBelow(LOW_STOCK_THRESHOLD),
                topProducts);
    }
}
