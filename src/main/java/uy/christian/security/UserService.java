package uy.christian.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findAll() {
        log.debug("Listing all users");
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<User> findById(@NotNull Long id) {
        log.debug("Looking up user by id={}", id);
        return userRepository.findById(id);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User save(@Valid @NotNull User user) {
        User saved = userRepository.save(user);
        log.info("User saved id={} username={} role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return saved;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@NotNull Long id) {
        userRepository.deleteById(id);
        log.info("User deleted id={}", id);
    }

    public String encodePassword(@NotBlank String rawPassword) {
        String encoded = Objects.requireNonNull(passwordEncoder.encode(rawPassword));
        log.debug("Password encoded");
        return encoded;
    }
}
