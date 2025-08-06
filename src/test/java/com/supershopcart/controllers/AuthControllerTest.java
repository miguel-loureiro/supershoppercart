package com.supershopcart.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.supershopcart.models.RefreshToken;
import com.supershopcart.models.Shopper;
import com.supershopcart.repositories.ShopperRepository;
import com.supershopcart.security.GoogleTokenVerifier;
import com.supershopcart.services.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private ShopperRepository shopperRepository;

    @Mock
    private Firestore firestore;

    @InjectMocks
    private AuthController authController;

    private final String validIdToken = "valid-google-id-token";
    private final String deviceId = "device-123";
    private final String email = "test@example.com";
    private final String name = "Test User";

    private GoogleIdToken.Payload mockPayload;

    @Test
    void testLoginWithGoogle_MissingAuthHeader() throws Exception {
        ResponseEntity<?> response = authController.loginWithGoogle(null, deviceId);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Missing or invalid Authorization header", response.getBody());
    }

    @Test
    void testLoginWithGoogle_InvalidAuthHeader() throws Exception {
        ResponseEntity<?> response = authController.loginWithGoogle("InvalidFormat " + validIdToken, deviceId);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Missing or invalid Authorization header", response.getBody());
    }

    @Test
    void testLoginWithGoogle_MissingDeviceId() throws Exception {
        ResponseEntity<?> response = authController.loginWithGoogle("Bearer " + validIdToken, null);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Missing X-Device-Id header", response.getBody());
    }

    @Test
    void testLoginWithGoogle_BlankDeviceId() throws Exception {
        ResponseEntity<?> response = authController.loginWithGoogle("Bearer " + validIdToken, "");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Missing X-Device-Id header", response.getBody());
    }

    @Test
    void testLoginWithGoogle_InvalidGoogleToken() throws Exception {
        when(googleTokenVerifier.verify(validIdToken)).thenReturn(null);

        ResponseEntity<?> response = authController.loginWithGoogle("Bearer " + validIdToken, deviceId);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid Google token", response.getBody());
    }

    @Test
    void testLoginWithGoogle_ExistingShopper_Success() throws Exception {
        // Setup mocks for this specific test
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        when(mockPayload.getEmail()).thenReturn(email);
        when(mockPayload.get("name")).thenReturn(name);

        Shopper mockShopper = new Shopper(email, name);
        mockShopper.setId("shopper123");

        // Step 1: Mock the Google token verification
        when(googleTokenVerifier.verify(validIdToken)).thenReturn(mockPayload);

        // Step 2: Mock the shopper repository to find an existing shopper
        when(shopperRepository.findByEmail(email)).thenReturn(Optional.of(mockShopper));

        // Step 3: Mock the JWT token service to return valid tokens
        when(jwtTokenService.generateAccessToken(mockShopper.getId(), deviceId)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(mockShopper.getId())).thenReturn("refresh-token");
        when(jwtTokenService.getRefreshTokenExpiration()).thenReturn(7 * 24 * 60 * 60 * 1000L);

        // Step 4: Mock the Firestore calls for saving the refresh token
        CollectionReference collectionRef = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<WriteResult> future = ApiFutures.immediateFuture(mock(WriteResult.class));

        when(firestore.collection("refresh_tokens")).thenReturn(collectionRef);
        when(collectionRef.document("refresh-token")).thenReturn(docRef);
        when(docRef.set(any(RefreshToken.class))).thenReturn(future);

        // Act
        ResponseEntity<?> response = authController.loginWithGoogle("Bearer " + validIdToken, deviceId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("access-token", body.get("accessToken"));
        assertEquals("refresh-token", body.get("refreshToken"));

        // Verify interactions
        verify(googleTokenVerifier).verify(validIdToken);
        verify(shopperRepository).findByEmail(email);
        verify(jwtTokenService).generateAccessToken(mockShopper.getId(), deviceId);
        verify(jwtTokenService).generateRefreshToken(mockShopper.getId());
        verify(docRef).set(any(RefreshToken.class));

        // Verify shopper was not saved (already existed)
        verify(shopperRepository, never()).save(any());
    }

    @Test
    void testLoginWithGoogle_NewShopper_Success() throws Exception {
        // Setup mocks for this specific test
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        when(mockPayload.getEmail()).thenReturn(email);
        when(mockPayload.get("name")).thenReturn(name);

        Shopper mockShopper = new Shopper(email, name);
        mockShopper.setId("shopper123");

        when(googleTokenVerifier.verify(validIdToken)).thenReturn(mockPayload);
        when(shopperRepository.findByEmail(email)).thenReturn(Optional.empty());

        Shopper newShopper = new Shopper(email, name);
        newShopper.setId("new-shopper-id");

        when(shopperRepository.save(any(Shopper.class))).thenReturn(newShopper);
        when(jwtTokenService.generateAccessToken(newShopper.getId(), deviceId)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(newShopper.getId())).thenReturn("refresh-token");
        when(jwtTokenService.getRefreshTokenExpiration()).thenReturn(7 * 24 * 60 * 60 * 1000L);

        // Mock Firestore operations
        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<WriteResult> future = mock(ApiFuture.class);
        WriteResult writeResult = mock(WriteResult.class);

        when(firestore.collection("refresh_tokens")).thenReturn(collection);
        when(collection.document("refresh-token")).thenReturn(docRef);
        when(docRef.set(any(RefreshToken.class))).thenReturn(future);
        when(future.get()).thenReturn(writeResult);

        ResponseEntity<?> response = authController.loginWithGoogle("Bearer " + validIdToken, deviceId);

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("access-token", body.get("accessToken"));
        assertEquals("refresh-token", body.get("refreshToken"));

        // Verify new shopper was created
        verify(shopperRepository).save(argThat(shopper ->
                shopper.getEmail().equals(email) && shopper.getName().equals(name)));
        verify(shopperRepository, never()).deleteById(any());
    }

    @Test
    void testLoginWithGoogle_NewShopper_ShopperSaveFails() throws Exception {
        // Setup mocks for this specific test
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        when(mockPayload.getEmail()).thenReturn(email);
        when(mockPayload.get("name")).thenReturn(name);

        Shopper mockShopper = new Shopper(email, name);
        mockShopper.setId("shopper123");

        when(googleTokenVerifier.verify(validIdToken)).thenReturn(mockPayload);
        when(shopperRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(shopperRepository.save(any(Shopper.class)))
                .thenThrow(new RuntimeException("Database save failed"));

        ResponseEntity<?> response = authController.loginWithGoogle("Bearer " + validIdToken, deviceId);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().toString().startsWith("Failed to create new shopper:"));

        // Verify no token generation attempted
        verify(jwtTokenService, never()).generateAccessToken(any(), any());
        verify(jwtTokenService, never()).generateRefreshToken(anyString());
    }

    @Test
    void testLoginWithGoogle_NewShopper_FirestoreFails() throws Exception {
        // --- Arrange: Data values for this test
        String email = "foo@example.com";
        String name = "Foo Bar";
        String validIdToken = "valid-id-token";
        String deviceId = "test-device-123";

        // Mock Google payload
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        when(googleTokenVerifier.verify(validIdToken)).thenReturn(mockPayload);
        when(mockPayload.getEmail()).thenReturn(email);
        when(mockPayload.get("name")).thenReturn(name);

        // Shopper repository returns no shopper (this is a new registration)
        when(shopperRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Save returns the newly created shopper
        Shopper newShopper = new Shopper(email, name);
        newShopper.setId("new-shopper-id");
        when(shopperRepository.save(any(Shopper.class))).thenReturn(newShopper);

        // JWT token service returns fake tokens & expiration
        when(jwtTokenService.generateAccessToken(newShopper.getId(), deviceId)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(newShopper.getId())).thenReturn("refresh-token");
        when(jwtTokenService.getRefreshTokenExpiration()).thenReturn(7 * 24 * 60 * 60 * 1000L);

        // Mock Firestore write to fail
        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<WriteResult> future = mock(ApiFuture.class);
        when(firestore.collection("refresh_tokens")).thenReturn(collection);
        when(collection.document("refresh-token")).thenReturn(docRef);
        when(docRef.set(any(RefreshToken.class))).thenReturn(future);
        when(future.get()).thenThrow(new RuntimeException("Firestore write failed"));

        // --- Act
        ResponseEntity<?> response = authController.loginWithGoogle(
                "Bearer " + validIdToken, deviceId);

        // --- Assert
        assertEquals(500, response.getStatusCode().value());
        String body = String.valueOf(response.getBody());
        assertTrue(body.startsWith("Failed to complete login:"), "Body: " + body);
        // Ensure the cleanup happened!
        verify(shopperRepository).deleteById(newShopper.getId());
    }

    @Test
    void testLoginWithGoogle_ExistingShopper_FirestoreFails() throws Exception {

        // Setup mock payload inside this test
        mockPayload = mock(GoogleIdToken.Payload.class);
        when(mockPayload.getEmail()).thenReturn(email);
        when(mockPayload.get("name")).thenReturn(name);

        when(googleTokenVerifier.verify(validIdToken)).thenReturn(mockPayload);
        when(shopperRepository.findByEmail(email)).thenReturn(Optional.empty());

        Shopper shopper = new Shopper(email, name);
        shopper.setId("shopper123");

        when(googleTokenVerifier.verify(validIdToken)).thenReturn(mockPayload);
        when(shopperRepository.findByEmail(email)).thenReturn(Optional.of(shopper));
        when(jwtTokenService.generateAccessToken(shopper.getId(), deviceId)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(shopper.getId())).thenReturn("refresh-token");
        when(jwtTokenService.getRefreshTokenExpiration()).thenReturn(7 * 24 * 60 * 60 * 1000L);

        // Mock Firestore operations to fail
        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<WriteResult> future = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(collection);
        when(collection.document("refresh-token")).thenReturn(docRef);
        when(docRef.set(any(RefreshToken.class))).thenReturn(future);
        when(future.get()).thenThrow(new RuntimeException("Firestore write failed"));

        ResponseEntity<?> response = authController.loginWithGoogle("Bearer " + validIdToken, deviceId);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().toString().startsWith("Failed to complete login:"));

        // Verify no cleanup for existing shopper
        verify(shopperRepository, never()).deleteById(any());
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        String refreshToken = "refresh-token";
        String shopperId = "shopper-123";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        Map<String, String> requestBody = Map.of(
                "refreshToken", refreshToken,
                "deviceId", deviceId
        );

        // Mock existing refresh token
        RefreshToken storedToken = new RefreshToken(
                refreshToken,
                shopperId,
                deviceId,
                System.currentTimeMillis() + 1000000 // Future expiry
        );

        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> getFuture = mock(ApiFuture.class);
        DocumentSnapshot docSnapshot = mock(DocumentSnapshot.class);
        ApiFuture<WriteResult> deleteFuture = mock(ApiFuture.class);
        ApiFuture<WriteResult> setFuture = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(collection);
        when(collection.document(refreshToken)).thenReturn(docRef);
        when(docRef.get()).thenReturn(getFuture);
        when(getFuture.get()).thenReturn(docSnapshot);
        when(docSnapshot.exists()).thenReturn(true);
        when(docSnapshot.toObject(RefreshToken.class)).thenReturn(storedToken);
        when(docRef.delete()).thenReturn(deleteFuture);
        when(deleteFuture.get()).thenReturn(null);

        when(jwtTokenService.generateAccessToken(shopperId, deviceId)).thenReturn(newAccessToken);
        when(jwtTokenService.generateRefreshToken(shopperId)).thenReturn(newRefreshToken);
        when(jwtTokenService.getRefreshTokenExpiration()).thenReturn(7 * 24 * 60 * 60 * 1000L);

        DocumentReference newDocRef = mock(DocumentReference.class);
        when(collection.document(newRefreshToken)).thenReturn(newDocRef);
        when(newDocRef.set(any(RefreshToken.class))).thenReturn(setFuture);
        when(setFuture.get()).thenReturn(null);

        ResponseEntity<?> response = authController.refreshToken(requestBody);

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals(newAccessToken, body.get("accessToken"));
        assertEquals(newRefreshToken, body.get("refreshToken"));

        verify(docRef).delete();
        verify(newDocRef).set(any(RefreshToken.class));
    }

    @Test
    void testRefreshToken_MissingParameters() throws Exception {
        Map<String, String> requestBody = Map.of("refreshToken", "token");

        ResponseEntity<?> response = authController.refreshToken(requestBody);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Missing refreshToken or deviceId", response.getBody());
    }

    @Test
    void testRefreshToken_TokenNotFound() throws Exception {
        Map<String, String> requestBody = Map.of(
                "refreshToken", "invalid-token",
                "deviceId", deviceId
        );

        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> getFuture = mock(ApiFuture.class);
        DocumentSnapshot docSnapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("refresh_tokens")).thenReturn(collection);
        when(collection.document("invalid-token")).thenReturn(docRef);
        when(docRef.get()).thenReturn(getFuture);
        when(getFuture.get()).thenReturn(docSnapshot);
        when(docSnapshot.exists()).thenReturn(false);

        ResponseEntity<?> response = authController.refreshToken(requestBody);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid refresh token", response.getBody());
    }

    @Test
    void testRefreshToken_ExpiredToken() throws Exception {
        String refreshToken = "expired-token";
        Map<String, String> requestBody = Map.of(
                "refreshToken", refreshToken,
                "deviceId", deviceId
        );

        RefreshToken expiredToken = new RefreshToken(
                refreshToken,
                "shopper-123",
                deviceId,
                System.currentTimeMillis() - 1000 // Past expiry
        );

        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> getFuture = mock(ApiFuture.class);
        DocumentSnapshot docSnapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("refresh_tokens")).thenReturn(collection);
        when(collection.document(refreshToken)).thenReturn(docRef);
        when(docRef.get()).thenReturn(getFuture);
        when(getFuture.get()).thenReturn(docSnapshot);
        when(docSnapshot.exists()).thenReturn(true);
        when(docSnapshot.toObject(RefreshToken.class)).thenReturn(expiredToken);

        ResponseEntity<?> response = authController.refreshToken(requestBody);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Refresh token expired or device mismatch", response.getBody());
    }

    @Test
    void testLogout_Success() throws Exception {
        String refreshToken = "refresh-token";
        Map<String, String> requestBody = Map.of(
                "refreshToken", refreshToken,
                "deviceId", deviceId
        );

        RefreshToken storedToken = new RefreshToken(refreshToken, "shopper-123", deviceId, System.currentTimeMillis() + 1000000);

        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> getFuture = mock(ApiFuture.class);
        DocumentSnapshot docSnapshot = mock(DocumentSnapshot.class);
        ApiFuture<WriteResult> deleteFuture = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(collection);
        when(collection.document(refreshToken)).thenReturn(docRef);
        when(docRef.get()).thenReturn(getFuture);
        when(getFuture.get()).thenReturn(docSnapshot);
        when(docSnapshot.exists()).thenReturn(true);
        when(docSnapshot.toObject(RefreshToken.class)).thenReturn(storedToken);
        when(docRef.delete()).thenReturn(deleteFuture);
        when(deleteFuture.get()).thenReturn(null);

        ResponseEntity<?> response = authController.logout(requestBody);

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Logged out from device", body.get("message"));

        verify(docRef).delete();
    }

    @Test
    void testLogout_InvalidRequest() throws Exception {
        Map<String, String> requestBody = Map.of(
                "refreshToken", "token",
                "deviceId", "wrong-device"
        );

        RefreshToken storedToken = new RefreshToken("token", "shopper-123", deviceId, System.currentTimeMillis() + 1000000);

        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> getFuture = mock(ApiFuture.class);
        DocumentSnapshot docSnapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("refresh_tokens")).thenReturn(collection);
        when(collection.document("token")).thenReturn(docRef);
        when(docRef.get()).thenReturn(getFuture);
        when(getFuture.get()).thenReturn(docSnapshot);
        when(docSnapshot.exists()).thenReturn(true);
        when(docSnapshot.toObject(RefreshToken.class)).thenReturn(storedToken);

        ResponseEntity<?> response = authController.logout(requestBody);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid logout request", response.getBody());

        verify(docRef, never()).delete();
    }

    @Test
    void testLogoutAllDevices_Success() throws Exception {
        String shopperId = "shopper-123";
        Map<String, String> requestBody = Map.of("shopperId", shopperId);

        CollectionReference collection = mock(CollectionReference.class);
        Query query = mock(Query.class);
        ApiFuture<QuerySnapshot> queryFuture = mock(ApiFuture.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);
        DocumentReference docRef1 = mock(DocumentReference.class);
        DocumentReference docRef2 = mock(DocumentReference.class);
        ApiFuture<WriteResult> deleteFuture1 = mock(ApiFuture.class);
        ApiFuture<WriteResult> deleteFuture2 = mock(ApiFuture.class);

        when(firestore.collection("refresh_tokens")).thenReturn(collection);
        when(collection.whereEqualTo("shopperId", shopperId)).thenReturn(query);
        when(query.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(doc1, doc2));
        when(doc1.getReference()).thenReturn(docRef1);
        when(doc2.getReference()).thenReturn(docRef2);
        when(docRef1.delete()).thenReturn(deleteFuture1);
        when(docRef2.delete()).thenReturn(deleteFuture2);
        when(deleteFuture1.get()).thenReturn(null);
        when(deleteFuture2.get()).thenReturn(null);

        ResponseEntity<?> response = authController.logoutAllDevices(requestBody);

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Logged out from all devices", body.get("message"));

        verify(docRef1).delete();
        verify(docRef2).delete();
    }

    @Test
    void testLogoutAllDevices_MissingShopperId() throws Exception {
        Map<String, String> requestBody = Map.of();

        ResponseEntity<?> response = authController.logoutAllDevices(requestBody);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Missing shopperId", response.getBody());
    }
}
