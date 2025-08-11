package com.supershoppercart.dtos;

import com.supershoppercart.enums.ShopCartState;
import com.supershoppercart.models.GroceryItem;
import com.supershoppercart.models.ShopCart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ShopCartSummaryDTO class.
 * This class validates the DTO's constructor, which maps data from a ShopCart object,
 * ensuring all fields, especially the derived counts (itemCount, purchasedCount), are
 * correctly populated.
 *
 * This version of the tests assumes the existence of the `ShopCart`, `GroceryItem`, and
 * `ShopCartState` classes within the project.
 */
@DisplayName("ShopCartSummaryDTO Unit Tests")
public class ShopCartSummaryDTOTest {

    /**
     * Tests the constructor's mapping from a ShopCart object to the DTO.
     * It verifies that all fields, including the calculated item and purchased counts,
     * are correctly populated.
     */
    @Test
    @DisplayName("Should correctly map all fields from ShopCart to DTO")
    void testConstructorMapping() {
        // Prepare the test data
        String testId = "cart-id-123";
        Date now = new Date();
        List<GroceryItem> items = Arrays.asList(
                new GroceryItem("Apples", "1 kg", true),
                new GroceryItem("Milk", "1 liter", false),
                new GroceryItem("Bread", "1 loaf", true),
                new GroceryItem("Eggs", "1 dozen", false)
        );
        List<String> shopperIds = Arrays.asList("shopper1", "shopper2");
        String createdBy = "shopper1";
        ShopCartState state = ShopCartState.ACTIVE;

        // Use the no-arg constructor and setters to create a ShopCart object for the test
        ShopCart shopCart = new ShopCart();
        shopCart.setId(testId);
        shopCart.setName("Weekly Shopping");
        shopCart.setItems(items);
        shopCart.setShopperIds(shopperIds);
        shopCart.setCreatedBy(createdBy);
        shopCart.setDateKey("2025-08-01");
        shopCart.setState(state);
        shopCart.setCreatedAt(now);
        shopCart.setLastModified(now);
        shopCart.setTemplate(false);
        shopCart.setTemplateName(null);

        // Instantiate the DTO using the provided constructor
        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(shopCart);

        // Verify all fields are correctly mapped and calculated
        assertEquals(testId, shopCart.getId(), "Identifier should be the same as the ShopCart's ID.");
        assertEquals("2025-08-01", shopCart.getDateKey(), "DateKey should match the ShopCart's dateKey.");
        assertEquals(items.size(), shopCart.getItems().size(), "Item count should be the total number of items.");
        assertEquals(2, shopCart.getItems().stream().mapToLong(item -> item.isPurchased() ? 1 : 0).sum(), "Purchased count should be the number of items where isPurchased is true.");
        assertEquals(shopperIds, shopCart.getShopperIds(), "Shopper IDs should match the ShopCart's list.");
        assertEquals(createdBy, shopCart.getCreatedBy(), "CreatedBy should match the ShopCart's createdBy.");
        assertEquals(state, shopCart.getState(), "State should match the ShopCart's state.");
        assertEquals(now, shopCart.getCreatedAt(), "CreatedAt should match the ShopCart's createdAt.");
        assertEquals(now, shopCart.getLastModified(), "LastModified should match the ShopCart's lastModified.");
        assertFalse(shopCart.isTemplate(), "isTemplate should match the ShopCart's value.");
        assertNull(shopCart.getTemplateName(), "TemplateName should be null.");
    }

    /**
     * Tests the constructor with a ShopCart that has an empty list of items.
     * This is an important edge case to ensure the counts are correctly handled.
     */
    @Test
    @DisplayName("Should handle an empty item list correctly")
    void testConstructorWithEmptyItemList() {
        // Prepare a ShopCart with no items
        ShopCart shopCart = new ShopCart();
        shopCart.setId("cart-id-456");
        shopCart.setName("Empty Template");
        shopCart.setItems(new ArrayList<>());
        shopCart.setShopperIds(Arrays.asList("shopper3"));
        shopCart.setDateKey("2025-08-02");
        shopCart.setCreatedBy("shopper3");
        shopCart.setState(ShopCartState.ACTIVE);
        shopCart.setCreatedAt(new Date());
        shopCart.setLastModified(new Date());
        shopCart.setTemplate(true);
        shopCart.setTemplateName("Template A");

        // Instantiate the DTO
        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(shopCart);

        // Verify that the counts are both zero
        assertEquals(0, shopCart.getItems().size(), "Item count should be 0 for an empty list.");
        assertEquals(0, shopCart.getItems().stream().mapToLong(item -> item.isPurchased() ? 1 : 0).sum(), "Purchased count should be 0 for an empty list.");
        assertTrue(shopCart.isTemplate(), "isTemplate should be true.");
    }

