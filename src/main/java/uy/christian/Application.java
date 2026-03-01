package uy.christian;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import uy.christian.security.Role;
import uy.christian.security.User;
import uy.christian.security.UserRepository;

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

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Profile("dev")
    public CommandLineRunner loadData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return _ -> {
            createUser(userRepository, passwordEncoder, "user", "Christian", "user", Role.USER);
            createUser(userRepository, passwordEncoder, "admin", "Admin", "admin", Role.ADMIN);
            createUser(userRepository, passwordEncoder, "barman", "Barman", "barman", Role.BARMAN);
        };
    }

    private void createUser(UserRepository userRepository, PasswordEncoder passwordEncoder, String username, String name, String password, Role role) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    if (user.getRole() == null) {
                        user.setRole(role);
                        userRepository.save(user);
                    }
                },
                () -> {
                    User user = new User();
                    user.setName(name);
                    user.setUsername(username);
                    user.setHashedPassword(passwordEncoder.encode(password));
                    user.setRole(role);
                    userRepository.save(user);
                }
        );
    }

}
