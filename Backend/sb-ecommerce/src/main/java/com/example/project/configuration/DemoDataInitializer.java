package com.example.project.configuration;

import com.example.project.model.AppRole;
import com.example.project.model.Role;
import com.example.project.model.User;
import com.example.project.repositories.RoleRepository;
import com.example.project.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;

/**
 * Optional local-development account seeding.
 *
 * Replaces the old initData runner, which hardcoded admin/adminPass,
 * seller1/password2 and user1/password1 and recreated them on every single boot -
 * including in production. This version:
 *
 *   1. does nothing at all unless app.seed-demo-users=true, and
 *   2. refuses to create an account whose password was not supplied via the
 *      environment, so there is no credential in the repository to leak.
 *
 * To bootstrap your first administrator locally:
 *   export APP_ADMIN_PASSWORD='something-long-and-random'
 *   export APP_SEED_DEMO_USERS=true
 *
 * For a real deployment, prefer creating one admin by hand and then using
 * PUT /api/admin/users/{userId}/roles to grant further roles.
 */
@Configuration
@ConditionalOnProperty(name = "app.seed-demo-users", havingValue = "true")
public class DemoDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataInitializer.class);

    @Value("${app.demo.admin-password:}")
    private String adminPassword;

    @Value("${app.demo.seller-password:}")
    private String sellerPassword;

    @Value("${app.demo.user-password:}")
    private String userPassword;

    @Bean
    @Order(2)
    public CommandLineRunner seedDemoUsers(RoleRepository roleRepository,
                                           UserRepository userRepository,
                                           PasswordEncoder passwordEncoder) {
        return args -> {
            logger.warn("app.seed-demo-users=true - seeding demo accounts. "
                    + "Never enable this in a production environment.");

            Role userRole = requireRole(roleRepository, AppRole.ROLE_USER);
            Role sellerRole = requireRole(roleRepository, AppRole.ROLE_SELLER);
            Role adminRole = requireRole(roleRepository, AppRole.ROLE_ADMIN);

            seed(userRepository, passwordEncoder, "user1", "user1@example.com",
                    userPassword, Set.of(userRole));

            seed(userRepository, passwordEncoder, "seller1", "seller1@example.com",
                    sellerPassword, Set.of(userRole, sellerRole));

            seed(userRepository, passwordEncoder, "admin", "admin@example.com",
                    adminPassword, Set.of(userRole, sellerRole, adminRole));
        };
    }

    private Role requireRole(RoleRepository roleRepository, AppRole appRole) {
        return roleRepository.findByRoleName(appRole)
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + appRole + " missing; DataInitializer should have created it."));
    }

    /**
     * Creates the account only when a password was provided through configuration.
     * Existing accounts are left completely alone - the previous implementation
     * reset the role set of these three usernames on every restart, which would
     * quietly undo any legitimate role change an admin had made.
     */
    private void seed(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      String username,
                      String email,
                      String rawPassword,
                      Set<Role> roles) {

        if (!StringUtils.hasText(rawPassword)) {
            logger.warn("Skipping demo account '{}': no password configured "
                    + "(set app.demo.{}-password).", username, username.replaceAll("\\d", ""));
            return;
        }

        Optional<User> existing = userRepository.findByUserName(username);
        if (existing.isPresent()) {
            logger.info("Demo account '{}' already exists, leaving it untouched.", username);
            return;
        }

        User user = new User(username, email, passwordEncoder.encode(rawPassword));
        user.setRoles(roles);
        userRepository.save(user);
        logger.info("Created demo account '{}'.", username);
    }
}