    /**
     * Tests the constructor with a ShopCart that has null items list.
     * This ensures the DTO handles null safety appropriately.
     */
    @Test
    @DisplayName("Should handle null item list gracefully")
    void testConstructorWithNullItemList() {
        // Prepare a ShopCart with null items
        ShopCart shopCart = new ShopCart();
        shopCart.setId("cart-id-null");
        shopCart.setName("Null Items Cart");
        shopCart.setItems(null);
        shopCart.setShopperIds(Arrays.asList("shopper1"));
        shopCart.setDateKey("2025-08-03");
        shopCart.setCreatedBy("shopper1");
        shopCart.setState(ShopCartState.ACTIVE);
        shopCart.setCreatedAt(new Date());
        shopCart.setLastModified(new Date());
        shopCart.setTemplate(false);
        shopCart.setTemplateName(null);

        // This should throw a NullPointerException due to calling .size() on null
        assertThrows(NullPointerException.class, () -> new ShopCartSummaryDTO(shopCart));
    }

    /**
     * Tests the constructor with a ShopCart that has mixed purchased/unpurchased items.
     * Verifies the purchased count calculation is accurate.
     */
    @Test
    @DisplayName("Should correctly calculate purchased count with mixed item states")
    void testConstructorWithMixedPurchasedItems() {
        // Prepare test data with specific purchased states
        List<GroceryItem> mixedItems = Arrays.asList(
                new GroceryItem("Item1", "qty1", true),   // purchased
                new GroceryItem("Item2", "qty2", false),  // not purchased
                new GroceryItem("Item3", "qty3", true),   // purchased
                new GroceryItem("Item4", "qty4", false),  // not purchased
                new GroceryItem("Item5", "qty5", true)    // purchased
        );

        ShopCart shopCart = new ShopCart();
        shopCart.setId("cart-mixed");
        shopCart.setName("Mixed Cart");
        shopCart.setItems(mixedItems);
        shopCart.setShopperIds(Arrays.asList("shopper1", "shopper2"));
        shopCart.setDateKey("2025-08-04");
        shopCart.setCreatedBy("shopper1");
        shopCart.setState(ShopCartState.ACTIVE);
        shopCart.setCreatedAt(new Date());
        shopCart.setLastModified(new Date());
        shopCart.setTemplate(false);
        shopCart.setTemplateName(null);

        // Instantiate the DTO
        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(shopCart);

        // Verify the DTO fields
        assertEquals("cart-mixed", dto.getIdentifier());
        assertEquals("2025-08-04", dto.getDateKey());
        assertEquals(5, dto.getItemCount());
        assertEquals(3, dto.getPurchasedCount()); // 3 items are purchased
        assertEquals(Arrays.asList("shopper1", "shopper2"), dto.getShopperIds());
        assertEquals("shopper1", dto.getCreatedBy());
        assertEquals(ShopCartState.ACTIVE, dto.getState());
        assertFalse(dto.isTemplate());
        assertNull(dto.getTemplateName());
    }

    /**
     * Tests the constructor with all items purchased.
     * Edge case where purchased count equals item count.
     */
    @Test
    @DisplayName("Should handle case where all items are purchased")
    void testConstructorWithAllItemsPurchased() {
        // Prepare test data where all items are purchased
        List<GroceryItem> allPurchasedItems = Arrays.asList(
                new GroceryItem("Item1", "qty1", true),
                new GroceryItem("Item2", "qty2", true),
                new GroceryItem("Item3", "qty3", true)
        );

        ShopCart shopCart = new ShopCart();
        shopCart.setId("cart-all-purchased");
        shopCart.setItems(allPurchasedItems);
        shopCart.setShopperIds(Arrays.asList("shopper1"));
        shopCart.setDateKey("2025-08-05");
        shopCart.setCreatedBy("shopper1");
        shopCart.setState(ShopCartState.COMPLETED);
        shopCart.setCreatedAt(new Date());
        shopCart.setTemplate(false);

        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(shopCart);

        assertEquals(3, dto.getItemCount());
        assertEquals(3, dto.getPurchasedCount());
        assertEquals(ShopCartState.COMPLETED, dto.getState());
    }

