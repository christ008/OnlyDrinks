package uy.christian.sales.ui;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import uy.christian.inventory.Product;

record PosItemForm(
        @NotNull(message = "{validation.pos.product.required}") Product product,
        @NotNull(message = "{validation.pos.quantity.required}") @Positive(message = "{validation.pos.quantity.positive}") Integer quantity
) {
}
