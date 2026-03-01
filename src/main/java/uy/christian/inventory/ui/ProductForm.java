package uy.christian.inventory.ui;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

record ProductForm(
        @NotBlank(message = "{validation.product.name.required}") String name,
        @Nullable String description,
        @NotNull(message = "{validation.product.stock.required}") @Min(value = 0, message = "{validation.product.stock.min}") Long stockQuantity,
        @NotNull(message = "{validation.product.price.required}") @DecimalMin(value = "0.00", message = "{validation.product.price.min}") BigDecimal price
) {
}