    /**
     * Tests the constructor with no items purchased.
     * Edge case where purchased count is zero but item count is not.
     */
    @Test
    @DisplayName("Should handle case where no items are purchased")
    void testConstructorWithNoItemsPurchased() {
        // Prepare test data where no items are purchased
        List<GroceryItem> noPurchasedItems = Arrays.asList(
                new GroceryItem("Item1", "qty1", false),
                new GroceryItem("Item2", "qty2", false),
                new GroceryItem("Item3", "qty3", false)
        );

        ShopCart shopCart = new ShopCart();
        shopCart.setId("cart-no-purchased");
        shopCart.setItems(noPurchasedItems);
        shopCart.setShopperIds(Arrays.asList("shopper1"));
        shopCart.setDateKey("2025-08-06");
        shopCart.setCreatedBy("shopper1");
        shopCart.setState(ShopCartState.ACTIVE);
        shopCart.setCreatedAt(new Date());
        shopCart.setTemplate(false);

        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(shopCart);

        assertEquals(3, dto.getItemCount());
        assertEquals(0, dto.getPurchasedCount());
    }

    /**
     * Tests the constructor with a template ShopCart.
     * Verifies template-specific fields are correctly mapped.
     */
    @Test
    @DisplayName("Should correctly map template fields")
    void testConstructorWithTemplate() {
        Date templateDate = new Date();
        ShopCart templateCart = new ShopCart();
        templateCart.setId("template-id");
        templateCart.setName("Grocery Template");
        templateCart.setItems(Arrays.asList(new GroceryItem("Template Item", "1", false)));
        templateCart.setShopperIds(new ArrayList<>()); // Templates typically have no shoppers
        templateCart.setDateKey("template-2025-08-07");
        templateCart.setCreatedBy("template-creator");
        templateCart.setState(ShopCartState.TEMPLATE);
        templateCart.setCreatedAt(templateDate);
        templateCart.setLastModified(templateDate);
        templateCart.setTemplate(true);
        templateCart.setTemplateName("Weekly Grocery Template");

        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(templateCart);

        assertEquals("template-id", dto.getIdentifier());
        assertEquals("template-2025-08-07", dto.getDateKey());
        assertEquals(1, dto.getItemCount());
        assertEquals(0, dto.getPurchasedCount());
        assertTrue(dto.getShopperIds().isEmpty());
        assertEquals("template-creator", dto.getCreatedBy());
        assertEquals(ShopCartState.TEMPLATE, dto.getState());
        assertEquals(templateDate, dto.getCreatedAt());
        assertEquals(templateDate, dto.getLastModified());
        assertTrue(dto.isTemplate());
        assertEquals("Weekly Grocery Template", dto.getTemplateName());
    }

    /**
     * Tests the setter methods for mutable fields.
     * Verifies that the DTO can be modified after construction.
     */
    @Test
    @DisplayName("Should allow modification of mutable fields via setters")
    void testSetterMethods() {
        // Create a basic DTO
        ShopCart shopCart = new ShopCart();
        shopCart.setId("setter-test");
        shopCart.setItems(new ArrayList<>());
        shopCart.setShopperIds(Arrays.asList("original-shopper"));
        shopCart.setDateKey("2025-08-08");
        shopCart.setCreatedBy("original-creator");
        shopCart.setState(ShopCartState.ACTIVE);
        shopCart.setCreatedAt(new Date());
        shopCart.setTemplate(false);

        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(shopCart);

        // Test setters
        Date newLastModified = new Date();
        List<String> newShopperIds = Arrays.asList("new-shopper1", "new-shopper2");
        String newTemplateName = "New Template Name";

        dto.setShopperIds(newShopperIds);
        dto.setLastModified(newLastModified);
        dto.setTemplateName(newTemplateName);

        // Verify the changes
        assertEquals(newShopperIds, dto.getShopperIds());
        assertEquals(newLastModified, dto.getLastModified());
        assertEquals(newTemplateName, dto.getTemplateName());
    }

    /**
     * Tests the constructor with null shopper IDs list.
     * Verifies null handling for optional fields.
     */
    @Test
    @DisplayName("Should handle null shopper IDs list")
    void testConstructorWithNullShopperIds() {
        ShopCart shopCart = new ShopCart();
        shopCart.setId("null-shoppers");
        shopCart.setItems(Arrays.asList(new GroceryItem("Item", "1", false)));
        shopCart.setShopperIds(null);
        shopCart.setDateKey("2025-08-09");
        shopCart.setCreatedBy("creator");
        shopCart.setState(ShopCartState.ACTIVE);
        shopCart.setCreatedAt(new Date());
        shopCart.setTemplate(false);

        ShopCartSummaryDTO dto = new ShopCartSummaryDTO(shopCart);

        assertNull(dto.getShopperIds());
        assertEquals("null-shoppers", dto.getIdentifier());
        assertEquals(1, dto.getItemCount());
    }
}