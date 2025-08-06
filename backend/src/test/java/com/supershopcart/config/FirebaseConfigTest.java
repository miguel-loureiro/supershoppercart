package com.supershopcart.config;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("FirebaseConfig Unit Tests")
class FirebaseConfigTest {

    @Mock
    private ResourceLoader resourceLoader;

    @InjectMocks
    private FirebaseConfig firebaseConfig; // This will inject resourceLoader

    // Private fields from FirebaseConfig to set via reflection
    private String emulatorProjectId = "test-emulator-project";
    private String emulatorHost = "localhost:8081";
    private String productionProjectId = "test-prod-project";
    private String serviceAccountPath = "classpath:test-service-account.json";

    @BeforeEach
    void setUp() {
        // Inject @Value properties using ReflectionTestUtils
        ReflectionTestUtils.setField(firebaseConfig, "emulatorProjectIdProp", emulatorProjectId);
        ReflectionTestUtils.setField(firebaseConfig, "emulatorHostProp", emulatorHost);
        ReflectionTestUtils.setField(firebaseConfig, "productionProjectIdProp", productionProjectId);
        ReflectionTestUtils.setField(firebaseConfig, "serviceAccountPathProp", serviceAccountPath);

        // Reset FirebaseApp state for each test if necessary (for static mocks)
        // This is tricky. For unit tests, we usually don't want to rely on global state.
        // If FirebaseApp.initializeApp is called in a test, it might affect others.
        // Using Mockito.mockStatic for FirebaseApp will help control this.
    }

    // --- Tests for firestoreEmulator() ---

    @Test
    @DisplayName("firestoreEmulator should throw IllegalStateException if direct connection test fails")
    void firestoreEmulator_directConnectionFails_throwsIllegalStateException() throws Exception {
        // Arrange
        // Mock FirestoreOptions.newBuilder() to return a mock builder
        // Then mock the build().getService() chain to return a mock Firestore
        // This mock Firestore will then be configured to throw an exception when its methods are called
        // by testDirectFirestoreConnection.

        // Create mocks for the Firestore chain that testDirectFirestoreConnection uses
        Firestore mockFirestoreForConnectionTest = mock(Firestore.class);
        CollectionReference mockCollectionRef = mock(CollectionReference.class);
        DocumentReference mockDocRef = mock(DocumentReference.class);
        ApiFuture<WriteResult> mockWriteFuture = mock(ApiFuture.class);
        ApiFuture<DocumentSnapshot> mockReadFuture = mock(ApiFuture.class);

        // Simulate a failure in the direct connection test (e.g., write fails)
        when(mockFirestoreForConnectionTest.collection(anyString())).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(anyString())).thenReturn(mockDocRef);
        when(mockDocRef.set(any(Map.class))).thenReturn(mockWriteFuture);
        when(mockWriteFuture.get()).thenThrow(new ExecutionException("Connection refused", new IOException("Failed to connect"))); // Simulate connection failure

        // Use Mockito.mockStatic to mock the static newBuilder() method of FirestoreOptions
        // and the static getApps() and initializeApp() methods of FirebaseApp
        try (MockedStatic<FirestoreOptions> mockedFirestoreOptions = mockStatic(FirestoreOptions.class);
             MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);

            mockedFirestoreOptions.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.setProjectId(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setEmulatorHost(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setCredentials(any(GoogleCredentials.class))).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestoreForConnectionTest); // This is the key: return our controlled mock Firestore

