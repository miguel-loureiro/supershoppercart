package com.supershoppercart.dtos;

import com.supershoppercart.enums.SharePermission;
import com.supershoppercart.enums.ShopCartState;
import com.supershoppercart.models.GroceryItem;
import com.supershoppercart.models.SharePermissionEntry;
import com.supershoppercart.models.ShopCart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ShopCartDetailDTO class.
 * This class validates the functionality of the DTO, particularly its constructor
 * which maps data from a ShopCart object, as well as its getters and setters.
 *
 * This test file assumes the existence of the ShopCart, GroceryItem,
 * SharePermissionEntry, ShopperSummaryDTO, and ShopCartState classes.
 */
@DisplayName("ShopCartDetailDTO Unit Tests")
public class ShopCartDetailDTOTest {

    private String testIdentifier;
    private ShopCart testShopCart;

    @BeforeEach
    void setUp() {
        // Prepare the test data for each test
        List<GroceryItem> items = Arrays.asList(
                new GroceryItem("Apples", "1 kg", false),
                new GroceryItem("Milk", "2 liters", true)
        );
        List<String> shopperIds = Arrays.asList("shopper1", "shopper2");
        List<SharePermissionEntry> permissions = Arrays.asList(
                new SharePermissionEntry("shopper1", SharePermission.EDIT),
                new SharePermissionEntry("shopper2", SharePermission.VIEW)
        );
        Date now = new Date();

        testIdentifier = "cart_abc123";

        // Create the ShopCart object using setters and a no-arg constructor
        testShopCart = new ShopCart();
        testShopCart.setId("firestore_id_123"); // ID will be overridden by the DTO's identifier
        testShopCart.setName("Weekend Groceries");
        testShopCart.setDateKey("2025-08-01");
        testShopCart.setItems(items);
        testShopCart.setShopperIds(shopperIds);
        testShopCart.setCreatedBy("shopper1");
        testShopCart.setSharePermissions(permissions);
        testShopCart.setState(ShopCartState.ACTIVE);
        testShopCart.setCreatedAt(now);
        testShopCart.setLastModified(now);
        testShopCart.setLastInteraction(now);
        testShopCart.setTemplate(false);
        testShopCart.setTemplateName(null);
    }

    /**
     * Tests the constructor's mapping from a ShopCart object to the DTO.
     * It verifies that all fields are correctly populated.
     */
    @Test
    @DisplayName("Should correctly map all fields from ShopCart to DTO via constructor")
    void testConstructorMapping() {
        // Instantiate the DTO using the provided constructor
        ShopCartDetailDTO testDto = new ShopCartDetailDTO(testIdentifier, testShopCart);

        // Verify identifier is set correctly from the constructor argument
        assertEquals(testIdentifier, testDto.getIdentifier(), "Identifier should match the one passed to the constructor.");

        // Verify fields mapped from ShopCart
        assertEquals(testShopCart.getName(), testDto.getName(), "Name should match the ShopCart name.");
        assertEquals(testShopCart.getDateKey(), testDto.getDateKey(), "DateKey should match the ShopCart dateKey.");
        assertEquals(testShopCart.getItems(), testDto.getItems(), "Items list should be equal.");
        assertEquals(testShopCart.getShopperIds(), testDto.getShopperIds(), "Shopper IDs should be equal.");
        assertEquals(testShopCart.getCreatedBy(), testDto.getCreatedBy(), "CreatedBy should match the ShopCart value.");
        assertEquals(testShopCart.getSharePermissions(), testDto.getSharePermissions(), "Share permissions should be equal.");
        assertEquals(testShopCart.getState(), testDto.getState(), "State should match the ShopCart state.");
        assertEquals(testShopCart.getCreatedAt(), testDto.getCreatedAt(), "CreatedAt date should be equal.");
        assertEquals(testShopCart.getLastModified(), testDto.getLastModified(), "LastModified date should be equal.");
        assertEquals(testShopCart.getLastInteraction(), testDto.getLastInteraction(), "LastInteraction date should be equal.");
        assertEquals(testShopCart.isTemplate(), testDto.isTemplate(), "isTemplate should match the ShopCart value.");
        assertEquals(testShopCart.getTemplateName(), testDto.getTemplateName(), "TemplateName should match the ShopCart value.");

        // Verify that the 'shoppers' list is initialized but empty, as per the DTO's constructor logic
        assertNotNull(testDto.getShoppers(), "Shoppers list should not be null.");
        assertTrue(testDto.getShoppers().isEmpty(), "Shoppers list should be empty after construction.");
    }

    /**
     * Tests that all setters function as expected, correctly updating the DTO's state.
     */
    @Test
    @DisplayName("Should correctly update DTO fields via setters")
    void testSetters() {
        // Create an initial DTO to test its setters
        ShopCartDetailDTO dto = new ShopCartDetailDTO(testIdentifier, testShopCart);

        // Define new values for all fields
        String newIdentifier = "new_id_456";
        String newName = "New Shopping List";
        String newDateKey = "2025-08-02";
        List<GroceryItem> newItems = Arrays.asList(new GroceryItem("Bread", "1 loaf", false));
        List<String> newShopperIds = Arrays.asList("shopper3");
        List<ShopperSummaryDTO> newShoppers = Arrays.asList(new ShopperSummaryDTO()); // Assuming a constructor that takes an ID
        String newCreatedBy = "shopper3";
        List<SharePermissionEntry> newPermissions = Arrays.asList(new SharePermissionEntry("shopper3", SharePermission.EDIT));
        ShopCartState newState = ShopCartState.ARCHIVED;
        Date newDate = new Date(123456789L);
        boolean newIsTemplate = true;
        String newTemplateName = "My Template";

        // Use setters to update the DTO
        dto.setIdentifier(newIdentifier);
        dto.setName(newName);
        dto.setDateKey(newDateKey);
        dto.setItems(newItems);
        dto.setShopperIds(newShopperIds);
        dto.setShoppers(newShoppers);
        dto.setCreatedBy(newCreatedBy);
        dto.setSharePermissions(newPermissions);
        dto.setState(newState);
        dto.setCreatedAt(newDate);
        dto.setLastModified(newDate);
        dto.setLastInteraction(newDate);
        dto.setTemplate(newIsTemplate);
        dto.setTemplateName(newTemplateName);

        // Verify that the getters return the newly set values
        assertEquals(newIdentifier, dto.getIdentifier());
        assertEquals(newName, dto.getName());
        assertEquals(newDateKey, dto.getDateKey());
        assertEquals(newItems, dto.getItems());
        assertEquals(newShopperIds, dto.getShopperIds());
        assertEquals(newShoppers, dto.getShoppers());
        assertEquals(newCreatedBy, dto.getCreatedBy());
        assertEquals(newPermissions, dto.getSharePermissions());
        assertEquals(newState, dto.getState());
        assertEquals(newDate, dto.getCreatedAt());
        assertEquals(newDate, dto.getLastModified());
        assertEquals(newDate, dto.getLastInteraction());
        assertEquals(newIsTemplate, dto.isTemplate());
        assertEquals(newTemplateName, dto.getTemplateName());
    }
}