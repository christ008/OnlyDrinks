package uy.christian.sales;

import java.math.BigDecimal;

/**
 * Read-only projection for aggregated sales grouped by product name.
 */
public interface ProductSalesProjection {
    String getProductName();

    BigDecimal getTotalSales();
}
