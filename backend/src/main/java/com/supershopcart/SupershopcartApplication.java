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
		// Set the active profile
		System.setProperty("spring.profiles.active", "dev-emulator");

		// Debug output to verify environment variables
		System.out.println("FIRESTORE_EMULATOR_HOST: " + System.getenv("FIRESTORE_EMULATOR_HOST"));
		System.out.println("GCLOUD_PROJECT: " + System.getenv("GCLOUD_PROJECT"));

		ConfigurableApplicationContext context = SpringApplication.run(SupershopcartApplication.class, args);
		Environment env = context.getEnvironment();
		String activeProfiles = String.join(", ", env.getActiveProfiles());
		System.out.println("Active Spring Profiles: [" + activeProfiles + "]");
	}
}
