package uy.christian.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@RolesAllowed("ADMIN")
@Transactional
@Validated
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(@NotNull Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User save(@Valid @NotNull User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void delete(@NotNull Long id) {
        userRepository.deleteById(id);
    }

    public String encodePassword(@NotBlank String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
