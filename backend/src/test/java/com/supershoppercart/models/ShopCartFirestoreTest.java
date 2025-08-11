package com.supershoppercart.models;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList; // Import ArrayList
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for directly interacting with Firestore using POJOs,
 * verifying data persistence and retrieval functionality.
 * This class specifically tests the low-level Firestore mapping,
 * not the business logic of ShopCartService.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("dev-emulator")
@TestPropertySource(properties = {
        "firebase.emulator.host=localhost:8081",
        "firebase.project.id.emulator=fir-supershopcart-test"
})
public class ShopCartFirestoreTest {

    @Autowired
    private Firestore firestore; // Injected by Spring Boot due to @SpringBootTest

    private static CollectionReference shopCartsCollection;
    private static CollectionReference shoppersCollection;

    private static final String SHOPCARTS_COLLECTION_NAME = "shopCarts";
    private static final String SHOPPERS_COLLECTION_NAME = "shoppers";

    @BeforeAll
    void setup() { // Changed to non-static as Firestore is now @Autowired
        // Firestore is now injected, so we just set up collection references
        shopCartsCollection = firestore.collection(SHOPCARTS_COLLECTION_NAME);
        shoppersCollection = firestore.collection(SHOPPERS_COLLECTION_NAME);
    }

    @AfterAll
    static void teardown() throws Exception {
        // Clear collections directly as 'firestore' isn't static anymore here
        // The teardown will need to be adjusted if you want to clear the emulator
        // after all tests in the class (across multiple test classes).
        // For a single test class, @BeforeEach clear should suffice.
        // If Firestore instance needs to be closed, it would ideally be done by Spring context
        // or explicitly if manually managed. For @SpringBootTest, Spring manages its lifecycle.
    }

    @BeforeEach
    void clearCollectionsBeforeEach() throws ExecutionException, InterruptedException {
        clearCollection(shopCartsCollection);
        clearCollection(shoppersCollection);
    }

    private static void clearCollection(CollectionReference collection) throws ExecutionException, InterruptedException {
        List<DocumentReference> documents = collection.get().get().getDocuments().stream()
                .map(DocumentSnapshot::getReference).collect(Collectors.toList());
        for (DocumentReference docRef : documents) docRef.delete().get();
    }

    @Test
    @DisplayName("Should write a ShopCart and Shopper to Firestore and read them back")
    @Order(1)
    void testWriteAndReadShopCartAndShopper() throws ExecutionException, InterruptedException {

        Shopper originalShopper = new Shopper("john.doe@example.com", "John Doe");

        DocumentReference shopperDocRef = shoppersCollection.add(originalShopper).get();
        String shopperId = shopperDocRef.getId();
        originalShopper.setId(shopperId);

        ShopCart originalCart = new ShopCart();
        originalCart.setName("Groceries with Apples and Milk");
        originalCart.setDateKey("2025-07-30");
        originalCart.setItems(Arrays.asList(
                new GroceryItem("Organic Apples", "2 kg"),
                new GroceryItem("Whole Milk", "1 liter")
        ));
        originalCart.getShopperIds().add(shopperId);

        DocumentReference cartDocRef = shopCartsCollection.add(originalCart).get();
        String cartId = cartDocRef.getId();
        originalCart.setId(cartId);

        originalShopper.getShopCartIds().add(cartId);
        shoppersCollection.document(shopperId).set(originalShopper).get();

        DocumentSnapshot cartSnapshot = cartDocRef.get().get();
        ShopCart retrievedCart = cartSnapshot.toObject(ShopCart.class);
        if (retrievedCart != null) retrievedCart.setId(cartSnapshot.getId());

        assertNotNull(retrievedCart);
        assertEquals(originalCart.getId(), retrievedCart.getId());
        assertEquals(originalCart.getDateKey(), retrievedCart.getDateKey());
        assertEquals(originalCart.getItems().size(), retrievedCart.getItems().size());
        assertTrue(retrievedCart.getItems().containsAll(originalCart.getItems()));
        assertEquals(originalCart.getShopperIds(), retrievedCart.getShopperIds());
        assertNotNull(retrievedCart.getCreatedAt());

        DocumentSnapshot shopperSnapshot = shopperDocRef.get().get();
        Shopper retrievedShopper = shopperSnapshot.toObject(Shopper.class);
        if (retrievedShopper != null) retrievedShopper.setId(shopperSnapshot.getId());

        assertNotNull(retrievedShopper);
        assertEquals(originalShopper.getId(), retrievedShopper.getId());
        assertEquals(originalShopper.getEmail(), retrievedShopper.getEmail());
        assertEquals(originalShopper.getName(), retrievedShopper.getName());
        assertEquals(originalShopper.getShopCartIds(), retrievedShopper.getShopCartIds());
    }

