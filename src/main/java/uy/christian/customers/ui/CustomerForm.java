package uy.christian.customers.ui;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

record CustomerForm(
        @NotBlank(message = "{validation.customer.name.required}") String name,
        @Nullable @Email(message = "{validation.customer.email.invalid}") String email,
        @Nullable String phone,
        @Nullable String address
) {
}
