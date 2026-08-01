package com.codepilot.config;

import com.codepilot.dto.RegisterRequest;
import com.codepilot.repository.UserRepository;
import com.codepilot.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    public DataInitializer(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("No users found in database. Initializing default system developer account...");
            RegisterRequest defaultAdmin = RegisterRequest.builder()
                    .username("admin")
                    .email("admin@codepilot.ai")
                    .password("password123")
                    .firstName("Default")
                    .lastName("Admin")
                    .build();

            authService.register(defaultAdmin);
            log.info("Default user created! Username: 'admin' | Password: 'password123'");
        }
    }
}