    @Test
    @DisplayName("Should update an existing ShopCart in Firestore")
    @Order(2)
    void testUpdateShopCart() throws ExecutionException, InterruptedException {
        ShopCart initialCart = new ShopCart();
        initialCart.setName("Test Update Cart");
        initialCart.setDateKey("2025-07-29");
        initialCart.setItems(new ArrayList<>(List.of(new GroceryItem("Old Item", "1 pc"))));
        DocumentReference docRef = shopCartsCollection.add(initialCart).get();
        String documentId = docRef.getId();
        initialCart.setId(documentId);

        Thread.sleep(100);

        initialCart.getItems().add(new GroceryItem("New Item", "5 units"));
        initialCart.getItems().get(0).setPurchased(true);
        initialCart.setDateKey("2025-07-30_updated");

        shopCartsCollection.document(documentId).set(initialCart).get();

        DocumentSnapshot snapshot = docRef.get().get();
        ShopCart updatedCart = snapshot.toObject(ShopCart.class);
        if (updatedCart != null) updatedCart.setId(snapshot.getId());

        assertNotNull(updatedCart);
        assertEquals("2025-07-30_updated", updatedCart.getDateKey());
        assertEquals(2, updatedCart.getItems().size());
        assertTrue(updatedCart.getItems().contains(new GroceryItem("New Item", "5 units")));
        assertTrue(updatedCart.getItems().stream().anyMatch(item -> item.getDesignation().equals("Old Item") && item.isPurchased()));
    }

    @Test
    @DisplayName("Should delete a ShopCart from Firestore")
    @Order(3)
    void testDeleteShopCart() throws ExecutionException, InterruptedException {
        ShopCart cartToDelete = new ShopCart();
        cartToDelete.setName("To Delete");
        cartToDelete.setDateKey("2025-07-31");
        cartToDelete.setItems(new ArrayList<>());
        DocumentReference docRef = shopCartsCollection.add(cartToDelete).get();
        String documentId = docRef.getId();

        docRef.delete().get();

        DocumentSnapshot snapshot = docRef.get().get();
        assertFalse(snapshot.exists());
    }

    @Test
    @DisplayName("Should retrieve multiple ShopCarts from Firestore")
    @Order(4)
    void testRetrieveMultipleShopCarts() throws ExecutionException, InterruptedException {
        ShopCart cart1 = new ShopCart();
        cart1.setName("Cart 1");
        cart1.setDateKey("2025-07-30");
        cart1.setItems(new ArrayList<>(Collections.singletonList(new GroceryItem("Bread", "1 loaf"))));
        shopCartsCollection.add(cart1).get();

        ShopCart cart2 = new ShopCart();
        cart2.setName("Cart 2");
        cart2.setDateKey("2025-07-30");
        cart2.setItems(new ArrayList<>(Collections.singletonList(new GroceryItem("Cheese", "200g"))));
        shopCartsCollection.add(cart2).get();

        QuerySnapshot snapshot = shopCartsCollection.get().get();
        List<ShopCart> retrievedCarts = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            ShopCart cart = doc.toObject(ShopCart.class);
            if (cart != null) {
                cart.setId(doc.getId());
                retrievedCarts.add(cart);
            }
        }

        assertNotNull(retrievedCarts);
        assertEquals(2, retrievedCarts.size());
        assertTrue(retrievedCarts.stream().anyMatch(c -> c.getItems().stream().anyMatch(item -> item.getDesignation().equals("Bread"))));
        assertTrue(retrievedCarts.stream().anyMatch(c -> c.getItems().stream().anyMatch(item -> item.getDesignation().equals("Cheese"))));
    }

    @Test
    @DisplayName("Should update an existing Shopper in Firestore")
    @Order(5)
    void testUpdateShopper() throws ExecutionException, InterruptedException {
        Shopper initialShopper = new Shopper("jane.doe@example.com", "Jane Doe", "initialPass");
        initialShopper.setShopCartIds(new ArrayList<>());
        DocumentReference docRef = shoppersCollection.add(initialShopper).get();
        String shopperId = docRef.getId();
        initialShopper.setId(shopperId);

        initialShopper.setName("Jane Smith");
        initialShopper.setEmail("jane.smith@example.com");
        initialShopper.getShopCartIds().add("newCartId123");

        shoppersCollection.document(shopperId).set(initialShopper).get();

        DocumentSnapshot snapshot = docRef.get().get();
        Shopper updatedShopper = snapshot.toObject(Shopper.class);
        if (updatedShopper != null) updatedShopper.setId(snapshot.getId());

        assertNotNull(updatedShopper);
        assertEquals("Jane Smith", updatedShopper.getName());
        assertEquals("jane.smith@example.com", updatedShopper.getEmail());
        assertTrue(updatedShopper.getShopCartIds().contains("newCartId123"));
    }

    @Test
    @DisplayName("Should delete a Shopper from Firestore")
    @Order(6)
    void testDeleteShopper() throws ExecutionException, InterruptedException {
        Shopper shopperToDelete = new Shopper("test@example.com", "Test User", "deleteMe");
        shopperToDelete.setShopCartIds(new ArrayList<>());
        DocumentReference docRef = shoppersCollection.add(shopperToDelete).get();
        String shopperId = docRef.getId();

        docRef.delete().get();

        DocumentSnapshot snapshot = docRef.get().get();
        assertFalse(snapshot.exists());
    }
}
