package com.supershoppercart.services;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite that runs both Firestore Emulator and Real Firebase integration tests.
 * <p>
 * Prerequisites:
 * - Firestore Emulator must be running on localhost:8081 for emulator tests
 * - Firebase service account credentials must be available for real Firebase tests
 * <p>
 * Usage:
 * mvn test -Dtest=DualFirestoreIntegrationTest
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        FirestoreServiceIntegrationTest.class,        // Emulator
        FirestoreServiceRealFirebaseIntegrationTest.class  // Real Firebase
})
public class DualFirestoreIntegrationTest {
}
