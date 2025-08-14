package com.supershoppercart.services;

import com.google.cloud.firestore.*;
import com.supershoppercart.config.FirebaseConfig;
import com.supershoppercart.dtos.ShopCartDetailDTO;
import com.supershoppercart.enums.SharePermission;
import com.supershoppercart.enums.ShopCartState;
import com.supershoppercart.models.GroceryItem;
import com.supershoppercart.models.SharePermissionEntry;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FirestoreService. This class connects to a real Firestore emulator.
 * It loads the full Spring application context to test the service with real dependencies.
 */
@SpringBootTest(classes = {FirestoreService.class, FirebaseConfig.class})
@ActiveProfiles("dev-emulator")
@TestPropertySource(properties = {
        "firebase.emulator.host=localhost:8081",
        "firebase.project.id.emulator=fir-supershopcart-test",
        "spring.main.allow-bean-definition-overriding=true" // Allows overriding beans if needed
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows @BeforeAll and @AfterAll on non-static methods
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Ensures tests run in a specific order
public class FirestoreServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreServiceIntegrationTest.class);

    @Autowired
    private Firestore firestore; // The real Firestore client connected to the emulator

    @Autowired
    private FirestoreService firestoreService; // The real service under test

    // Static test data to be used across tests
    private static String testShopperId = UUID.randomUUID().toString();
    private static String creatorShopperId = UUID.randomUUID().toString();
    private static String sharedShopperId = UUID.randomUUID().toString();
    private static String adminShopperId = UUID.randomUUID().toString();
    private static String editorShopperId = UUID.randomUUID().toString();
    private static String testCartId; // To store the ID of a cart created in one test for use in others

    /**
     * Setup method to ensure necessary shoppers exist for later tests.
     * This runs once before all tests in this class.
     */
    @BeforeAll
    void setup() throws Exception {
        logger.info("Setting up integration test data for all tests...");

        // Save the main test shopper
        Shopper shopper = new Shopper("test@example.com", "Test User", "plainPassword123");
        shopper.setId(testShopperId);
        firestoreService.saveShopper(shopper);

        // Save creator shopper
        Shopper creator = new Shopper("creator@example.com", "Creator", "password123");
        creator.setId(creatorShopperId);
        firestoreService.saveShopper(creator);

        // Save shared shopper
        Shopper shared = new Shopper("shared@example.com", "Shared Shopper", "password123");
        shared.setId(sharedShopperId);
        firestoreService.saveShopper(shared);

        // Save admin shopper
        Shopper admin = new Shopper("admin@example.com", "Admin Shopper", "password123");
        admin.setId(adminShopperId);
        firestoreService.saveShopper(admin);

        // Save editor shopper
        Shopper editor = new Shopper("editor@example.com", "Editor Shopper", "password123");
        editor.setId(editorShopperId);
        firestoreService.saveShopper(editor);

        logger.info("Test shoppers created: testShopperId={}, creatorShopperId={}, sharedShopperId={}, adminShopperId={}, editorShopperId={}",
                testShopperId, creatorShopperId, sharedShopperId, adminShopperId, editorShopperId);
    }

    /**
     * Cleanup method to remove test data from the Firestore emulator after all tests have run.
     */
    @AfterAll
    void cleanup() {
        logger.info("Cleaning up Firestore emulator data...");
        try {
            // Delete shoppers
            firestore.collection("shoppers").document(testShopperId).delete().get(5, TimeUnit.SECONDS);
            firestore.collection("shoppers").document(creatorShopperId).delete().get(5, TimeUnit.SECONDS);
            firestore.collection("shoppers").document(sharedShopperId).delete().get(5, TimeUnit.SECONDS);
            firestore.collection("shoppers").document(adminShopperId).delete().get(5, TimeUnit.SECONDS);
            firestore.collection("shoppers").document(editorShopperId).delete().get(5, TimeUnit.SECONDS);

            // Delete the test cart if it was created
            if (testCartId != null) {
                firestore.collection("shopcarts").document(testCartId).delete().get(5, TimeUnit.SECONDS);
            }

            // Clean up any bulk shoppers created
            firestore.collection("shoppers").whereGreaterThanOrEqualTo(FieldPath.documentId(), "bulk").get().get().getDocuments()
                    .forEach(doc -> {
                        try {
                            doc.getReference().delete().get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            logger.error("Error deleting bulk shopper document: {}", doc.getId(), e);
                        }
                    });

            // Clean up connection-test document
            firestore.collection("config-test").document("connection-verification").delete().get(5, TimeUnit.SECONDS);

            logger.info("Firestore emulator data cleanup complete.");
        } catch (Exception e) {
            logger.error("Error during Firestore emulator data cleanup: {}", e.getMessage(), e);
            // Do not rethrow, as cleanup should not fail the test run
        }
    }

    /**
     * Test to verify the connection to the Firestore emulator is working.
     * This is a basic sanity check for the test environment.
     */
    @Test
    @Order(1)
    @DisplayName("Should successfully connect to and interact with the Firestore emulator")
    void testEmulatorConnection() throws Exception {
        logger.info("Running testEmulatorConnection...");
        CollectionReference testCollection = firestore.collection("config-test");
        Map<String, Object> testData = Map.of(
                "timestamp", System.currentTimeMillis(),
                "test", true,
                "environment", "emulator",
                "debug", "testing-emulator-connection"
        );

        // Add a document and verify its existence
        DocumentReference docRef = testCollection.add(testData).get(10, TimeUnit.SECONDS);
        assertNotNull(docRef, "Document reference should not be null after adding data");

        DocumentSnapshot doc = docRef.get().get(5, TimeUnit.SECONDS);
        assertTrue(doc.exists(), "Document should exist in the emulator");
        assertEquals(true, doc.get("test"), "The 'test' field should be true");
        logger.info("Successfully connected to emulator and verified document.");
    }

    @Test
    @Order(2)
    @DisplayName("Should save a new shopper and retrieve it")
    void testSaveShopperAndRetrieve() throws Exception {
        String newShopperId = UUID.randomUUID().toString();
        Shopper newShopper = new Shopper("new.shopper@example.com", "New Test Shopper", "password123");
        newShopper.setId(newShopperId);

        String savedId = firestoreService.saveShopper(newShopper);
        assertEquals(newShopperId, savedId, "Saved shopper ID should match the provided ID");

        // Verify directly from Firestore
        DocumentSnapshot doc = firestore.collection("shoppers").document(newShopperId).get().get();
        assertTrue(doc.exists(), "New shopper document should exist in Firestore");
        Shopper retrievedShopper = doc.toObject(Shopper.class);
        assertNotNull(retrievedShopper);
        assertEquals(newShopper.getEmail(), retrievedShopper.getEmail());

        // Clean up this specific shopper
        firestore.collection("shoppers").document(newShopperId).delete().get(5, TimeUnit.SECONDS);
    }

    @Test
    @Order(3)
    @DisplayName("Should save ShopCart with creator and assign permissions correctly")
    void testSaveShopCartWithCreatorAssignsPermissions() throws Exception {
        ShopCart cart = new ShopCart();
        cart.setName("Integration Test Cart");
        cart.setId("something_123");
        cart.setItems(List.of(new GroceryItem("Milk", "1 gallon", false)));
        cart.setDateKey(LocalDate.now().toString());
        cart.setShopperIds(new ArrayList<>()); // Initialize to empty, service will add creator

        String savedId = firestoreService.saveShopCartWithCreator(cart, creatorShopperId);
        assertNotNull(savedId, "savedId must not be null");
        testCartId = savedId; // Store for subsequent tests

        // Retrieve the cart directly from Firestore to verify raw data
        DocumentSnapshot doc = firestore.collection("shopcarts").document(savedId).get().get();
        assertTrue(doc.exists(), "Cart document should exist");
        ShopCart savedCart = doc.toObject(ShopCart.class);

        assertNotNull(savedCart);
        assertEquals("Integration Test Cart", savedCart.getName());
        assertEquals(creatorShopperId, savedCart.getCreatedBy());
        assertTrue(savedCart.getShopperIds().contains(creatorShopperId));

        // Verify permissions directly on the saved cart object
        SharePermission actualCreatorPermission = savedCart.getSharePermissions().stream()
                .filter(p -> p.getShopperId().equals(creatorShopperId))
                .map(SharePermissionEntry::getPermission)
                .findFirst()
                .orElse(null);
        assertEquals(SharePermission.ADMIN, actualCreatorPermission, "Creator should have ADMIN permission");

        // Retrieve via service DTO method to check hydration
        ShopCartDetailDTO dto = firestoreService.getShopCartById(savedId);
        assertNotNull(dto, "DTO should not be null");
        assertEquals("Integration Test Cart", dto.getName());
        assertEquals(creatorShopperId, dto.getCreatedBy());
        assertTrue(dto.getShopperIds().contains(creatorShopperId));

        SharePermission dtoCreatorPermission = dto.getSharePermissions().stream()
                .filter(p -> p.getShopperId().equals(creatorShopperId))
                .map(SharePermissionEntry::getPermission)
                .findFirst()
                .orElse(null);
        assertEquals(SharePermission.ADMIN, dtoCreatorPermission, "DTO should reflect ADMIN permission for creator");
    }

    @Test
    @Order(4)
    @DisplayName("Should retrieve ShopCarts for creator shopper ID with correct details")
    void testGetShopCartsByShopperIdReturnsValidCart() {
        List<ShopCartDetailDTO> carts = firestoreService.getShopCartsByShopperId(creatorShopperId);
        assertFalse(carts.isEmpty(), "Should find at least one cart for the creator");

        // Find the specific cart created in the previous test
        ShopCartDetailDTO dto = carts.stream()
                .filter(c -> c.getIdentifier().equals(testCartId))
                .findFirst()
                .orElse(null);

        assertNotNull(dto, "The specific test cart should be found");
        assertEquals(creatorShopperId, dto.getCreatedBy(), "Cart's createdBy should match creator ID");
        assertNotNull(dto.getShoppers(), "Shoppers list in DTO should not be null");
        assertFalse(dto.getShoppers().isEmpty(), "Shoppers list in DTO should not be empty");
        assertTrue(dto.getShoppers().stream()
                .anyMatch(s -> s.getId().equals(creatorShopperId)), "Creator should be in the shopper list");
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve single ShopCart by ID and hydrate all fields including shoppers")
    void testGetShopCartByIdHydratesShopperDTOs() {
        ShopCartDetailDTO dto = firestoreService.getShopCartById(testCartId);
        assertNotNull(dto, "Retrieved DTO should not be null");
        assertEquals(testCartId, dto.getIdentifier(), "DTO identifier should match testCartId");
        assertNotNull(dto.getShoppers(), "Shoppers list in DTO should not be null");
        assertTrue(dto.getShoppers().stream()
                .anyMatch(s -> s.getId().equals(creatorShopperId)), "Creator shopper should be included in DTO's shoppers");
    }

    @Test
    @Order(6)
    @DisplayName("Should share cart with EDIT permission and then remove it")
    void testShareAndRemoveCartPermission() throws Exception {
        // Share with EDIT permission
        boolean shared = firestoreService.shareCartWithShopper(
                testCartId, creatorShopperId, "shared@example.com", SharePermission.EDIT
        );
        assertTrue(shared, "Sharing cart should return true");

        // Verify sharing by retrieving the cart via service and checking DTO
        ShopCartDetailDTO dtoAfterShare = firestoreService.getShopCartById(testCartId);
        assertNotNull(dtoAfterShare, "Cart DTO should exist after sharing");
        assertTrue(dtoAfterShare.getShopperIds().contains(sharedShopperId), "Shared shopper ID should be in shopperIds list");

        SharePermission actualPermission = dtoAfterShare.getSharePermissions().stream()
                .filter(p -> p.getShopperId().equals(sharedShopperId))
                .map(SharePermissionEntry::getPermission)
                .findFirst()
                .orElse(null);
        assertEquals(SharePermission.EDIT, actualPermission, "Shared shopper should have EDIT permission");

        // Remove sharing permission
        boolean removed = firestoreService.removeCartSharing(testCartId, creatorShopperId, sharedShopperId);
        assertTrue(removed, "Removing cart sharing should return true");

        // Verify removal by retrieving the cart via service and checking DTO
        ShopCartDetailDTO dtoAfterRemoval = firestoreService.getShopCartById(testCartId);
        assertNotNull(dtoAfterRemoval, "Cart DTO should still exist after removal");
        assertFalse(dtoAfterRemoval.getShopperIds().contains(sharedShopperId), "Shared shopper ID should NOT be in shopperIds list after removal");

        SharePermission afterRemovalPermission = dtoAfterRemoval.getSharePermissions().stream()
                .filter(p -> p.getShopperId().equals(sharedShopperId))
                .map(SharePermissionEntry::getPermission)
                .findFirst()
                .orElse(null);
        assertNull(afterRemovalPermission, "Shared shopper should have NO permission after removal");
    }

    @Test
    @Order(7)
    @DisplayName("Should allow creator to edit cart")
    void testCanEditCart_Creator() throws Exception {
        // Retrieve the cart from Firestore to get the actual ShopCart object
        DocumentSnapshot doc = firestore.collection("shopcarts").document(testCartId).get().get();
        ShopCart cart = doc.toObject(ShopCart.class);
        assertNotNull(cart, "ShopCart object should be retrieved");

        // Test the canEditCart method from FirestoreService
        boolean canEdit = firestoreService.canEditCart(cart, creatorShopperId);
        assertTrue(canEdit, "Creator should be able to edit the cart");
    }

    @Test
    @Order(8)
    @DisplayName("Should allow ADMIN to edit cart")
    void testCanEditCart_Admin() throws Exception {
        // First, add ADMIN permission to the adminShopperId
        firestoreService.shareCartWithShopper(testCartId, creatorShopperId, "admin@example.com", SharePermission.ADMIN);

        // Retrieve the updated cart from Firestore
        DocumentSnapshot doc = firestore.collection("shopcarts").document(testCartId).get().get();
        ShopCart cart = doc.toObject(ShopCart.class);
        assertNotNull(cart, "ShopCart object should be retrieved");

        // Test the canEditCart method from FirestoreService
        boolean canEdit = firestoreService.canEditCart(cart, adminShopperId);
        assertTrue(canEdit, "ADMIN should be able to edit the cart");

        // Clean up: remove admin permission
        firestoreService.removeCartSharing(testCartId, creatorShopperId, adminShopperId);
    }

    @Test
    @Order(9)
    @DisplayName("Should allow EDITOR to edit cart")
    void testCanEditCart_Editor() throws Exception {
        // First, add EDIT permission to the editorShopperId
        firestoreService.shareCartWithShopper(testCartId, creatorShopperId, "editor@example.com", SharePermission.EDIT);

        // Retrieve the updated cart from Firestore
        DocumentSnapshot doc = firestore.collection("shopcarts").document(testCartId).get().get();
        ShopCart cart = doc.toObject(ShopCart.class);
        assertNotNull(cart, "ShopCart object should be retrieved");

        // Test the canEditCart method from FirestoreService
        boolean canEdit = firestoreService.canEditCart(cart, editorShopperId);
        assertTrue(canEdit, "EDITOR should be able to edit the cart");

        // Clean up: remove editor permission
        firestoreService.removeCartSharing(testCartId, creatorShopperId, editorShopperId);
    }

    @Test
    @Order(10)
    @DisplayName("Should NOT allow VIEWER to edit cart")
    void testCanEditCart_Viewer() throws Exception {
        // First, add VIEW permission to the sharedShopperId
        firestoreService.shareCartWithShopper(testCartId, creatorShopperId, "shared@example.com", SharePermission.VIEW);

        // Retrieve the updated cart from Firestore
        DocumentSnapshot doc = firestore.collection("shopcarts").document(testCartId).get().get();
        ShopCart cart = doc.toObject(ShopCart.class);
        assertNotNull(cart, "ShopCart object should be retrieved");

        // Test the canEditCart method from FirestoreService
        boolean canEdit = firestoreService.canEditCart(cart, sharedShopperId);
        assertFalse(canEdit, "VIEWER should NOT be able to edit the cart");

        // Clean up: remove view permission
        firestoreService.removeCartSharing(testCartId, creatorShopperId, sharedShopperId);
    }

    @Test
    @Order(11)
    @DisplayName("Should NOT allow unknown shopper to edit cart")
    void testCanEditCart_UnknownShopper() throws Exception {
        // Retrieve the cart from Firestore
        DocumentSnapshot doc = firestore.collection("shopcarts").document(testCartId).get().get();
        ShopCart cart = doc.toObject(ShopCart.class);
        assertNotNull(cart, "ShopCart object should be retrieved");

        // Test the canEditCart method from FirestoreService with an unknown shopper ID
        boolean canEdit = firestoreService.canEditCart(cart, "someUnknownShopperId");
        assertFalse(canEdit, "Unknown shopper should NOT be able to edit the cart");
    }

    @Test
    @Order(12)
    @DisplayName("Should fetch shoppers using batching when >10 IDs")
    void testFetchShoppersByIdsBatch() throws Exception {
        List<String> manyIds = new ArrayList<>();
        // Create 12 new shoppers for this test
        for (int i = 0; i < 12; i++) {
            String id = UUID.randomUUID().toString();
            Shopper s = new Shopper("bulk" + i + "@example.com", "Bulk " + i, "bulkpass");
            s.setId(id);
            firestoreService.saveShopper(s); // Save to the emulator
            manyIds.add(id);
        }

        // Add some existing shopper IDs to ensure mixed batching works
        manyIds.add(creatorShopperId);
        manyIds.add(editorShopperId);

        List<Shopper> results = firestoreService.fetchShoppersByIds(manyIds);
        // Expect 12 new shoppers + 2 existing shoppers
        assertEquals(14, results.size(), "Should retrieve all 14 shoppers");

        // Verify some specific shoppers are present
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(creatorShopperId)));
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(manyIds.get(0)))); // First bulk shopper
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(manyIds.get(11)))); // Last bulk shopper
    }

    @Test
    @Order(13)
    @DisplayName("Should mark all items as purchased and transition cart state to SHOPPING")
    void testStateTransitionToShopping() throws Exception {
        // Retrieve the cart
        DocumentReference cartRef = firestore.collection("shopcarts").document(testCartId);
        ShopCart cart = cartRef.get().get().toObject(ShopCart.class);
        assertNotNull(cart);

        // Add items and mark them all as purchased
        List<GroceryItem> items = List.of(
                new GroceryItem("Apples", "3 kg", true),
                new GroceryItem("Bananas", "2 kg", true)
        );
        cart.setItems(items);
        cart.updateStateBasedOnItems(); // This method is on the ShopCart model

        // Update the cart in Firestore
        cartRef.set(cart).get();

        // Retrieve again to verify updated state from Firestore
        ShopCart updatedCart = cartRef.get().get().toObject(ShopCart.class);
        assertNotNull(updatedCart);
        assertEquals(ShopCartState.SHOPPING, updatedCart.getState(), "Cart state should be SHOPPING");
        assertTrue(updatedCart.getItems().stream().allMatch(GroceryItem::isPurchased), "All items should be purchased");
    }

    @Test
    @Order(14)
    @DisplayName("Should convert cart to template and reset purchased flags")
    void testConvertCartToTemplate() throws Exception {
        // Retrieve the cart
        DocumentReference cartRef = firestore.collection("shopcarts").document(testCartId);
        ShopCart cart = cartRef.get().get().toObject(ShopCart.class);
        assertNotNull(cart);

        // Convert to template
        cart.convertToTemplate("Weekly Basics Template"); // This method is on the ShopCart model

        // Update the cart in Firestore
        cartRef.set(cart).get();

        // Retrieve again to verify updated state from Firestore
        ShopCart updatedCart = cartRef.get().get().toObject(ShopCart.class);
        assertNotNull(updatedCart);
        assertTrue(updatedCart.isTemplate(), "Cart should be a template");
        assertEquals("Weekly Basics Template", updatedCart.getTemplateName(), "Template name should be set");
        assertEquals(ShopCartState.TEMPLATE, updatedCart.getState(), "Cart state should be TEMPLATE");
        assertTrue(updatedCart.getItems().stream().noneMatch(GroceryItem::isPurchased), "Items in template should be unpurchased");
    }

    @Test
    @Order(15)
    @DisplayName("Should create new cart from template with reset state and items")
    void testCreateFromTemplate() throws Exception {
        // Retrieve the template cart (from previous test)
        DocumentReference templateCartRef = firestore.collection("shopcarts").document(testCartId);
        ShopCart templateCart = templateCartRef.get().get().toObject(ShopCart.class);
        assertNotNull(templateCart);
        assertTrue(templateCart.isTemplate(), "Original cart must be a template for this test");

        // Create a new cart from the template
        ShopCart newCart = templateCart.createFromTemplate(creatorShopperId, List.of(creatorShopperId, sharedShopperId));
        assertNotNull(newCart, "New cart should not be null");

        // Save the new cart using the service (this will assign a new ID)
        String newCartId = firestoreService.saveShopCart(newCart);
        assertNotNull(newCartId, "New cart should have a generated ID");

        // Retrieve the new cart from Firestore to verify its state
        DocumentSnapshot newCartDoc = firestore.collection("shopcarts").document(newCartId).get().get();
        assertTrue(newCartDoc.exists(), "New cart document should exist");
        ShopCart retrievedNewCart = newCartDoc.toObject(ShopCart.class);
        assertNotNull(retrievedNewCart);

        assertNotEquals(testCartId, retrievedNewCart.getId(), "New cart should have a different ID");
        assertEquals(ShopCartState.ACTIVE, retrievedNewCart.getState(), "New cart state should be ACTIVE");
        assertEquals(creatorShopperId, retrievedNewCart.getCreatedBy(), "New cart createdBy should be set");
        assertFalse(retrievedNewCart.getItems().stream().anyMatch(GroceryItem::isPurchased), "Items in new cart should be unpurchased");
        assertFalse(retrievedNewCart.isTemplate(), "New cart should not be a template");
        assertTrue(retrievedNewCart.getShopperIds().contains(creatorShopperId));
        assertTrue(retrievedNewCart.getShopperIds().contains(sharedShopperId));

        // Clean up the newly created cart
        firestore.collection("shopcarts").document(newCartId).delete().get(5, TimeUnit.SECONDS);
    }
}

