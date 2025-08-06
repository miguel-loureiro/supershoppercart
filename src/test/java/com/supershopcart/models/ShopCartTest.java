package com.supershopcart.models;

import com.supershopcart.enums.SharePermission;
import com.supershopcart.enums.ShopCartState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class ShopCartTest {

    private ShopCart cart;

    @BeforeEach
    void setup() {
        cart = new ShopCart();
        cart.setItems(null); // start with no items
        cart.setState(ShopCartState.ACTIVE);
    }

    // --- updateStateBasedOnItems() tests ---

    @Test
    void updateStateWithNullItems_setsActiveAndUpdatesLastInteraction() {
        cart.setItems(null);
        cart.setState(ShopCartState.SHOPPING); // set non-default to verify reset logic

        Date before = new Date();

        cart.updateStateBasedOnItems();

        assertEquals(ShopCartState.ACTIVE, cart.getState());
        assertNotNull(cart.getLastInteraction());
        assertTrue(cart.getLastInteraction().after(before) || cart.getLastInteraction().equals(before));
    }

    @Test
    void updateStateWithEmptyItems_setsActiveAndUpdatesLastInteraction() {
        cart.setItems(List.of());
        cart.setState(ShopCartState.SHOPPING);

        Date before = new Date();

        cart.updateStateBasedOnItems();

        assertEquals(ShopCartState.ACTIVE, cart.getState());
        assertNotNull(cart.getLastInteraction());
        assertTrue(cart.getLastInteraction().after(before) || cart.getLastInteraction().equals(before));
    }

    @Test
    void updateStateAllItemsPurchasedFromActive_setsShopping() {
        cart.setState(ShopCartState.ACTIVE);

        GroceryItem purchasedItem1 = new GroceryItem();
        purchasedItem1.setPurchased(true);

        GroceryItem purchasedItem2 = new GroceryItem();
        purchasedItem2.setPurchased(true);

        cart.setItems(List.of(purchasedItem1, purchasedItem2));

        cart.updateStateBasedOnItems();

        assertEquals(ShopCartState.SHOPPING, cart.getState());
    }

    @Test
    void updateStateNotAllPurchasedFromShopping_setsActive() {
        cart.setState(ShopCartState.SHOPPING);

        GroceryItem purchasedItem = new GroceryItem();
        purchasedItem.setPurchased(true);

        GroceryItem notPurchasedItem = new GroceryItem();
        notPurchasedItem.setPurchased(false);

        cart.setItems(List.of(purchasedItem, notPurchasedItem));

        cart.updateStateBasedOnItems();

        assertEquals(ShopCartState.ACTIVE, cart.getState());
    }

    @Test
    void updateStateAllPurchasedButStateNotActive_doesNotChangeState() {
        cart.setState(ShopCartState.COMPLETED);

        GroceryItem purchasedItem = new GroceryItem();
        purchasedItem.setPurchased(true);

        cart.setItems(List.of(purchasedItem));

        cart.updateStateBasedOnItems();

        // state should remain unchanged because logic changes only if state == ACTIVE or SHOPPING
        assertEquals(ShopCartState.COMPLETED, cart.getState());
    }

    // --- completeShoppingTrip() tests ---

    @Test
    void completeShoppingTrip_setsCompletedStateAndDateAndClearsCurrentShopper() {
        cart.setState(ShopCartState.ACTIVE);
        cart.setCurrentShopper("shopper1");

        Date before = new Date();

        cart.completeShoppingTrip("shopper1");

        assertEquals(ShopCartState.COMPLETED, cart.getState());
        assertNotNull(cart.getCompletedAt());
        assertTrue(cart.getCompletedAt().after(before) || cart.getCompletedAt().equals(before));
        assertNull(cart.getCurrentShopper());
        assertNotNull(cart.getLastInteraction());
    }

    // --- startNewShoppingSession() tests ---

    @Test
    void startNewShoppingSession_resetsPurchaseState_setsActiveAndNullCompletedAtCurrentShopper() {
        GroceryItem item1 = new GroceryItem();
        item1.setPurchased(true);
        GroceryItem item2 = new GroceryItem();
        item2.setPurchased(true);

        cart.setItems(List.of(item1, item2));
        cart.setState(ShopCartState.COMPLETED);
        cart.setCompletedAt(new Date());
        cart.setCurrentShopper("shopper1");

        cart.startNewShoppingSession();

        assertEquals(ShopCartState.ACTIVE, cart.getState());
        assertNull(cart.getCompletedAt());
        assertNull(cart.getCurrentShopper());
        assertFalse(cart.getItems().stream().anyMatch(GroceryItem::isPurchased)); // all reset to false
        assertNotNull(cart.getLastInteraction());
    }

    @Test
    void startNewShoppingSession_withNoItems_doesNotFail_andSetsActive() {
        cart.setItems(null);
        cart.setState(ShopCartState.COMPLETED);
        cart.setCompletedAt(new Date());
        cart.setCurrentShopper("shopper1");

        cart.startNewShoppingSession();

        assertEquals(ShopCartState.ACTIVE, cart.getState());
        assertNull(cart.getCompletedAt());
        assertNull(cart.getCurrentShopper());
        assertNotNull(cart.getLastInteraction());
    }

    // --- toString() test ---

    @Test
    void toString_containsAllExpectedFields() {
        cart.setId("cart123");
        cart.setDateKey("2023-09-01");
        cart.setShopperIds(List.of("s1", "s2"));
        cart.setSharePermissions(List.of(new SharePermissionEntry("s1", SharePermission.ADMIN)));
        cart.setCreatedBy("creator1");
        cart.setState(ShopCartState.ACTIVE);
        cart.setCurrentShopper("shopper1");
        cart.setTemplateName("template1");
        cart.setPublic(true); // isPublic
        cart.isTemplate(); // isTemplate

        String str = cart.toString();

        assertTrue(str.contains("cart123"));
        assertTrue(str.contains("2023-09-01"));
        assertTrue(str.contains("2 shoppers") || str.contains("shopperIds=2"));
        assertTrue(str.contains("entries"));
        assertTrue(str.contains("creator1"));
        assertTrue(str.contains("ACTIVE"));
        assertTrue(str.contains("shopper1"));
        assertTrue(str.contains("template1"));
        assertTrue(str.contains("true")); // for booleans like isTemplate or isPublic
    }

    @Test
    void shouldBeArchived_returnsFalse_whenCompletedAtIsNull() {
        cart.setCompletedAt(null);
        cart.setState(ShopCartState.COMPLETED);

        assertFalse(cart.shouldBeArchived());
    }

    @Test
    void shouldBeArchived_returnsFalse_whenStateNotCompleted() {
        cart.setCompletedAt(new Date(System.currentTimeMillis() - 200L * 24 * 60 * 60 * 1000)); // > 6 months ago
        cart.setState(ShopCartState.ACTIVE);

        assertFalse(cart.shouldBeArchived());
    }

    @Test
    void shouldBeArchived_returnsFalse_whenCompletedLessThanSixMonthsAgo() {
        long now = System.currentTimeMillis();
        long fiveMonthsMillis = 150L * 24 * 60 * 60 * 1000L; // 5 months approx
        cart.setCompletedAt(new Date(now - fiveMonthsMillis));
        cart.setState(ShopCartState.COMPLETED);

        assertFalse(cart.shouldBeArchived());
    }

    @Test
    void shouldBeArchived_returnsTrue_whenCompletedMoreThanSixMonthsAgo() {
        long now = System.currentTimeMillis();
        long sevenMonthsMillis = 210L * 24 * 60 * 60 * 1000L; // 7 months approx
        cart.setCompletedAt(new Date(now - sevenMonthsMillis));
        cart.setState(ShopCartState.COMPLETED);

        assertTrue(cart.shouldBeArchived());
    }

    @Test
    void archive_setsStateToArchived_andUpdatesLastInteraction() {
        cart.setState(ShopCartState.COMPLETED);
        cart.setLastInteraction(null);

        cart.archive();

        assertEquals(ShopCartState.ARCHIVED, cart.getState());
        assertNotNull(cart.getLastInteraction());
    }

    @Test
    void canDelete_returnsTrue_ifShopperHasAdminPermission() {
        String shopperId = "adminShopper";

        // Add ADMIN permission for this shopper
        cart.addOrUpdatePermission(shopperId, SharePermission.ADMIN);

        assertTrue(cart.canDelete(shopperId));
    }

    @Test
    void canDelete_returnsFalse_ifShopperHasEditPermission() {
        String shopperId = "editorShopper";

        cart.addOrUpdatePermission(shopperId, SharePermission.EDIT);

        assertFalse(cart.canDelete(shopperId));
    }

    @Test
    void canDelete_returnsFalse_ifShopperHasViewPermission() {
        String shopperId = "viewerShopper";

        cart.addOrUpdatePermission(shopperId, SharePermission.VIEW);

        assertFalse(cart.canDelete(shopperId));
    }

    @Test
    void canDelete_returnsFalse_ifShopperHasNoPermission() {
        String shopperId = "unknownShopper";

        // No permission set for this shopper
        assertFalse(cart.canDelete(shopperId));
    }
}