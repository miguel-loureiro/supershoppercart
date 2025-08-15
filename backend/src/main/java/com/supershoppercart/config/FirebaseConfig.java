package com.supershoppercart.config;

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
import java.util.Base64;
import java.util.Date;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    // --- Emulator Configuration Properties ---
    @Value("${firebase.project.id.emulator:fir-supershopcart-test}")
    private String emulatorProjectIdProp;

    @Value("${firebase.emulator.host:localhost:8081}")
    private String emulatorHostProp;

    // --- Production Configuration Properties ---
    @Value("${firebase.project.id.prod:supershoppercart}")
    private String productionProjectIdProp;

    @Value("${firebase.service.account.path.prod:classpath:firebase-service-account-prod.json}")
    private String serviceAccountPathProd;

    @Value("${FIREBASE_SERVICE_ACCOUNT_B64_PROD:}")
    private String serviceAccountB64Prod;

    // --- Development Configuration Properties ---
    @Value("${firebase.project.id.dev:devsupershoppercart}")
    private String devProjectIdProp;

    @Value("${firebase.service.account.path.dev:classpath:firebase-service-account-dev.json}")
    private String serviceAccountPathDev;

    @Value("${FIREBASE_SERVICE_ACCOUNT_B64_DEV:}")
    private String serviceAccountB64Dev;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // Custom NoCredentials class for emulator connection
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
     * Configures and provides a Firestore client bean for the development environment.
     * This bean is active only when the "dev" Spring profile is active.
     */
    @Bean
    @Profile("dev")
    public Firestore firestoreDev() throws IOException {
        logger.info("🚀 Configuring Firestore for 'dev' profile...");
        return initializeFirestore(devProjectIdProp, serviceAccountB64Dev, serviceAccountPathDev);
    }

    /**
     * Configures and provides a Firestore client bean for the production environment.
     * This bean is active only when the "prod" Spring profile is active.
     */
    @Bean
    @Profile("prod")
    public Firestore firestoreProduction() throws IOException {
        logger.info("🚀 Configuring Firestore for 'prod' profile...");
        return initializeFirestore(productionProjectIdProp, serviceAccountB64Prod, serviceAccountPathProd);
    }

    /**
     * Private helper method to initialize the Firestore client.
     */
    private Firestore initializeFirestore(String projectId, String serviceAccountB64, String serviceAccountPath) throws IOException {
        // Ensure FIRESTORE_EMULATOR_HOST environment variable is NOT set
        String envEmulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
        if (envEmulatorHost != null && !envEmulatorHost.isBlank()) {
            logger.warn("WARNING: FIRESTORE_EMULATOR_HOST is set to '{}'. Unset it to prevent unintended emulator connections.", envEmulatorHost);
        }

        // Initialize credentials based on the environment variable first.
        GoogleCredentials credentials;
        try {
            if (serviceAccountB64 != null && !serviceAccountB64.isBlank()) {
                logger.info("Using service account key from environment variable.");
                byte[] decodedBytes = Base64.getDecoder().decode(serviceAccountB64);
                credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decodedBytes));
            } else {
                logger.warn("Environment variable for service account key not found. Falling back to file path: '{}'", serviceAccountPath);
                Resource resource = resourceLoader.getResource(serviceAccountPath);
                if (!resource.exists()) {
                    logger.error("CRITICAL ERROR: Firebase service account file not found at: '{}'", serviceAccountPath);
                    throw new IOException("Firebase service account file not found at: " + serviceAccountPath);
                }
                credentials = GoogleCredentials.fromStream(resource.getInputStream());
            }

            // Initialize FirebaseApp only once
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(options);
                logger.info("FirebaseApp initialized for project '{}'.", projectId);
            } else {
                logger.info("FirebaseApp already initialized for project '{}'.", projectId);
            }

            // Create and return Firestore instance
            Firestore firestore = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(credentials)
                    .build().getService();

            logger.info("✅ Firestore client initialized for project '{}'.", projectId);
            return firestore;
        } catch (IOException e) {
            logger.error("CRITICAL ERROR: Error initializing FirebaseApp for project '{}': {}", projectId, e.getMessage(), e);
            throw e;
        }
    }
}