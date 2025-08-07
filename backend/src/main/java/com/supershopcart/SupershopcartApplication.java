package com.supershopcart;

import com.supershopcart.seeder.ShopCartSeeder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class SupershopcartApplication {
    public static void main(String[] args) {
        // A Spring Boot application will automatically use the active profile
        // set by environment variables (e.g., SPRING_PROFILES_ACTIVE) or
        // command-line arguments (e.g., --spring.profiles.active=dev-emulator).
        // We remove the hardcoded setting here to maintain flexibility.

        // Running the application
        ConfigurableApplicationContext context = SpringApplication.run(SupershopcartApplication.class, args);
        Environment env = context.getEnvironment();
        String activeProfiles = String.join(", ", env.getActiveProfiles());
        System.out.println("Active Spring Profiles: [" + activeProfiles + "]");
    }
}
