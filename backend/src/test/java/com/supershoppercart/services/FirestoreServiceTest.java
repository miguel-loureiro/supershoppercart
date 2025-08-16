package com.supershoppercart.services; // This MUST be the first line (after comments)

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.supershoppercart.dtos.ShopCartDetailDTO;
import com.supershoppercart.enums.SharePermission;
import com.supershoppercart.enums.ShopCartState;
import com.supershoppercart.models.SharePermissionEntry;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.ExecutionException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.OngoingStubbing;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * Unit tests for FirestoreService using Mockito.
 * These tests do not require a running Firestore emulator.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Permite stubs desnecessários
public class FirestoreServiceTest {

    @Mock
    private Firestore firestore;
    @Mock
    private CollectionReference collectionReference;
    @Mock
    private ApiFuture<DocumentReference> documentReferenceFuture;
    @Mock
    private CollectionReference shoppersCollection;
    @Mock
    private CollectionReference shopcartsCollection;
    @Mock
    private DocumentReference documentReference;
    @Mock
    private ApiFuture<WriteResult> writeResultFuture;
    @Mock
    private ApiFuture<DocumentSnapshot> documentSnapshotFuture;
    @Mock
    private ApiFuture<QuerySnapshot> querySnapshotFuture;
    @Mock
    private DocumentSnapshot documentSnapshot;
    @Mock
    private DocumentSnapshot shopperDocumentSnapshot;
    @Mock
    private QuerySnapshot querySnapshot;
    @Mock
    private Query query;
    @Mock
    private DocumentReference cartDocRef;
    @Mock
    private DocumentReference shopperDocRef;
    @Mock
    private DocumentSnapshot cartDocumentSnapshot;
    @Mock
    private ApiFuture<QuerySnapshot> apiFutureQuerySnapshot;
    @Mock
    private ApiFuture<DocumentSnapshot> apiFutureDocumentSnapshot;
    @Mock
    private ApiFuture<Void> apiFutureVoid;
    @Mock
    private QueryDocumentSnapshot shopperQueryDocumentSnapshot;
    @Mock
    private FirestoreOptions mockFirestoreOptions;

    @InjectMocks
    private FirestoreService firestoreService;

    // Test data
    private Shopper testShopper;
    private ShopCart testShopCart;
    private static final String TEST_SHOPPER_ID = "shopper123";
    private static final String TEST_CART_ID = "cart123";
    private ShopCart shopCart;
    private Shopper shopper;
    private Shopper targetShopper;
    private String cartId = "cart123";
    private String requesterShopperId = "shopper1";
    private String targetEmail = "target@example.com";
    private String targetShopperId = "shopper2";
    private SharePermission permission = SharePermission.EDIT;

    @BeforeEach
    void setUp() {
        // Configuração geral para evitar repetição.
        // Mover mocks específicos para dentro dos testes individuais.
        testShopper = new Shopper(TEST_SHOPPER_ID, "test@example.com", "Test Shopper");
        testShopCart = new ShopCart();
        testShopCart.setId(TEST_CART_ID);
        testShopCart.setCreatedBy(TEST_SHOPPER_ID);
        testShopCart.setShopperIds(List.of(TEST_SHOPPER_ID));
        testShopCart.addOrUpdatePermission(TEST_SHOPPER_ID, SharePermission.ADMIN);

        // Inicializar objetos de dados
        shopCart = new ShopCart();
        shopCart.setId(cartId);
        shopCart.setCreatedBy(requesterShopperId);
        shopCart.setShopperIds(new ArrayList<>(Collections.singletonList(requesterShopperId)));
        shopCart.setSharePermissions(new ArrayList<>());
        shopCart.addOrUpdatePermission(requesterShopperId, SharePermission.ADMIN);

        shopper = new Shopper();
        shopper.setId(requesterShopperId);
        shopper.setEmail("requester@example.com");
        shopper.setName("Requester");

        targetShopper = new Shopper();
        targetShopper.setId(targetShopperId);
        targetShopper.setEmail(targetEmail);
        targetShopper.setName("Target");

        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(firestore.collection("shoppers")).thenReturn(shoppersCollection);
    }

