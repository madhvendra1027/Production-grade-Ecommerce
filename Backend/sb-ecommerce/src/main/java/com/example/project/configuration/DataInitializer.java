package com.example.project.configuration;

import com.example.project.model.AppRole;
import com.example.project.model.Role;
import com.example.project.repositories.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Ensures the roles table contains a row for every AppRole.
 *
 * This has to run unconditionally: registration resolves ROLE_USER from the
 * database, so a missing role row breaks signup entirely. It creates no user
 * accounts - that is what made the old initData runner dangerous.
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Order(1)
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            for (AppRole appRole : AppRole.values()) {
                roleRepository.findByRoleName(appRole)
                        .orElseGet(() -> {
                            logger.info("Bootstrapping missing role {}", appRole);
                            return roleRepository.save(new Role(appRole));
                        });
            }
        };
    }
}
