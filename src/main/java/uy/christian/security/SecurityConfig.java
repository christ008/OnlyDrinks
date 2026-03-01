package uy.christian.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyAuthoritiesMapper;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import uy.christian.security.ui.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthenticationManager authManager) {
        return http.with(VaadinSecurityConfigurer.vaadin(), configurer ->
                        configurer.loginView(LoginView.class))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/actuator/health/**")).permitAll())
                .authenticationManager(authManager)
                .build();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsServiceImpl userDetailsService, PasswordEncoder passwordEncoder) {
        var roleHierarchy = RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("MANAGER")
                .role("MANAGER").implies("BARMAN")
                .role("BARMAN").implies("USER")
                .build();

        var roleHierarchyAuthoritiesMapper = new RoleHierarchyAuthoritiesMapper(roleHierarchy);

        var daoAuthProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthProvider.setPasswordEncoder(passwordEncoder);
        daoAuthProvider.setAuthoritiesMapper(roleHierarchyAuthoritiesMapper);
        return new ProviderManager(daoAuthProvider);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