    // --- Save Shopper Tests ---
    @Nested
    @DisplayName("saveShopper - Successful Scenarios")
    class SaveShopperSuccessTests {
        @Test
        @DisplayName("Should successfully save a shopper and return their ID")
        void saveShopper_shouldSaveAndReturnId() throws ExecutionException, InterruptedException {
            // Arrange
            // Garantir que o shopper tem um ID para passar na validação do serviço.
            testShopper.setId(TEST_SHOPPER_ID);

            when(firestore.collection("shoppers")).thenReturn(shoppersCollection);
            when(shoppersCollection.document(TEST_SHOPPER_ID)).thenReturn(documentReference);
            when(documentReference.set(testShopper)).thenReturn(writeResultFuture);
            when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));
            when(documentReference.getId()).thenReturn(TEST_SHOPPER_ID);

            // Act
            String savedId = firestoreService.saveShopper(testShopper);

            // Assert
            assertEquals(TEST_SHOPPER_ID, savedId);
            verify(documentReference, times(1)).set(testShopper);
        }
    }

    @Nested
    @DisplayName("saveShopper - Exception Scenarios")
    class SaveShopperExceptionTests {
        @Test
        @DisplayName("Should throw IllegalArgumentException when shopper object is null")
        void saveShopper_throwsException_onNullShopper() {
            assertThatThrownBy(() -> firestoreService.saveShopper(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Shopper ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when shopper ID is null, empty or blank")
        void saveShopper_throwsException_onNullOrEmptyId() {
            testShopper.setId(null);
            assertThatThrownBy(() -> firestoreService.saveShopper(testShopper))
                    .isInstanceOf(IllegalArgumentException.class);

            testShopper.setId("");
            assertThatThrownBy(() -> firestoreService.saveShopper(testShopper))
                    .isInstanceOf(IllegalArgumentException.class);

            testShopper.setId("   ");
            assertThatThrownBy(() -> firestoreService.saveShopper(testShopper))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw RuntimeException when Firestore operation fails due to ExecutionException")
        void saveShopper_throwsRuntimeException_onExecutionException() throws ExecutionException, InterruptedException {
            // Arrange
            // Garantir que o shopper tem um ID para passar na validação do serviço.
            testShopper.setId(TEST_SHOPPER_ID);
            when(firestore.collection("shoppers")).thenReturn(shoppersCollection);
            when(shoppersCollection.document(anyString())).thenReturn(documentReference);
            when(documentReference.set(any(Shopper.class))).thenReturn(writeResultFuture);
            when(writeResultFuture.get()).thenThrow(new ExecutionException("Simulated error", new RuntimeException()));

            // Act & Assert
            assertThatThrownBy(() -> firestoreService.saveShopper(testShopper))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error saving shopper");
        }

        @Test
        @DisplayName("Should throw RuntimeException when Firestore operation is interrupted")
        void saveShopper_throwsRuntimeException_onInterruptedException() throws ExecutionException, InterruptedException {
            // Arrange
            // Garantir que o shopper tem um ID para passar na validação do serviço.
            testShopper.setId(TEST_SHOPPER_ID);
            when(firestore.collection("shoppers")).thenReturn(shoppersCollection);
            when(shoppersCollection.document(anyString())).thenReturn(documentReference);
            when(documentReference.set(any(Shopper.class))).thenReturn(writeResultFuture);
            when(writeResultFuture.get()).thenThrow(new InterruptedException("Thread interrupted"));

            // Act & Assert
            assertThatThrownBy(() -> firestoreService.saveShopper(testShopper))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error saving shopper");
        }
    }

    // --- Save ShopCart Tests ---
    @Nested
    @DisplayName("saveShopCart - Scenarios")
    class SaveShopCartTests {
        @BeforeEach
        void setupMocks() {
            // This mocking sequence is now correct for .document(id).set(object)
            when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
            when(shopcartsCollection.document(anyString())).thenReturn(documentReference);
            when(documentReference.set(any(ShopCart.class))).thenReturn(writeResultFuture);
        }

        @Test
        @DisplayName("Should set default permissions and save a cart with a predefined ID")
        void saveShopCart_shouldSetPermissionsAndSave() throws ExecutionException, InterruptedException {
            // Arrange
            String creatorId = TEST_SHOPPER_ID;
            String sharedId = "sharedId456";
            testShopCart.setShopperIds(List.of(creatorId, sharedId));
            when(firestore.getOptions()).thenReturn(mockFirestoreOptions);
            when(mockFirestoreOptions.getProjectId()).thenReturn("test-project");
            // Mock the successful future result
            when(writeResultFuture.get()).thenReturn(null); // Return value of WriteResult doesn't matter here
            when(documentReference.getId()).thenReturn(TEST_CART_ID);

            // Act
            String savedId = firestoreService.saveShopCart(testShopCart);

            // Assert
            assertEquals(TEST_CART_ID, savedId);

            // Capture the argument passed to .set() to verify its state
            ArgumentCaptor<ShopCart> captor = ArgumentCaptor.forClass(ShopCart.class);
            verify(documentReference, times(1)).set(captor.capture());
            verify(shopcartsCollection, times(1)).document(TEST_CART_ID);

            ShopCart capturedCart = captor.getValue();
            assertEquals(SharePermission.ADMIN, capturedCart.getPermissionForShopper(creatorId));
            assertEquals(SharePermission.EDIT, capturedCart.getPermissionForShopper(sharedId));
        }

        @Test
        @DisplayName("Should throw RuntimeException when Firestore operation fails with ExecutionException")
        void saveShopCart_throwsRuntimeException_onExecutionException() throws ExecutionException, InterruptedException {
            // Arrange
            when(firestore.getOptions()).thenReturn(mockFirestoreOptions);
            when(mockFirestoreOptions.getProjectId()).thenReturn("test-project");
            // Mock the future to throw the exception
            Exception cause = new RuntimeException("DB error");
            when(writeResultFuture.get()).thenThrow(new ExecutionException(cause));

            // Act & Assert
            assertThatThrownBy(() -> firestoreService.saveShopCart(testShopCart))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Execution error while saving shop cart")
                    .hasCause(cause); // Assert the original cause is preserved
        }

        @Test
        @DisplayName("Should throw RuntimeException when InterruptedException occurs")
        void saveShopCart_throwsRuntimeException_onInterruptedException() throws ExecutionException, InterruptedException {
            // Arrange
            InterruptedException cause = new InterruptedException("Simulated interruption");
            when(firestore.getOptions()).thenReturn(mockFirestoreOptions);
            when(mockFirestoreOptions.getProjectId()).thenReturn("test-project");
            when(writeResultFuture.get()).thenThrow(cause);

            // Act & Assert
            assertThatThrownBy(() -> firestoreService.saveShopCart(testShopCart))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Interrupted while saving shop cart")
                    .hasCause(cause);
        }
    }

    // --- Get ShopCarts By Shopper Id Tests ---
    @Nested
    @DisplayName("getShopCartsByShopperId - Scenarios")
    class GetShopCartsByShopperIdTests {

        @Test
        @DisplayName("Should return a list of ShopCartDetailDTOs for a valid shopper")
        void getShopCartsByShopperId_shouldReturnListOfCarts() throws ExecutionException, InterruptedException {
            // Arrange
            Shopper shopper1 = new Shopper(TEST_SHOPPER_ID, "test1@example.com", "Test One");
            String sharedShopperId = "shopper456";
            Shopper shopper2 = new Shopper(sharedShopperId, "test2@example.com", "Test Two");
            ShopCart shopCart = new ShopCart();
            shopCart.setShopperIds(List.of(TEST_SHOPPER_ID, sharedShopperId));

            when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
            when(shopcartsCollection.whereArrayContains(anyString(), anyString())).thenReturn(query);
            when(query.orderBy(anyString(), any())).thenReturn(query);
            when(query.get()).thenReturn(querySnapshotFuture);

            QueryDocumentSnapshot cartDoc = mock(QueryDocumentSnapshot.class);
            when(querySnapshotFuture.get()).thenReturn(querySnapshot);
            when(querySnapshot.getDocuments()).thenReturn(List.of(cartDoc));
            when(cartDoc.toObject(ShopCart.class)).thenReturn(shopCart);
            when(cartDoc.getId()).thenReturn(TEST_CART_ID);

            // Mock the internal fetchShoppersByIds call
            FirestoreService spyService = spy(firestoreService);
            doReturn(List.of(shopper1, shopper2)).when(spyService).fetchShoppersByIds(anyList());

            // Act
            List<ShopCartDetailDTO> result = spyService.getShopCartsByShopperId(TEST_SHOPPER_ID);

            // Assert
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(TEST_CART_ID, result.get(0).getIdentifier());
            assertEquals(2, result.get(0).getShoppers().size());
            verify(spyService, times(1)).fetchShoppersByIds(anyList());
        }

        @Test
        @DisplayName("Should return an empty list if no carts are found")
        void getShopCartsByShopperId_shouldReturnEmptyList_whenNoCartsFound() throws ExecutionException, InterruptedException {
            // Arrange
            when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
            when(shopcartsCollection.whereArrayContains(anyString(), anyString())).thenReturn(query);
            when(query.orderBy(anyString(), any())).thenReturn(query);
            when(query.get()).thenReturn(querySnapshotFuture);
            when(querySnapshotFuture.get()).thenReturn(querySnapshot);
            when(querySnapshot.getDocuments()).thenReturn(Collections.emptyList());

            // Act
            List<ShopCartDetailDTO> result = firestoreService.getShopCartsByShopperId(TEST_SHOPPER_ID);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when shopper ID is null or empty")
        void getShopCartsByShopperId_throwsException_onNullOrEmptyId() {
            assertThatThrownBy(() -> firestoreService.getShopCartsByShopperId(null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> firestoreService.getShopCartsByShopperId(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw RuntimeException when Firestore operation fails")
        void getShopCartsByShopperId_throwsRuntimeException_onException() throws ExecutionException, InterruptedException {
            // Arrange
            when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
            when(shopcartsCollection.whereArrayContains(anyString(), anyString())).thenReturn(query);
            when(query.orderBy(anyString(), any())).thenReturn(query);
            when(query.get()).thenReturn(querySnapshotFuture);
            when(querySnapshotFuture.get()).thenThrow(new InterruptedException("Simulated interruption"));

            // Act & Assert
            assertThatThrownBy(() -> firestoreService.getShopCartsByShopperId(TEST_SHOPPER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error getting shop carts by shopper ID");
        }
    }

    @Test
    @DisplayName("shareCartWithShopper - should return true when cart is shared successfully")
    void shareCartWithShopper_shouldReturnTrue_whenSuccessful() throws Exception {
        ShopCart existingCart = new ShopCart();
        existingCart.setCreatedBy(TEST_SHOPPER_ID);
        existingCart.setShopperIds(new ArrayList<>(List.of(TEST_SHOPPER_ID)));
        existingCart.addOrUpdatePermission(TEST_SHOPPER_ID, SharePermission.ADMIN);

        Shopper targetShopper = new Shopper("target123", "target@example.com", "Target Shopper");

        DocumentSnapshot mockCartDoc = mock(DocumentSnapshot.class);
        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(mockCartDoc);
        when(mockCartDoc.exists()).thenReturn(true);
        when(mockCartDoc.toObject(ShopCart.class)).thenReturn(existingCart);

        CollectionReference shoppersCol = mock(CollectionReference.class);
        Query targetQuery = mock(Query.class);
        when(firestore.collection("shoppers")).thenReturn(shoppersCol);
        when(shoppersCol.whereEqualTo("email", "target@example.com")).thenReturn(targetQuery);
        when(targetQuery.limit(1)).thenReturn(targetQuery);
        when(targetQuery.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        QueryDocumentSnapshot targetDoc = mock(QueryDocumentSnapshot.class);
        when(querySnapshot.isEmpty()).thenReturn(false);
        when(querySnapshot.getDocuments()).thenReturn(List.of(targetDoc));
        when(targetDoc.toObject(Shopper.class)).thenReturn(targetShopper);

        ApiFuture<WriteResult> updateFuture = mock(ApiFuture.class);
        when(documentReference.update(anyMap())).thenReturn(updateFuture);
        when(updateFuture.get()).thenReturn(mock(WriteResult.class));

        boolean result = firestoreService.shareCartWithShopper(
                TEST_CART_ID, TEST_SHOPPER_ID, "target@example.com", SharePermission.EDIT);

        assertTrue(result);
    }

    @Test
    @DisplayName("getShopCartById - should return DTO when cart exists")
    void getShopCartById_shouldReturnDTO_whenExists() throws Exception {
        ShopCart cart = new ShopCart();
        cart.setShopperIds(List.of(TEST_SHOPPER_ID));
        cart.setCreatedBy(TEST_SHOPPER_ID);

        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(ShopCart.class)).thenReturn(cart);
        when(documentSnapshot.getId()).thenReturn(TEST_CART_ID);

        FirestoreService spyService = spy(firestoreService);
        doReturn(List.of(testShopper)).when(spyService).fetchShoppersByIds(anyList());

        ShopCartDetailDTO dto = spyService.getShopCartById(TEST_CART_ID);

        assertNotNull(dto);
        assertEquals(TEST_CART_ID, dto.getIdentifier());
        assertEquals(1, dto.getShoppers().size());
    }

    @Test
    @DisplayName("getShopCartById - should return null when cart does not exist")
    void getShopCartById_shouldReturnNull_whenNotExists() throws Exception {
        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        ShopCartDetailDTO dto = firestoreService.getShopCartById(TEST_CART_ID);

        assertNull(dto);
    }

    @Test
    @DisplayName("removeCartSharing - should return true when permission is removed")
    void removeCartSharing_shouldReturnTrue_whenRemoved() throws Exception {
        ShopCart cart = new ShopCart();
        cart.setCreatedBy(TEST_SHOPPER_ID);
        cart.setShopperIds(new ArrayList<>(List.of(TEST_SHOPPER_ID, "target123")));
        cart.addOrUpdatePermission(TEST_SHOPPER_ID, SharePermission.ADMIN);
        cart.addOrUpdatePermission("target123", SharePermission.EDIT);

        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(ShopCart.class)).thenReturn(cart);

        ApiFuture<WriteResult> updateFuture = mock(ApiFuture.class);
        when(documentReference.update(anyMap())).thenReturn(updateFuture);
        when(updateFuture.get()).thenReturn(mock(WriteResult.class));

        boolean result = firestoreService.removeCartSharing(TEST_CART_ID, TEST_SHOPPER_ID, "target123");

        assertTrue(result);
    }

    @Test
    @DisplayName("removeCartSharing - should return false when cart not found")
    void removeCartSharing_shouldReturnFalse_whenCartNotFound() throws Exception {
        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        boolean result = firestoreService.removeCartSharing(TEST_CART_ID, TEST_SHOPPER_ID, "target123");

        assertFalse(result);
    }

    @Test
    @DisplayName("shareCartWithShopper - should throw exception when Firestore get() fails")
    void shareCartWithShopper_shouldThrow_whenFirestoreFails() throws Exception {
        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);
        when(documentReference.get()).thenThrow(new RuntimeException("Firestore error"));

        assertThrows(RuntimeException.class, () ->
                firestoreService.shareCartWithShopper(
                        TEST_CART_ID, TEST_SHOPPER_ID, "target@example.com", SharePermission.EDIT)
        );
    }

    @Test
    @DisplayName("getShopCartById - should throw exception when Firestore call fails")
    void getShopCartById_shouldThrow_whenFirestoreFails() throws Exception {
        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);
        when(documentReference.get()).thenThrow(new RuntimeException("Firestore failure"));

        assertThrows(RuntimeException.class, () ->
                firestoreService.getShopCartById(TEST_CART_ID)
        );
    }

    @Test
    @DisplayName("removeCartSharing - should throw exception when Firestore update fails")
    void removeCartSharing_shouldThrow_whenUpdateFails() throws Exception {
        ShopCart cart = new ShopCart();
        cart.setCreatedBy(TEST_SHOPPER_ID);
        cart.setShopperIds(new ArrayList<>(List.of(TEST_SHOPPER_ID, "target123")));
        cart.addOrUpdatePermission(TEST_SHOPPER_ID, SharePermission.ADMIN);
        cart.addOrUpdatePermission("target123", SharePermission.EDIT);

        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(ShopCart.class)).thenReturn(cart);

        when(documentReference.update(anyMap())).thenThrow(new RuntimeException("Update failed"));

        assertThrows(RuntimeException.class, () ->
                firestoreService.removeCartSharing(TEST_CART_ID, TEST_SHOPPER_ID, "target123")
        );
    }

    @Test
    @DisplayName("getShopCartById - should throw RuntimeException when Firestore get() throws ExecutionException")
    void getShopCartById_shouldThrow_whenExecutionException() throws Exception {
        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);

        // Simulate ExecutionException during get()
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenThrow(new ExecutionException("failed", new RuntimeException()));

        assertThrows(RuntimeException.class, () -> firestoreService.getShopCartById(TEST_CART_ID));
    }

    @Test
    @DisplayName("getShopCartById - should throw RuntimeException when Firestore get() throws InterruptedException")
    void getShopCartById_shouldThrow_whenInterruptedException() throws Exception {
        when(firestore.collection("shopcarts")).thenReturn(shopcartsCollection);
        when(shopcartsCollection.document(TEST_CART_ID)).thenReturn(documentReference);

        // Simulate InterruptedException during get()
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenThrow(new InterruptedException("interrupted"));

        assertThrows(RuntimeException.class, () -> firestoreService.getShopCartById(TEST_CART_ID));
    }

    @Test
    void hydrateShopCartDTO_shouldReturnDTO_withShoppersPopulated() throws Exception {
        // Given
        shopCart.getShopperIds().add(targetShopperId);

        when(cartDocumentSnapshot.toObject(ShopCart.class)).thenReturn(shopCart);
        when(cartDocumentSnapshot.getId()).thenReturn(cartId);

        // Mocking the shoppers query
        Query shoppersQuery = mock(Query.class);
        when(shoppersCollection.whereIn(FieldPath.documentId(), shopCart.getShopperIds())).thenReturn(shoppersQuery);
        when(shoppersQuery.get()).thenReturn(apiFutureQuerySnapshot);
        when(apiFutureQuerySnapshot.get()).thenReturn(querySnapshot);

        // Mocking the result of the shoppers query
        when(querySnapshot.getDocuments()).thenReturn(List.of(shopperQueryDocumentSnapshot));

        when(shopperQueryDocumentSnapshot.toObject(Shopper.class)).thenReturn(shopper);

        // When
        ShopCartDetailDTO result = firestoreService.hydrateShopCartDTO(cartDocumentSnapshot);

        // Then
        assertNotNull(result);
        assertFalse(result.getShoppers().isEmpty()); // Esta asserção agora deve passar
        assertEquals(1, result.getShoppers().size());
        assertEquals(shopper.getName(), result.getShoppers().get(0).getName());
    }

    @Test
    void hydrateShopCartDTO_shouldReturnNull_whenCartDocumentIsNull() throws Exception {
        // Given
        when(documentSnapshot.toObject(ShopCart.class)).thenReturn(null);

        // When
        ShopCartDetailDTO result = firestoreService.hydrateShopCartDTO(documentSnapshot);

        // Then
        assertNull(result);
    }

    @Test
    void hydrateShopCartDTO_shouldReturnDTO_withEmptyShoppers_whenNoShoppersFound() throws Exception {
        // Given
        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(ShopCart.class)).thenReturn(shopCart);
        when(documentSnapshot.getId()).thenReturn(cartId);

        // Mocking the shoppers query to return no documents
        Query shoppersQuery = mock(Query.class);
        when(shoppersCollection.whereIn(FieldPath.documentId(), shopCart.getShopperIds())).thenReturn(shoppersQuery);
        when(shoppersQuery.get()).thenReturn(apiFutureQuerySnapshot);
        when(apiFutureQuerySnapshot.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(Collections.emptyList());

        // When
        ShopCartDetailDTO result = firestoreService.hydrateShopCartDTO(documentSnapshot);

        // Then
        assertNotNull(result);
        assertEquals(cartId, result.getIdentifier());
        assertTrue(result.getShoppers().isEmpty());
    }

    //------------------------------------------------------------------------------------------------------------------
    // Testes para o método public shareCartWithShopper()
    //------------------------------------------------------------------------------------------------------------------

    @Test
    void shareCartWithShopper_shouldShareCart_whenAllConditionsMet() throws ExecutionException, InterruptedException {
        // Given
        // Configurar o mock do cart document para simular um cart que existe
        when(shopcartsCollection.document(cartId)).thenReturn(cartDocRef);
        when(cartDocRef.get()).thenReturn(apiFutureDocumentSnapshot);
        when(apiFutureDocumentSnapshot.get()).thenReturn(cartDocumentSnapshot);
        when(cartDocumentSnapshot.exists()).thenReturn(true);
        when(cartDocumentSnapshot.toObject(ShopCart.class)).thenReturn(shopCart);

        // Mock do método privado findShopperByEmail para retornar um shopper
        FirestoreService mockFirestoreService = Mockito.spy(firestoreService);
        doReturn(targetShopper).when(mockFirestoreService).findShopperByEmail(targetEmail);
        doReturn(true).when(mockFirestoreService).canShareCart(any(ShopCart.class), anyString());

        // Mock da operação de update
        when(cartDocRef.update(any(Map.class))).thenReturn(apiFutureVoid);

        // When
        boolean result = mockFirestoreService.shareCartWithShopper(cartId, requesterShopperId, targetEmail, permission);

        // Then
        assertTrue(result);
        verify(cartDocRef).update(any(Map.class));
        assertTrue(shopCart.getShopperIds().contains(targetShopperId));
        assertEquals(SharePermission.EDIT, shopCart.getPermissionForShopper(targetShopperId));
    }

    @Test
    void shareCartWithShopper_shouldReturnFalse_whenCartNotFound() throws ExecutionException, InterruptedException {
        // Given
        when(shopcartsCollection.document(cartId)).thenReturn(cartDocRef);
        when(cartDocRef.get()).thenReturn(apiFutureDocumentSnapshot);
        when(apiFutureDocumentSnapshot.get()).thenReturn(cartDocumentSnapshot);
        when(documentSnapshot.exists()).thenReturn(false);

        // When
        boolean result = firestoreService.shareCartWithShopper(cartId, requesterShopperId, targetEmail, permission);

        // Then
        assertFalse(result);
        verify(cartDocRef, never()).update(any());
    }

    @Test
    void shareCartWithShopper_shouldReturnFalse_whenRequesterHasNoPermission() throws ExecutionException, InterruptedException {
        // Given
        when(shopcartsCollection.document(cartId)).thenReturn(cartDocRef);
        when(cartDocRef.get()).thenReturn(apiFutureDocumentSnapshot);
        when(apiFutureDocumentSnapshot.get()).thenReturn(cartDocumentSnapshot);
        when(cartDocumentSnapshot.exists()).thenReturn(true);
        when(cartDocumentSnapshot.toObject(ShopCart.class)).thenReturn(shopCart);

        // Mock do método privado canShareCart para retornar false
        FirestoreService mockFirestoreService = Mockito.spy(firestoreService);
        doReturn(false).when(mockFirestoreService).canShareCart(any(ShopCart.class), anyString());

        // When
        boolean result = mockFirestoreService.shareCartWithShopper(cartId, requesterShopperId, targetEmail, permission);

        // Then
        assertFalse(result);
        verify(cartDocRef, never()).update(any());
    }

    @Test
    void shareCartWithShopper_shouldReturnFalse_whenTargetShopperNotFound() throws ExecutionException, InterruptedException {
        // Given
        when(shopcartsCollection.document(cartId)).thenReturn(cartDocRef);
        when(cartDocRef.get()).thenReturn(apiFutureDocumentSnapshot);
        when(apiFutureDocumentSnapshot.get()).thenReturn(documentSnapshot);
        when(cartDocumentSnapshot.exists()).thenReturn(true);
        when(cartDocumentSnapshot.toObject(ShopCart.class)).thenReturn(shopCart);

        // Mock do método privado findShopperByEmail para retornar null
        FirestoreService mockFirestoreService = Mockito.spy(firestoreService);
        doReturn(null).when(mockFirestoreService).findShopperByEmail(targetEmail);
        doReturn(true).when(mockFirestoreService).canShareCart(any(ShopCart.class), anyString());

        // When
        boolean result = mockFirestoreService.shareCartWithShopper(cartId, requesterShopperId, targetEmail, permission);

        // Then
        assertFalse(result);
        verify(cartDocRef, never()).update(any());
    }

    //------------------------------------------------------------------------------------------------------------------
    // Testes de exceção para shareCartWithShopper()
    //------------------------------------------------------------------------------------------------------------------

    @Test
    void shareCartWithShopper_shouldThrowIllegalArgumentException_whenAnyParameterIsNull() {
        assertThrows(IllegalArgumentException.class, () -> firestoreService.shareCartWithShopper(null, requesterShopperId, targetEmail, permission));
        assertThrows(IllegalArgumentException.class, () -> firestoreService.shareCartWithShopper(cartId, null, targetEmail, permission));
        assertThrows(IllegalArgumentException.class, () -> firestoreService.shareCartWithShopper(cartId, requesterShopperId, null, permission));
        assertThrows(IllegalArgumentException.class, () -> firestoreService.shareCartWithShopper(cartId, requesterShopperId, targetEmail, null));
    }

    @Test
    void shareCartWithShopper_shouldThrowRuntimeException_onInterruptedException() throws ExecutionException, InterruptedException {
        // Given
        when(shopcartsCollection.document(cartId)).thenReturn(cartDocRef);
        when(cartDocRef.get()).thenReturn(apiFutureDocumentSnapshot);
        OngoingStubbing<DocumentSnapshot> stubbing = when(apiFutureDocumentSnapshot.get());
        stubbing.thenThrow(new InterruptedException("Simulated interruption"));

        // When & Then
        assertThrows(RuntimeException.class, () -> firestoreService.shareCartWithShopper(cartId, requesterShopperId, targetEmail, permission));
        verify(cartDocRef, never()).update(any());
    }

    @Test
    void shareCartWithShopper_shouldThrowRuntimeException_onExecutionException() throws ExecutionException, InterruptedException {
        // Given
        when(shopcartsCollection.document(cartId)).thenReturn(cartDocRef);
        when(cartDocRef.get()).thenReturn(apiFutureDocumentSnapshot);
        OngoingStubbing<DocumentSnapshot> stubbing = when(apiFutureDocumentSnapshot.get());
        stubbing.thenThrow(new ExecutionException("Simulated execution error", new Exception()));

        // When & Then
        assertThrows(RuntimeException.class, () -> firestoreService.shareCartWithShopper(cartId, requesterShopperId, targetEmail, permission));
        verify(cartDocRef, never()).update(any());
    }
}