package com.supershopcart.models;

import com.supershopcart.enums.SharePermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharePermissionEntryTest {

    @Test
    @DisplayName("Default constructor should create an object with null values")
    void testDefaultConstructor() {
        SharePermissionEntry entry = new SharePermissionEntry();
        assertNull(entry.getShopperId());
        assertNull(entry.getPermission());
    }

    @Test
    @DisplayName("Parameterized constructor should correctly set shopperId and permission")
    void testParameterizedConstructor() {
        String shopperId = "shopper123";
        SharePermission permission = SharePermission.EDIT;
        SharePermissionEntry entry = new SharePermissionEntry(shopperId, permission);

        assertEquals(shopperId, entry.getShopperId());
        assertEquals(permission, entry.getPermission());
    }

    @Test
    @DisplayName("Setter methods should correctly update shopperId and permission")
    void testSetters() {
        SharePermissionEntry entry = new SharePermissionEntry();
        String newShopperId = "shopper456";
        SharePermission newPermission = SharePermission.ADMIN;

        entry.setShopperId(newShopperId);
        entry.setPermission(newPermission);

        assertEquals(newShopperId, entry.getShopperId());
        assertEquals(newPermission, entry.getPermission());
    }

    @Test
    @DisplayName("equals: Should return true for two identical objects")
    void testEquals_IdenticalObjects() {
        SharePermissionEntry entry1 = new SharePermissionEntry("shopper789", SharePermission.VIEW);
        SharePermissionEntry entry2 = new SharePermissionEntry("shopper789", SharePermission.VIEW);

        assertEquals(entry1, entry2);
        assertEquals(entry1, entry1);
        assertEquals(entry1, entry2);
        assertEquals(entry1, entry1);
    }

    @Test
    @DisplayName("equals: Should return false for two different objects")
    void testEquals_DifferentObjects() {
        SharePermissionEntry entry1 = new SharePermissionEntry("shopper101", SharePermission.EDIT);
        SharePermissionEntry entry2 = new SharePermissionEntry("shopper102", SharePermission.EDIT);
        SharePermissionEntry entry3 = new SharePermissionEntry("shopper101", SharePermission.ADMIN);

        assertNotEquals(entry1, entry2);
        assertNotEquals(entry1, entry3);
        assertNotEquals(entry1, entry2);
        assertNotEquals(entry1, entry3);
    }

    @Test
    @DisplayName("equals: Should handle null and different class types")
    void testEquals_NullAndDifferentClass() {
        SharePermissionEntry entry = new SharePermissionEntry("shopper202", SharePermission.EDIT);
        assertNotEquals(null, entry);
        assertNotEquals(new Object(), entry);
    }

    @Test
    @DisplayName("hashCode: Should produce the same hash for identical objects")
    void testHashCode_IdenticalObjects() {
        SharePermissionEntry entry1 = new SharePermissionEntry("shopper789", SharePermission.VIEW);
        SharePermissionEntry entry2 = new SharePermissionEntry("shopper789", SharePermission.VIEW);

        assertEquals(entry1.hashCode(), entry2.hashCode());
    }

    @Test
    @DisplayName("hashCode: Should produce different hashes for different objects")
    void testHashCode_DifferentObjects() {
        SharePermissionEntry entry1 = new SharePermissionEntry("shopper101", SharePermission.EDIT);
        SharePermissionEntry entry2 = new SharePermissionEntry("shopper102", SharePermission.EDIT);

        assertNotEquals(entry1.hashCode(), entry2.hashCode());
    }
}