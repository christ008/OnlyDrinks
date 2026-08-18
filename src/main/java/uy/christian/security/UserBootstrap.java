package uy.christian.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Seeds the initial user accounts. Kept out of the main application class so that
 * {@code Application} carries no service-layer constructor dependencies and test
 * slices (e.g. {@code @DataJpaTest}) can load without the full bean graph.
 */
@Configuration
class UserBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(UserBootstrap.class);

    private final UserService userService;

    UserBootstrap(UserService userService) {
        this.userService = userService;
    }

    @Bean
    @Profile("dev")
    CommandLineRunner loadDevData(UserRepository userRepository) {
        return _ -> {
            createUser(userRepository, "user", "Christian", "user", Role.USER);
            createUser(userRepository, "admin", "Admin", "admin", Role.ADMIN);
            createUser(userRepository, "barman", "Barman", "barman", Role.BARMAN);
            LOG.info("Development users bootstrap completed");
        };
    }

    @Bean
    @Profile("prod")
    CommandLineRunner ensureProductionAdmin(
            UserRepository userRepository,
            @Value("${app.bootstrap.admin.enabled:true}") boolean adminBootstrapEnabled,
            @Value("${app.bootstrap.admin.username:}") String adminUsername,
            @Value("${app.bootstrap.admin.name:}") String adminName,
            @Value("${app.bootstrap.admin.password:}") String adminPassword
    ) {
        return _ -> {
            if (!adminBootstrapEnabled) {
                LOG.info("Production admin bootstrap disabled");
                return;
            }
            if (adminUsername.isBlank() || adminName.isBlank() || adminPassword.isBlank()) {
                throw new IllegalStateException("Missing required production admin bootstrap properties: app.bootstrap.admin.username, app.bootstrap.admin.name, app.bootstrap.admin.password");
            }
            createUser(userRepository, adminUsername, adminName, adminPassword, Role.ADMIN);
            LOG.info("Production admin bootstrap verified for user '{}'", adminUsername);
        };
    }

    private void createUser(UserRepository userRepository, String username, String name, String password, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            LOG.debug("Bootstrap user '{}' already exists, skipping", username);
            return;
        }

        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setHashedPassword(userService.encodePassword(password));
        user.setRole(role);
        userRepository.save(user);
        LOG.info("Bootstrap user '{}' created with role {}", username, role);
    }
}
