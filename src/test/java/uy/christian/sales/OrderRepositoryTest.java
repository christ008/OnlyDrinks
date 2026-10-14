package uy.christian.sales;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the dashboard queries against the Flyway-managed H2 schema. The DB is seeded,
 * so assertions compare before/after deltas instead of absolute values.
 */
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void revenueAndCountSinceOnlyIncludeRecentOrders() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        BigDecimal revenueBefore = orderRepository.getRevenueSince(since);
        long countBefore = orderRepository.countByOrderDateGreaterThanEqual(since);

        Order recent = new Order();
        recent.setOrderDate(LocalDateTime.now());
        recent.setTotalAmount(new BigDecimal("42.50"));
        Order old = new Order();
        old.setOrderDate(since.minusDays(30));
        old.setTotalAmount(new BigDecimal("1000.00"));
        orderRepository.saveAll(java.util.List.of(recent, old));
        orderRepository.flush();

        assertEquals(0, revenueBefore.add(new BigDecimal("42.50")).compareTo(orderRepository.getRevenueSince(since)));
        assertEquals(countBefore + 1, orderRepository.countByOrderDateGreaterThanEqual(since));
    }
}
