package uy.christian.security.ui;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import uy.christian.security.Role;

record UserForm(
        @NotBlank(message = "{validation.user.username.required}") String username,
        @NotBlank(message = "{validation.user.name.required}") String name,
        @NotNull(message = "{validation.user.role.required}") Role role,
        @Nullable String password
) {
}
