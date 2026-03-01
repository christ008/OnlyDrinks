package uy.christian;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uy.christian.security.Role;
import uy.christian.security.User;
import uy.christian.security.UserRepository;
import uy.christian.security.UserService;

@SpringBootApplication
@EnableJpaAuditing
@PWA(
    name = "OnlyDrinks POS",
    shortName = "OnlyDrinks",
    description = "Point of Sale system for beverage management",
    backgroundColor = "#ffffff",
    themeColor = "#1976D2",
    offlinePath = "offline.html"
)
@StyleSheet(Lumo.STYLESHEET) // Use Aura.STYLESHEET to use Aura instead
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css") // Your custom styles
public class Application implements AppShellConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private final UserService userService;

    public Application(UserService userService) {
        this.userService = userService;
    }

    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Profile("dev")
    public CommandLineRunner loadDevData(UserRepository userRepository) {
        return _ -> {
            createUser(userRepository, "user", "Christian", "user", Role.USER);
            createUser(userRepository, "admin", "Admin", "admin", Role.ADMIN);
            createUser(userRepository, "barman", "Barman", "barman", Role.BARMAN);
            LOG.info("Development users bootstrap completed");
        };
    }

    @Bean
    @Profile("prod")
    public CommandLineRunner ensureProductionAdmin(
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
