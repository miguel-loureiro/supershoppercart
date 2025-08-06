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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    // --- Emulator Configuration Properties ---
    @Value("${firebase.project.id.emulator:fir-supershopcart-test}")  // Updated to match your firebase.json
    private String emulatorProjectIdProp;

    @Value("${firebase.emulator.host:localhost:8081}")  // Added property for emulator host
    private String emulatorHostProp;

    // --- Production Configuration Properties ---
    @Value("${firebase.project.id.production:supershopcart-prod}")
    private String productionProjectIdProp;

    @Value("${firebase.service.account.path:classpath:firebase-service-account-prod.json}")
    private String serviceAccountPathProp;

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
     * Uses both environment variable and properties file configuration with fallback logic.
     */
    @Bean
    @Profile("dev-emulator")
    public Firestore firestoreEmulator() {
        logger.info("🚀 Configuring Firestore for 'dev-emulator' profile using working test approach...");

        String projectId = emulatorProjectIdProp; // Use the configured project ID for the emulator

        // Check both environment variable and properties file
        String envEmulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
        String finalEmulatorHost;

        // Prefer environment variable if set, otherwise use property
        if (envEmulatorHost != null && !envEmulatorHost.isBlank()) {
            finalEmulatorHost = envEmulatorHost;
            logger.info("DEBUG: Using FIRESTORE_EMULATOR_HOST environment variable: '{}'", envEmulatorHost);
        } else {
            finalEmulatorHost = emulatorHostProp;
            logger.info("DEBUG: Using firebase.emulator.host property: '{}'", emulatorHostProp);
        }

        logger.info("DEBUG: Project ID for emulator: '{}'", projectId);
        logger.info("DEBUG: Final emulator host: '{}'", finalEmulatorHost);

        try {
            // Test direct connection first (like your working test)
            logger.info("Testing direct Firestore emulator connection...");
            testDirectFirestoreConnection(finalEmulatorHost, projectId);

            // Initialize FirebaseApp for the emulator scenario (optional but good practice)
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(NoCredentials.getInstance()) // Use NoCredentials for emulator
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(options);
                logger.info("FirebaseApp initialized for emulator context.");
            } else {
                logger.info("FirebaseApp already initialized for emulator context.");
            }

            // Create the Firestore client using the EXACT same approach as your working test
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setEmulatorHost(finalEmulatorHost) // This will connect to Docker container via localhost:8081
                    .setCredentials(NoCredentials.getInstance()) // Use dummy credentials
                    .build();

            Firestore firestore = firestoreOptions.getService();

            // Log configuration details
            logger.info("✅ Firestore client initialized for EMULATOR using working test approach:");
            logger.info("  - Project ID: {}", firestoreOptions.getProjectId());
            logger.info("  - Displayed Host: {}", firestoreOptions.getHost()); // May still show googleapis.com
            logger.info("  - Emulator Host Setting: {}", firestoreOptions.getEmulatorHost());
            logger.info("  - Actual connection will be to: {} (insecure)", finalEmulatorHost);

            // Note: The displayed host might still show firestore.googleapis.com:443
            // This is a known informational display issue, not a connection issue
            logger.info("Note: If 'Displayed Host' shows 'firestore.googleapis.com:443', this is normal.");
            logger.info("The actual connection is directed to the emulator at {}", finalEmulatorHost);

            return firestore;

        } catch (Exception e) {
            logger.error("❌ Failed to configure Firestore client for emulator. ProjectId: '{}'. Error: {}",
                    projectId, e.getMessage(), e);
            throw new IllegalStateException("Could not create Firestore client for emulator - " + e.getMessage(), e);
        }
    }

    /**
     * Test direct Firestore connection using the same approach as your working MinimalFirestoreEmulatorTest
     */
    private void testDirectFirestoreConnection(String emulatorHost, String projectId) {
        Firestore firestore = null;
        try {
            FirestoreOptions options = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setEmulatorHost(emulatorHost)
                    .setCredentials(NoCredentials.getInstance())
                    .build();

            firestore = options.getService();
            logger.info("Direct connection test - Options created:");
            logger.info("  - Project ID: {}", options.getProjectId());
            logger.info("  - Emulator Host Setting: {}", options.getEmulatorHost());

            // Test write operation (same as your working test)
            DocumentReference docRef = firestore.collection("config-test").document("connection-verification");
            Map<String, Object> testData = Collections.singletonMap("timestamp", System.currentTimeMillis());

            logger.info("Testing write to emulator...");
            ApiFuture<WriteResult> writeResult = docRef.set(testData);
            WriteResult result = writeResult.get();
            logger.info("✅ CONFIG TEST WRITE SUCCESSFUL! Update time: {}", result.getUpdateTime());

            // Test read operation
            ApiFuture<DocumentSnapshot> readResult = docRef.get();
            DocumentSnapshot document = readResult.get();

            if (document.exists()) {
                logger.info("✅ CONFIG TEST READ SUCCESSFUL! Data: {}", document.getData());
                logger.info("🎉 Direct Firestore emulator connection is working in FirebaseConfig!");
            } else {
                logger.warn("❌ Document not found after writing in config test");
            }

        } catch (Exception e) {
            logger.error("❌ CONFIG CONNECTION TEST FAILED: {}", e.getMessage());

            if (e.getMessage().contains("UNAVAILABLE") ||
                    e.getMessage().contains("Connection refused") ||
                    e.getMessage().contains("Failed to connect")) {
                logger.error("💡 This suggests:");
                logger.error("1. Firestore Emulator is NOT RUNNING on {}", emulatorHost);
                logger.error("   - Make sure you run Docker container with Firestore emulator.");
                logger.error("   - Ensure port mapping: docker run -p 8081:8081 <emulator-image>");
                logger.error("2. Firewall is blocking the connection to {}", emulatorHost);
                logger.error("3. Check firebase.json has host: '0.0.0.0' for Docker accessibility");
            } else if (e.getMessage().contains("PERMISSION_DENIED") ||
                    e.getMessage().contains("UNAUTHENTICATED")) {
                logger.error("💡 This error can appear if accidentally connecting to PRODUCTION Firestore.");
                logger.error("   - Double-check FirestoreOptions and setEmulatorHost configuration.");
            } else if (e.getMessage().contains("http2 exception")) {
                logger.error("💡 This 'http2 exception' often means:");
                logger.error("   - Client trying to use TLS/SSL but emulator expects plain HTTP.");
                logger.error("   - Ensure FirestoreOptions.setEmulatorHost() is used correctly.");
            }

            throw new RuntimeException("Failed to verify emulator connection in FirebaseConfig", e);
        } finally {
            if (firestore != null) {
                try {
                    firestore.close();
                } catch (Exception e) {
                    logger.error("Error closing config connection test: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Configures and provides a Firestore client bean for the production environment.
     * This bean is active when the "dev-emulator" Spring profile is NOT active.
     */
    @Bean
    @Profile("!dev-emulator") // Active when dev-emulator profile is NOT active
    public Firestore firestoreProduction() throws IOException {
        logger.info("🚀 Configuring Firestore for PRODUCTION profile...");

        // Ensure FIRESTORE_EMULATOR_HOST environment variable is NOT set for production
        String envEmulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
        if (envEmulatorHost != null && !envEmulatorHost.isBlank()) {
            logger.warn("WARNING: FIRESTORE_EMULATOR_HOST environment variable is set to '{}' in production profile. " +
                    "This could cause unintended connections to an emulator. Please unset it for production.", envEmulatorHost);
        }

        logger.info("DEBUG: Production Project ID from @Value: '{}'", productionProjectIdProp);
        logger.info("DEBUG: Service Account Path from @Value: '{}'", serviceAccountPathProp);

        // Ensure FirebaseApp is initialized only once for the production setup
        if (FirebaseApp.getApps().isEmpty()) {
            Resource resource = resourceLoader.getResource(serviceAccountPathProp);
            if (!resource.exists()) {
                logger.error("CRITICAL ERROR: Firebase service account file not found at: '{}'", serviceAccountPathProp);
                throw new IOException("Firebase service account file not found at: " + serviceAccountPathProp);
            }

            try (InputStream serviceAccount = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(productionProjectIdProp)
                        .build();
                FirebaseApp.initializeApp(options);
                logger.info("FirebaseApp initialized for production environment.");
            } catch (IOException e) {
                logger.error("CRITICAL ERROR: Error initializing FirebaseApp for production: {}", e.getMessage(), e);
                throw e;
            }
        } else {
            logger.info("FirebaseApp already initialized for production.");
        }

        // Return Firestore instance for production
        FirestoreOptions.Builder optionsBuilder = FirestoreOptions.newBuilder();
        optionsBuilder.setProjectId(productionProjectIdProp);

        FirestoreOptions options = optionsBuilder.build();
        Firestore firestore = options.getService();

        logger.info("✅ Firestore client initialized for PRODUCTION. Host: '{}', Project ID: '{}'",
                options.getHost(), options.getProjectId());
        return firestore;
    }
}