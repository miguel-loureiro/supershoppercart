package com.supershopcart.config;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    // --- Emulator Configuration Properties ---
    @Value("${firebase.project.id.emulator:fir-supershopcart-test}")
    private String emulatorProjectIdProp;

    @Value("${firebase.emulator.host:localhost:8081}")
    private String emulatorHostProp;

    // --- Production Configuration Properties ---
    @Value("${firebase.project.id:supershoppercart}")
    private String productionProjectIdProp;

    // This property is for a local dev setup where the key might still be on the classpath.
    // In production, we prefer the environment variable below.
    @Value("${firebase.service.account.path:classpath:firebase-service-account-prod.json}")
    private String serviceAccountPathProp;

    // --- New Environment Variable for Production Secret ---
    @Value("${FIREBASE_SERVICE_ACCOUNT_B64:}")
    private String serviceAccountB64;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // Custom NoCredentials class for emulator connection (same as your working test)
    static class NoCredentials extends GoogleCredentials {
        private static final NoCredentials INSTANCE = new NoCredentials();

        private NoCredentials() {
        }

        public static NoCredentials getInstance() {
            return INSTANCE;
        }

        @Override
        public AccessToken refreshAccessToken() throws IOException {
            return new AccessToken("dummy-token", new Date(Long.MAX_VALUE));
        }
    }

    /**
     * Configures and provides a Firestore client bean for the local emulator.
     * This bean is active only when the "dev-emulator" Spring profile is active.
     */
    @Bean
    @Profile("dev-emulator")
    public Firestore firestoreEmulator() {
        logger.info("🚀 Configuring Firestore for 'dev-emulator' profile...");

        String projectId = emulatorProjectIdProp;
        String envEmulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
        String finalEmulatorHost;

        // Prefer environment variable if set, otherwise use property
        if (envEmulatorHost != null && !envEmulatorHost.isBlank()) {
            finalEmulatorHost = envEmulatorHost;
            logger.info("Using FIRESTORE_EMULATOR_HOST environment variable: '{}'", envEmulatorHost);
        } else {
            finalEmulatorHost = emulatorHostProp;
            logger.info("Using firebase.emulator.host property: '{}'", emulatorHostProp);
        }

        logger.info("Project ID for emulator: '{}'", projectId);
        logger.info("Final emulator host: '{}'", finalEmulatorHost);

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(NoCredentials.getInstance())
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(options);
            }

            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setEmulatorHost(finalEmulatorHost)
                    .setCredentials(NoCredentials.getInstance())
                    .build();

            return firestoreOptions.getService();
        } catch (Exception e) {
            logger.error("❌ Failed to configure Firestore client for emulator. ProjectId: '{}'. Error: {}",
                    projectId, e.getMessage(), e);
            throw new IllegalStateException("Could not create Firestore client for emulator - " + e.getMessage(), e);
        }
    }

    /**
     * Configures and provides a Firestore client bean for the production environment.
     * This bean is active when the "dev-emulator" Spring profile is NOT active.
     */
    @Bean
    @Profile("!dev-emulator")
    public Firestore firestoreProduction() throws IOException {
        logger.info("🚀 Configuring Firestore for PRODUCTION profile...");

        // Ensure FIRESTORE_EMULATOR_HOST environment variable is NOT set for production
        String envEmulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
        if (envEmulatorHost != null && !envEmulatorHost.isBlank()) {
            logger.warn("WARNING: FIRESTORE_EMULATOR_HOST environment variable is set to '{}' in production profile. " +
                    "This could cause unintended connections to an emulator. Please unset it for production.", envEmulatorHost);
        }

        // Initialize credentials based on the environment variable first.
        GoogleCredentials credentials;
        try {
            if (serviceAccountB64 != null && !serviceAccountB64.isBlank()) {
                logger.info("Using service account key from FIREBASE_SERVICE_ACCOUNT_B64 environment variable.");
                byte[] decodedBytes = Base64.getDecoder().decode(serviceAccountB64);
                credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decodedBytes));
            } else {
                logger.warn("FIREBASE_SERVICE_ACCOUNT_B64 not found. Falling back to service account file path: '{}'", serviceAccountPathProp);
                Resource resource = resourceLoader.getResource(serviceAccountPathProp);
                if (!resource.exists()) {
                    logger.error("CRITICAL ERROR: Firebase service account file not found at: '{}'", serviceAccountPathProp);
                    throw new IOException("Firebase service account file not found at: " + serviceAccountPathProp);
                }
                credentials = GoogleCredentials.fromStream(resource.getInputStream());
            }

            // Initialize FirebaseApp only once
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setProjectId(productionProjectIdProp)
                        .build();
                FirebaseApp.initializeApp(options);
                logger.info("FirebaseApp initialized for production environment.");
            } else {
                logger.info("FirebaseApp already initialized for production.");
            }

            // Create and return Firestore instance
            Firestore firestore = FirestoreOptions.newBuilder()
                    .setProjectId(productionProjectIdProp)
                    .setCredentials(credentials)
                    .build().getService();

            logger.info("✅ Firestore client initialized for PRODUCTION. Host: '{}', Project ID: '{}'",
                    firestore.getOptions().getHost(), firestore.getOptions().getProjectId());
            return firestore;
        } catch (IOException e) {
            logger.error("CRITICAL ERROR: Error initializing FirebaseApp for production: {}", e.getMessage(), e);
            throw e;
        }
    }
}