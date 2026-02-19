package com.academy.config;

import com.academy.entity.User;
import com.academy.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSetup {

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if admin already exists
            if (userRepository.findByUsername("nada@maher").isEmpty()) {

                User admin = new User();
                admin.setUsername("nada@maher");
                admin.setPortalId("a7f3b2c9-4d1e-4a8f-9b2c-3e5f6a7b8c9d");
                admin.setFullName("ندا ماهر");  // or your actual name
                admin.setPassword(passwordEncoder.encode("nada@maher@1234"));  // ← change this password

                userRepository.save(admin);

                System.out.println("✅ Admin account created: username=admin, password=admin123");
            }
        };
    }
}