            // Mock FirebaseApp.getApps() to simulate no app initialized initially
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            // Act & Assert
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                    firebaseConfig.firestoreEmulator()
            );

            assertTrue(thrown.getMessage().contains("Could not create Firestore client for emulator"));
            verify(mockFirestoreForConnectionTest, times(1)).close(); // Ensure close is called on failure
        }
    }

    @Test
    @DisplayName("firestoreEmulator should initialize successfully with emulator host")
    void firestoreEmulator_success() throws Exception {
        // Arrange
        Firestore mockFirestore = mock(Firestore.class); // This will be the final Firestore bean
        CollectionReference mockCollectionRef = mock(CollectionReference.class);
        DocumentReference mockDocRef = mock(DocumentReference.class);
        ApiFuture<WriteResult> mockWriteFuture = mock(ApiFuture.class);
        ApiFuture<DocumentSnapshot> mockReadFuture = mock(ApiFuture.class);

        // Simulate successful direct connection test
        when(mockFirestore.collection(anyString())).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(anyString())).thenReturn(mockDocRef);
        when(mockDocRef.set(any(Map.class))).thenReturn(mockWriteFuture);
        when(mockWriteFuture.get()).thenReturn(mock(WriteResult.class)); // Successful write
        when(mockDocRef.get()).thenReturn(mockReadFuture);
        when(mockReadFuture.get()).thenReturn(mock(DocumentSnapshot.class)); // Successful read
        when(mockReadFuture.get().exists()).thenReturn(true);


        try (MockedStatic<FirestoreOptions> mockedFirestoreOptions = mockStatic(FirestoreOptions.class);
             MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);

            mockedFirestoreOptions.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.setProjectId(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setEmulatorHost(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setCredentials(any(GoogleCredentials.class))).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestore); // Return the mock Firestore instance
            when(mockOptions.getProjectId()).thenReturn(emulatorProjectId);
            when(mockOptions.getHost()).thenReturn("localhost:8081");
            when(mockOptions.getEmulatorHost()).thenReturn(emulatorHost);

            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList()); // Simulate no app initialized

            // Act
            Firestore resultFirestore = firebaseConfig.firestoreEmulator();

            // Assert
            assertNotNull(resultFirestore);
            assertEquals(mockFirestore, resultFirestore); // Should return the mocked Firestore instance
            verify(mockFirestore, times(1)).close(); // Close is called at the end of testDirectFirestoreConnection
        }
    }

    // --- Tests for firestoreProduction() ---

    @Test
    @DisplayName("firestoreProduction should throw IOException if service account file not found")
    void firestoreProduction_serviceAccountNotFound_throwsIOException() throws IOException {
        // Arrange
        Resource mockResource = mock(Resource.class);
        when(resourceLoader.getResource(serviceAccountPath)).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(false); // Simulate file not found

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            // Act & Assert
            IOException thrown = assertThrows(IOException.class, () ->
                    firebaseConfig.firestoreProduction()
            );

            assertTrue(thrown.getMessage().contains("Firebase service account file not found"));
        }
    }

    @Test
    @DisplayName("firestoreProduction should throw IOException if service account input stream fails")
    void firestoreProduction_serviceAccountStreamFails_throwsIOException() throws IOException {
        // Arrange
        Resource mockResource = mock(Resource.class);
        when(resourceLoader.getResource(serviceAccountPath)).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenThrow(new IOException("Mock stream read error"));

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            // Act & Assert
            IOException thrown = assertThrows(IOException.class, () ->
                    firebaseConfig.firestoreProduction()
            );

            // Test the message from the IOException (the cause)
            assertTrue(thrown.getMessage().contains("Mock stream read error"));
        }
    }

    @Test
    @DisplayName("firestoreProduction should initialize successfully with mocked GoogleCredentials")
    void firestoreProduction_successWithMockedCredentials() throws IOException {
        // Arrange
        Resource mockResource = mock(Resource.class);
        when(resourceLoader.getResource(serviceAccountPath)).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);

        // You can provide any dummy InputStream; it will not be parsed because fromStream is mocked
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream("invalid but ignored".getBytes()));

        try (MockedStatic<GoogleCredentials> mockedCredentials = mockStatic(GoogleCredentials.class);
             MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class);
             MockedStatic<FirestoreOptions> mockedFirestoreOptions = mockStatic(FirestoreOptions.class)) {

            GoogleCredentials mockCreds = mock(GoogleCredentials.class);
            mockedCredentials.when(() -> GoogleCredentials.fromStream(any(InputStream.class))).thenReturn(mockCreds);

            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList()); // No app initialized

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);
            Firestore mockFirestore = mock(Firestore.class);

            mockedFirestoreOptions.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.setProjectId(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestore);
            when(mockOptions.getProjectId()).thenReturn("test-project");
            when(mockOptions.getHost()).thenReturn("firestore.googleapis.com:443");

            // Act
            Firestore resultFirestore = firebaseConfig.firestoreProduction();

            // Assert
            assertNotNull(resultFirestore);
            assertEquals(mockFirestore, resultFirestore);
            mockedFirebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), times(1));
        }
    }
}