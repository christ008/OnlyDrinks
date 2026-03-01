package uy.christian.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.jspecify.annotations.Nullable;
import uy.christian.base.AbstractAuditEntity;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(unique = true)
    @NotBlank(message = "Username is required")
    private String username = "";

    @NotBlank(message = "Name is required")
    private String name = "";

    @JsonIgnore
    @NotNull(message = "Password is required")
    private String hashedPassword = "";

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Role is required")
    private Role role;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
