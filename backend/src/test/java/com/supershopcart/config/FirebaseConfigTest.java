package com.supershopcart.config;

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
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FirebaseConfig Unit Tests")
class FirebaseConfigTest {

    @Mock
    private ResourceLoader resourceLoader;

    @InjectMocks
    private FirebaseConfig firebaseConfig;

    private String emulatorProjectId = "test-emulator-project";
    private String emulatorHost = "localhost:8081";
    private String productionProjectId = "test-prod-project";
    private String serviceAccountPath = "classpath:test-service-account.json";
    private String serviceAccountB64 = "dummy-base64-string";

    @BeforeEach
    void setUp() {
        // Inject @Value properties using ReflectionTestUtils
        ReflectionTestUtils.setField(firebaseConfig, "emulatorProjectIdProp", emulatorProjectId);
        ReflectionTestUtils.setField(firebaseConfig, "emulatorHostProp", emulatorHost);
        ReflectionTestUtils.setField(firebaseConfig, "productionProjectIdProp", productionProjectId);
        ReflectionTestUtils.setField(firebaseConfig, "serviceAccountPathProp", serviceAccountPath);
        // By default, set the B64 string to be non-blank for some tests
        ReflectionTestUtils.setField(firebaseConfig, "serviceAccountB64", serviceAccountB64);
    }

    // --- Tests for firestoreEmulator() ---

    @Test
    @DisplayName("firestoreEmulator should throw IllegalStateException on initialization failure")
    void firestoreEmulator_initializationFails_throwsIllegalStateException() throws Exception {
        // Arrange
        try (MockedStatic<FirestoreOptions> mockedFirestoreOptions = mockStatic(FirestoreOptions.class);
             MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class);
            mockedFirestoreOptions.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.setProjectId(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setEmulatorHost(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setCredentials(any(GoogleCredentials.class))).thenReturn(mockBuilder);

            // Simulate a failure during the build process
            when(mockBuilder.build()).thenThrow(new IllegalStateException("Mock build failure"));

            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            // Act & Assert
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                    firebaseConfig.firestoreEmulator()
            );

            assertTrue(thrown.getMessage().contains("Mock build failure"));
            // The close() call is no longer performed by the method under test, so the verification is removed.
        }
    }

    @Test
    @DisplayName("firestoreEmulator should initialize successfully with emulator host")
    void firestoreEmulator_success() throws Exception {
        // Arrange
        Firestore mockFirestore = mock(Firestore.class);

        try (MockedStatic<FirestoreOptions> mockedFirestoreOptions = mockStatic(FirestoreOptions.class);
             MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);

            mockedFirestoreOptions.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.setProjectId(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setEmulatorHost(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setCredentials(any(GoogleCredentials.class))).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestore);

            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            // Act
            Firestore resultFirestore = firebaseConfig.firestoreEmulator();

            // Assert
            assertNotNull(resultFirestore);
            assertEquals(mockFirestore, resultFirestore);
            // The close() verification is removed as the method under test no longer calls it.
        }
    }

    // --- Tests for firestoreProduction() ---

    @Test
    @DisplayName("firestoreProduction should initialize successfully using Base64 environment variable")
    void firestoreProduction_successWithB64Credentials() throws IOException {
        // Arrange
        // Set the B64 string to a valid mock value for this test
        ReflectionTestUtils.setField(firebaseConfig, "serviceAccountB64", "eyJtZXNzYWdlIjoiaGVsbG8ifQ==");

        try (MockedStatic<GoogleCredentials> mockedCredentials = mockStatic(GoogleCredentials.class);
             MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class);
             MockedStatic<FirestoreOptions> mockedFirestoreOptions = mockStatic(FirestoreOptions.class);
             MockedStatic<Base64> mockedBase64 = mockStatic(Base64.class)) {

            Base64.Decoder mockDecoder = mock(Base64.Decoder.class);
            mockedBase64.when(Base64::getDecoder).thenReturn(mockDecoder);
            when(mockDecoder.decode(anyString())).thenReturn("{}".getBytes());

            GoogleCredentials mockCreds = mock(GoogleCredentials.class);
            mockedCredentials.when(() -> GoogleCredentials.fromStream(any(InputStream.class))).thenReturn(mockCreds);
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);
            Firestore mockFirestore = mock(Firestore.class);

            mockedFirestoreOptions.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.setProjectId(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setCredentials(any(GoogleCredentials.class))).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestore);
            when(mockOptions.getProjectId()).thenReturn("test-project");

            // CORRIGIDO: Mockar o comportamento de firestore.getOptions() para retornar o mockOptions
            when(mockFirestore.getOptions()).thenReturn(mockOptions);

            // Act
            Firestore resultFirestore = firebaseConfig.firestoreProduction();

            // Assert
            assertNotNull(resultFirestore);
            assertEquals(mockFirestore, resultFirestore);
            mockedFirebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), times(1));
            // Verify that the file-based path was not taken
            verify(resourceLoader, never()).getResource(anyString());
        }
    }

    @Test
    @DisplayName("firestoreProduction should initialize successfully using service account file as fallback")
    void firestoreProduction_successWithFileFallback() throws IOException {
        // Arrange
        // Set the B64 string to be blank to test the fallback logic
        ReflectionTestUtils.setField(firebaseConfig, "serviceAccountB64", "");

        Resource mockResource = mock(Resource.class);
        when(resourceLoader.getResource(serviceAccountPath)).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream("invalid but ignored".getBytes()));

        try (MockedStatic<GoogleCredentials> mockedCredentials = mockStatic(GoogleCredentials.class);
             MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class);
             MockedStatic<FirestoreOptions> mockedFirestoreOptions = mockStatic(FirestoreOptions.class)) {

            GoogleCredentials mockCreds = mock(GoogleCredentials.class);
            mockedCredentials.when(() -> GoogleCredentials.fromStream(any(InputStream.class))).thenReturn(mockCreds);
            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

            FirestoreOptions.Builder mockBuilder = mock(FirestoreOptions.Builder.class);
            FirestoreOptions mockOptions = mock(FirestoreOptions.class);
            Firestore mockFirestore = mock(Firestore.class);

            mockedFirestoreOptions.when(FirestoreOptions::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.setProjectId(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.setCredentials(any(GoogleCredentials.class))).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockOptions);
            when(mockOptions.getService()).thenReturn(mockFirestore);
            when(mockOptions.getProjectId()).thenReturn("test-project");

            // CORRIGIDO: Mockar o comportamento de firestore.getOptions() para retornar o mockOptions
            when(mockFirestore.getOptions()).thenReturn(mockOptions);

            // Act
            Firestore resultFirestore = firebaseConfig.firestoreProduction();

            // Assert
            assertNotNull(resultFirestore);
            assertEquals(mockFirestore, resultFirestore);
            mockedFirebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), times(1));
        }
    }

    @Test
    @DisplayName("firestoreProduction should throw IOException if service account file not found (fallback scenario)")
    void firestoreProduction_serviceAccountNotFound_throwsIOException() throws IOException {
        // Arrange
        // Set the B64 string to be blank to test the fallback logic
        ReflectionTestUtils.setField(firebaseConfig, "serviceAccountB64", "");

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
}