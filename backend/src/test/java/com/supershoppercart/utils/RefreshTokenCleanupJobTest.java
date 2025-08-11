package com.supershoppercart.utils;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RefreshTokenCleanupJob class using JUnit 5 and Mockito.
 * This test suite isolates the job's logic from the actual Firestore service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenCleanupJob Unit Tests")
class RefreshTokenCleanupJobTest {

    // Mock Firestore and its components
    @Mock
    private Firestore firestore;
    @Mock
    private CollectionReference collectionReference;
    @Mock
    private Query query;
    @Mock
    private ApiFuture<QuerySnapshot> future;
    @Mock
    private QuerySnapshot querySnapshot;

    // The class under test, with mocks injected automatically
    @InjectMocks
    private RefreshTokenCleanupJob job;

    // The shared setup is now removed to prevent conflicts with the constructor test.
    // The individual tests will now contain their own specific mock setup.

    @Test
    @DisplayName("Should delete expired refresh tokens when they exist")
    void deleteExpiredRefreshTokens_shouldDeleteExistingTokens() throws Exception {
        // Arrange
        // Define the mock behavior for the Firestore call chain specifically for this test
        when(firestore.collection("refresh_tokens")).thenReturn(collectionReference);
        when(collectionReference.whereLessThan(anyString(), anyLong())).thenReturn(query);
        when(query.get()).thenReturn(future);

        // Create a list of mock documents to simulate expired tokens
        List<QueryDocumentSnapshot> expiredTokens = new ArrayList<>();
        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);

        DocumentReference docRef1 = mock(DocumentReference.class);
        DocumentReference docRef2 = mock(DocumentReference.class);

        when(doc1.getId()).thenReturn("token1");
        when(doc1.getReference()).thenReturn(docRef1);
        when(doc2.getId()).thenReturn("token2");
        when(doc2.getReference()).thenReturn(docRef2);

        expiredTokens.add(doc1);
        expiredTokens.add(doc2);

        when(future.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(expiredTokens);

        // Act
        job.deleteExpiredRefreshTokens();

        // Assert
        // Verify the entire call chain was executed as expected
        verify(firestore).collection("refresh_tokens");
        verify(collectionReference).whereLessThan(anyString(), anyLong());
        verify(query).get();

        // Verify that the delete() method was called on each document's reference
        verify(docRef1, times(1)).delete();
        verify(docRef2, times(1)).delete();

        // Ensure delete was called exactly the number of times there were expired tokens
        for (QueryDocumentSnapshot doc : expiredTokens) {
            verify(doc.getReference(), times(1)).delete();
        }
    }

    @Test
    @DisplayName("Should do nothing when no expired refresh tokens are found")
    void deleteExpiredRefreshTokens_shouldDoNothingWhenNoTokensExist() throws Exception {
        // Arrange
        // Define the mock behavior for the Firestore call chain specifically for this test
        when(firestore.collection("refresh_tokens")).thenReturn(collectionReference);
        when(collectionReference.whereLessThan(anyString(), anyLong())).thenReturn(query);
        when(query.get()).thenReturn(future);

        // Mock the call to get() to return an empty list of documents
        when(future.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(Collections.emptyList());

        // We also need a mock for a document reference to verify it's never called
        DocumentReference docRef = mock(DocumentReference.class);

        // Act
        job.deleteExpiredRefreshTokens();

        // Assert
        // Verify the Firestore query chain was executed
        verify(firestore).collection("refresh_tokens");
        verify(collectionReference).whereLessThan(anyString(), anyLong());
        verify(query).get();

        // Verify that the delete() method was never called
        verify(docRef, never()).delete();
    }

    @Test
    @DisplayName("Constructor should correctly initialize with injected Firestore client")
    void constructor_shouldInjectFirestore() {
        // Arrange, Act & Assert
        // We can just assert that the job instance and its Firestore field are not null,
        // as Mockito has already handled the injection for us.
        assertNotNull(job);
        assertNotNull(job.firestore);
    }
}

