package com.supershoppercart.services;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.supershoppercart.config.FirebaseConfig;
import com.supershoppercart.enums.SharePermission;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FirestoreService using REAL Firebase devsupershoppercart project.
 * This class connects to the actual Firebase development environment.
 * It loads the full Spring application context to test the service with real dependencies.
 */
@SpringBootTest(classes = {FirestoreService.class, FirebaseConfig.class})
@ActiveProfiles("dev") // ✅ Use dev profile for real Firebase
@TestPropertySource(properties = {
        "firebase.project.id.dev=devsupershoppercart",
        "firebase.service.account.path.dev=classpath:firebase-service-accounts/dev-service-account.json",
        "spring.cloud.gcp.project-id=devsupershoppercart",
        "spring.cloud.gcp.firestore.project-id=devsupershoppercart",
        "spring.main.allow-bean-definition-overriding=true",
        // Disable emulator for this test
        "firebase.emulator.enabled=false",
        "spring.cloud.gcp.firestore.emulator.enabled=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FirestoreServiceRealFirebaseIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreServiceRealFirebaseIntegrationTest.class);

    @Autowired
    private Firestore firestore; // Real Firestore client connected to devsupershoppercart

    @Autowired
    private FirestoreService firestoreService; // The service under test

    // Test data - using prefixes to easily identify and clean up test data
    private static final String TEST_PREFIX = "integration_test_";
    private static String testShopperId = TEST_PREFIX + UUID.randomUUID().toString();
    private static String creatorShopperId = TEST_PREFIX + UUID.randomUUID().toString();
    private static String testCartId;
    private static List<String> createdDocumentIds = new ArrayList<>(); // Track all created docs for cleanup

    /**
     * Setup method to ensure necessary shoppers exist for later tests.
     */
    @BeforeAll
    void setup() throws Exception {
        logger.info("🔥 Setting up REAL Firebase integration test data...");
        logger.info("📍 Using Firebase project: devsupershoppercart");

        // Verify connection to real Firebase
        verifyFirebaseConnection();

        // Save the main test shopper
        Shopper shopper = new Shopper("test.integration@example.com", "Integration Test User", "plainPassword123");
        shopper.setId(testShopperId);
        firestoreService.saveShopper(shopper);
        createdDocumentIds.add("shoppers/" + testShopperId);

        // Save creator shopper
        Shopper creator = new Shopper("creator.integration@example.com", "Integration Creator", "password123");
        creator.setId(creatorShopperId);
        firestoreService.saveShopper(creator);
        createdDocumentIds.add("shoppers/" + creatorShopperId);

        logger.info("✅ Test shoppers created in REAL Firebase: testShopperId={}, creatorShopperId={}",
                testShopperId, creatorShopperId);
    }

    /**
     * Verify that we can connect to the real Firebase project
     */
    private void verifyFirebaseConnection() throws Exception {
        logger.info("🔍 Verifying connection to real Firebase...");

        // Test basic connectivity by writing and reading a test document
        String testDocId = TEST_PREFIX + "connection_" + System.currentTimeMillis();
        DocumentReference testDoc = firestore.collection("integration_tests").document(testDocId);

        Map<String, Object> testData = Map.of(
                "timestamp", System.currentTimeMillis(),
                "test", "firebase-connection",
                "project", "devsupershoppercart"
        );

        testDoc.set(testData).get(10, TimeUnit.SECONDS);
        createdDocumentIds.add("integration_tests/" + testDocId);

        DocumentSnapshot snapshot = testDoc.get().get(10, TimeUnit.SECONDS);
        assertTrue(snapshot.exists(), "Should be able to write and read from real Firebase");

        logger.info("✅ Real Firebase connection verified successfully!");
    }

    /**
     * Cleanup method to remove ALL test data from real Firebase after tests complete.
     * This is CRITICAL to avoid leaving test data in your development environment.
     */
    @AfterAll
    void cleanup() {
        logger.info("🧹 Cleaning up REAL Firebase test data...");
        /*
        int cleanedCount = 0;
        List<String> failedCleanups = new ArrayList<>();

        try {
            // Clean up all tracked documents
            for (String docPath : createdDocumentIds) {
                try {
                    String[] parts = docPath.split("/");
                    if (parts.length == 2) {
                        firestore.collection(parts[0]).document(parts[1]).delete().get(5, TimeUnit.SECONDS);
                        cleanedCount++;
                        logger.debug("🗑️ Deleted: {}", docPath);
                    }
                } catch (Exception e) {
                    logger.error("❌ Failed to delete {}: {}", docPath, e.getMessage());
                    failedCleanups.add(docPath);
                }
            }

            // Clean up any additional test documents that might have been missed
            cleanupTestCollections();

            logger.info("✅ Firebase cleanup completed: {} documents cleaned, {} failures",
                    cleanedCount, failedCleanups.size());

            if (!failedCleanups.isEmpty()) {
                logger.warn("⚠️ Failed to clean up these documents: {}", failedCleanups);
            }

        } catch (Exception e) {
            logger.error("❌ Error during Firebase cleanup: {}", e.getMessage(), e);
            // Don't throw - cleanup should not fail the test run
        } */
    }

    /**
     * Clean up any test documents in collections that might have been created
     */
    private void cleanupTestCollections() throws Exception {
        // Clean up any test documents in integration_tests collection
        firestore.collection("integration_tests")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), TEST_PREFIX)
                .whereLessThan(FieldPath.documentId(), TEST_PREFIX + "z")
                .get().get(10, TimeUnit.SECONDS)
                .getDocuments()
                .forEach(doc -> {
                    try {
                        doc.getReference().delete().get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.error("Error deleting test document: {}", doc.getId(), e);
                    }
                });
    }

    @Test
    @Order(0) // Run first
    void debugFirebaseConnection() throws Exception {
        logger.info("🔍 DEBUG: Testing basic Firebase write...");

        String testDoc = "debug_" + System.currentTimeMillis();
        DocumentReference ref = firestore.collection("debug_test").document(testDoc);

        Map<String, Object> data = Map.of(
                "timestamp", System.currentTimeMillis(),
                "message", "Debug test from integration test"
        );

        ref.set(data).get(10, TimeUnit.SECONDS);
        logger.info("📝 DEBUG: Document written with ID: {}", testDoc);

        // Don't add to cleanup list - leave it there for manual verification

        DocumentSnapshot snapshot = ref.get().get(5, TimeUnit.SECONDS);
        logger.info("📋 DEBUG: Document exists: {}", snapshot.exists());

        assertTrue(snapshot.exists(), "Debug document should exist");
    }

    @Test
    @Order(1)
    void testSaveAndReadShopCartFromRealFirebase() throws Exception {
        logger.info("🧪 Testing saveAndReadShopCart with REAL Firebase...");

        // Create a ShopCart for testing
        ShopCart cart = new ShopCart();
        cart.setId(TEST_PREFIX + UUID.randomUUID().toString()); // ✅ Set ID with test prefix
        cart.setName("Real Firebase Integration Test Cart");
        cart.setDateKey("2025-08-17");
        cart.setCreatedBy(testShopperId); // Use existing test shopper
        cart.setShopperIds(List.of(testShopperId, creatorShopperId));

        // Save using the service (this tests your actual service code)
        String savedId = firestoreService.saveShopCart(cart);
        logger.info("📝 SAVED CART WITH ID: {}", savedId);
        assertNotNull(savedId, "Saved cart ID should not be null");
        assertEquals(cart.getId(), savedId, "Service should return the cart ID");

        // Add a small delay to ensure write completes
        Thread.sleep(1000);

        // Track for cleanup
        testCartId = savedId;
        createdDocumentIds.add("shopcarts/" + savedId);

        logger.info("🔥 Cart saved to REAL Firebase with ID: {}", savedId);

        // Read back the document to verify
        DocumentReference docRef = firestore.collection("shopcarts").document(savedId);
        DocumentSnapshot snapshot = docRef.get().get(10, TimeUnit.SECONDS);
        logger.info("📋 IMMEDIATE CHECK - EXISTS: {}", snapshot.exists());
        assertTrue(snapshot.exists(), "The document should exist in real Firebase");

        ShopCart loaded = snapshot.toObject(ShopCart.class);
        assertNotNull(loaded, "The converted ShopCart should not be null");
        assertEquals(cart.getName(), loaded.getName());
        assertEquals(cart.getCreatedBy(), loaded.getCreatedBy());
        assertEquals(cart.getDateKey(), loaded.getDateKey());
        assertEquals(cart.getShopperIds(), loaded.getShopperIds());
        assertEquals(cart.getId(), loaded.getId());

        // Verify that permissions were set correctly (your service adds permissions)
        assertNotNull(loaded.getSharePermissions(), "Share permissions should be set");
        assertFalse(loaded.getSharePermissions().isEmpty(), "Share permissions should not be empty");

        // Verify that all shoppers in shopperIds list have EDIT permission
        for (String shopperId : loaded.getShopperIds()) {
            SharePermission permission = loaded.getPermissionForShopper(shopperId);
            assertNotNull(permission, "Shopper " + shopperId + " should have permissions");

            if (shopperId.equals(loaded.getCreatedBy())) {
                // Creator should have ADMIN permission
                assertEquals(SharePermission.ADMIN, permission, "Creator " + shopperId + " should have ADMIN permission");
            } else {
                // Non-creators should have EDIT permission
                assertEquals(SharePermission.EDIT, permission, "Non-creator " + shopperId + " should have EDIT permission");
            }
        }

        // Verify creator has ADMIN permission (creator should override EDIT with ADMIN)
        SharePermission creatorPermission = loaded.getPermissionForShopper(loaded.getCreatedBy());
        assertNotNull(creatorPermission, "Creator should have permissions");
        assertEquals(SharePermission.ADMIN, creatorPermission, "Creator should have ADMIN permission");

        logger.info("📋 Permissions verified: {} total permissions set", loaded.getSharePermissions().size());

        logger.info("✅ Real Firebase test completed successfully - cart verified");
    }

    @Test
    @Order(2)
    void testDirectFirestoreOperationsWithRealFirebase() throws Exception {
        logger.info("🧪 Testing direct Firestore operations with REAL Firebase...");

        // Create test data
        ShopCart cart = new ShopCart();
        cart.setId(TEST_PREFIX + "direct_" + UUID.randomUUID().toString());
        cart.setName("Direct Real Firebase Test Cart");
        cart.setDateKey("2025-08-17");
        cart.setCreatedBy("direct.test@example.com");
        cart.setShopperIds(List.of("direct.test@example.com"));

        // Direct Firestore operations (bypassing your service)
        DocumentReference docRef = firestore.collection("shopcarts").document(cart.getId());
        docRef.set(cart).get(10, TimeUnit.SECONDS);

        // Track for cleanup
        createdDocumentIds.add("shopcarts/" + cart.getId());

        logger.info("🔥 Cart saved directly to REAL Firebase with ID: {}", docRef.getId());

        // Read back the document
        DocumentSnapshot snapshot = docRef.get().get(10, TimeUnit.SECONDS);
        assertTrue(snapshot.exists(), "The document should exist in real Firebase");

        ShopCart loaded = snapshot.toObject(ShopCart.class);
        assertNotNull(loaded, "The converted ShopCart should not be null");
        assertEquals(cart.getName(), loaded.getName());
        assertEquals(cart.getCreatedBy(), loaded.getCreatedBy());
        assertEquals(cart.getId(), loaded.getId());

        logger.info("✅ Direct real Firebase test completed successfully");
    }

    @Test
    @Order(3)
    void testShopCartPermissionsWithRealFirebase() throws Exception {
        logger.info("🧪 Testing ShopCart permissions with REAL Firebase...");

        // Create a cart with multiple shoppers
        ShopCart cart = new ShopCart();
        cart.setId(TEST_PREFIX + "permissions_" + UUID.randomUUID().toString());
        cart.setName("Permissions Test Cart");
        cart.setDateKey("2025-08-17");
        cart.setCreatedBy(creatorShopperId); // Creator should get ADMIN
        cart.setShopperIds(List.of(testShopperId, creatorShopperId)); // Both should get EDIT initially

        // Save using the service (which should set permissions automatically)
        String savedId = firestoreService.saveShopCart(cart);
        createdDocumentIds.add("shopcarts/" + savedId);

        // Read back and verify permissions
        DocumentSnapshot snapshot = firestore.collection("shopcarts")
                .document(savedId).get().get(10, TimeUnit.SECONDS);
        ShopCart loaded = snapshot.toObject(ShopCart.class);

        assertNotNull(loaded);
        assertNotNull(loaded.getSharePermissions(), "Share permissions should be set");
        assertFalse(loaded.getSharePermissions().isEmpty(), "Share permissions should not be empty");

        // Verify specific permissions
        SharePermission testShopperPermission = loaded.getPermissionForShopper(testShopperId);
        SharePermission creatorPermission = loaded.getPermissionForShopper(creatorShopperId);

        assertEquals(SharePermission.EDIT, testShopperPermission, "Regular shopper should have EDIT permission");
        assertEquals(SharePermission.ADMIN, creatorPermission, "Creator should have ADMIN permission (overrides EDIT)");

        // Test permission helper methods
        assertNotNull(loaded.getPermissionForShopper(testShopperId), "Should find permission for test shopper");
        assertNull(loaded.getPermissionForShopper("non-existent-id"), "Should return null for non-existent shopper");

        logger.info("✅ Permissions test completed - verified EDIT and ADMIN permissions");
    }

    @Test
    @Order(4)
    void testBulkOperationsWithRealFirebase() throws Exception {
        logger.info("🧪 Testing bulk operations with REAL Firebase...");

        // Create multiple test documents
        List<ShopCart> carts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ShopCart cart = new ShopCart();
            cart.setId(TEST_PREFIX + "bulk_" + i + "_" + UUID.randomUUID().toString());
            cart.setName("Bulk Test Cart " + i);
            cart.setDateKey("2025-08-17");
            cart.setCreatedBy("bulk.test@example.com");
            cart.setShopperIds(List.of("bulk.test@example.com"));
            carts.add(cart);
        }

        // Save all carts
        for (ShopCart cart : carts) {
            String savedId = firestoreService.saveShopCart(cart);
            createdDocumentIds.add("shopcarts/" + savedId);
            assertNotNull(savedId, "Each cart should be saved successfully");
        }

        // Verify all carts exist
        for (ShopCart cart : carts) {
            DocumentSnapshot snapshot = firestore.collection("shopcarts")
                    .document(cart.getId()).get().get(10, TimeUnit.SECONDS);
            assertTrue(snapshot.exists(), "Bulk cart should exist: " + cart.getId());
        }

        logger.info("✅ Bulk operations test completed - {} carts processed", carts.size());
    }
}