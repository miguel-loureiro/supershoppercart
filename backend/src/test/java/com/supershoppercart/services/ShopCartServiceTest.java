package com.supershoppercart.services;

import com.supershoppercart.models.GroceryItem;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopCartRepository;
import com.supershoppercart.repositories.ShopperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ShopCartService class using JUnit 5 and Mockito.
 * These tests cover all public methods of the service, mocking the
 * repository dependencies to isolate the service's logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShopCartService Unit Tests")
public class ShopCartServiceTest {

    @Mock
    private ShopCartRepository shopCartRepository;

    @Mock
    private ShopperRepository shopperRepository;

    @InjectMocks
    private ShopCartService shopCartService;

    // Test data
    private Shopper shopper1;
    private Shopper shopper2;
    private ShopCart shopCart;
    private ShopCart templateShopCart;
    private GroceryItem item1;
    private GroceryItem item2;

    @BeforeEach
    void setUp() {
        // Initialize test data for a consistent state before each test
        shopper1 = new Shopper();
        shopper1.setId("shopperId1");
        shopper1.setEmail("shopper1@example.com");
        shopper1.setShopCartIds(new ArrayList<>());

        shopper2 = new Shopper();
        shopper2.setId("shopperId2");
        shopper2.setEmail("shopper2@example.com");
        shopper2.setShopCartIds(new ArrayList<>());

        item1 = new GroceryItem("Apples", "2kg");
        item2 = new GroceryItem("Bread", "1 loaf");

        shopCart = new ShopCart();
        shopCart.setId("cartId1");
        shopCart.setDateKey("2025-08-01");
        shopCart.setItems(new ArrayList<>(Arrays.asList(item1, item2)));
        shopCart.setShopperIds(new ArrayList<>(Collections.singletonList(shopper1.getId())));

        templateShopCart = new ShopCart();
        templateShopCart.setId("template1");
        templateShopCart.setTemplate(true);
        templateShopCart.setName("Weekly groceries");
        templateShopCart.setItems(List.of(new GroceryItem("Milk", "1 liter")));
    }

    // --- createShopCart tests ---

    @Test
    @DisplayName("Should create a new cart and update associated shoppers")
    void testCreateShopCart_Success() throws ExecutionException, InterruptedException {
        List<String> shopperEmails = Arrays.asList(shopper1.getEmail(), shopper2.getEmail());
        List<GroceryItem> items = Arrays.asList(item1, item2);

        // Create a new ShopCart object that represents the expected state after saving
        ShopCart savedShopCart = new ShopCart();
        savedShopCart.setId("newCartId");
        savedShopCart.setDateKey("2025-08-01");
        savedShopCart.setItems(new ArrayList<>(items));
        savedShopCart.setShopperIds(Arrays.asList(shopper1.getId(), shopper2.getId()));

        // Mock repository calls
        when(shopperRepository.findByEmail(shopper1.getEmail())).thenReturn(Optional.of(shopper1));
        when(shopperRepository.findByEmail(shopper2.getEmail())).thenReturn(Optional.of(shopper2));
        // The key fix: mock the save method to return the new, correct ShopCart object
        when(shopCartRepository.save(any(ShopCart.class))).thenReturn(savedShopCart);
        when(shopperRepository.findById(shopper1.getId())).thenReturn(Optional.of(shopper1));
        when(shopperRepository.findById(shopper2.getId())).thenReturn(Optional.of(shopper2));

        // Call the service method
        ShopCart createdCart = shopCartService.createShopCart("2025-08-01", items, shopperEmails);

        // Verify that the correct methods were called on the mocks
        verify(shopCartRepository, times(1)).save(any(ShopCart.class));
        verify(shopperRepository, times(1)).findByEmail(shopper1.getEmail());
        verify(shopperRepository, times(1)).findByEmail(shopper2.getEmail());
        verify(shopperRepository, times(1)).findById(shopper1.getId());
        verify(shopperRepository, times(1)).findById(shopper2.getId());
        verify(shopperRepository, times(2)).save(any(Shopper.class));

        // Verify the created cart and shopper updates
        assertNotNull(createdCart.getId());
        assertEquals(2, createdCart.getShopperIds().size());
        assertTrue(createdCart.getShopperIds().contains("shopperId1"));
        assertTrue(createdCart.getShopperIds().contains("shopperId2"));
        assertTrue(shopper1.getShopCartIds().contains(createdCart.getId()));
        assertTrue(shopper2.getShopCartIds().contains(createdCart.getId()));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if shopper email not found")
    void testCreateShopCart_ShopperNotFound() throws ExecutionException, InterruptedException {
        List<String> shopperEmails = Arrays.asList(shopper1.getEmail(), "nonexistent@example.com");
        List<GroceryItem> items = Collections.emptyList();

        // Mock repository calls, simulating a non-existent email
        when(shopperRepository.findByEmail(shopper1.getEmail())).thenReturn(Optional.of(shopper1));
        when(shopperRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Verify that the correct exception is thrown
        assertThrows(IllegalArgumentException.class, () ->
                shopCartService.createShopCart("2025-08-01", items, shopperEmails)
        );

        // Verify that the save method was never called
        verify(shopCartRepository, never()).save(any(ShopCart.class));
    }

    // --- getShopCartById tests ---

    @Test
    @DisplayName("Should retrieve a cart by ID if it exists")
    void testGetShopCartById_Found() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findById(shopCart.getId())).thenReturn(Optional.of(shopCart));

        Optional<ShopCart> result = shopCartService.getShopCartById(shopCart.getId());

        assertTrue(result.isPresent());
        assertEquals(shopCart.getId(), result.get().getId());
        verify(shopCartRepository, times(1)).findById(shopCart.getId());
    }

    @Test
    @DisplayName("Should return empty optional if cart is not found")
    void testGetShopCartById_NotFound() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findById("nonexistentId")).thenReturn(Optional.empty());

        Optional<ShopCart> result = shopCartService.getShopCartById("nonexistentId");

        assertTrue(result.isEmpty());
        verify(shopCartRepository, times(1)).findById("nonexistentId");
    }

    // --- getAllShopCarts tests ---

    @Test
    @DisplayName("Should retrieve all carts when they exist")
    void testGetAllShopCarts_Success() throws ExecutionException, InterruptedException {
        ShopCart cart2 = new ShopCart();
        cart2.setId("cartId2");
        List<ShopCart> allCarts = Arrays.asList(shopCart, cart2);
        when(shopCartRepository.findAll()).thenReturn(allCarts);

        List<ShopCart> result = shopCartService.getAllShopCarts();

        assertEquals(2, result.size());
        assertTrue(result.contains(shopCart));
        assertTrue(result.contains(cart2));
    }

    @Test
    @DisplayName("Should return an empty list if no carts exist")
    void testGetAllShopCarts_Empty() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findAll()).thenReturn(Collections.emptyList());

        List<ShopCart> result = shopCartService.getAllShopCarts();

        assertTrue(result.isEmpty());
    }

    // --- addItemToCart tests ---

    @Test
    @DisplayName("Should add an item to an existing cart")
    void testAddItemToCart_Success() throws ExecutionException, InterruptedException {
        GroceryItem newItem = new GroceryItem("Oranges", "1 bag");
        when(shopCartRepository.findById(shopCart.getId())).thenReturn(Optional.of(shopCart));
        when(shopCartRepository.save(any(ShopCart.class))).thenReturn(shopCart);

        ShopCart updatedCart = shopCartService.addItemToCart(shopCart.getId(), newItem);

        assertEquals(3, updatedCart.getItems().size());
        assertTrue(updatedCart.getItems().contains(newItem));
        verify(shopCartRepository, times(1)).save(any(ShopCart.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding item to a non-existent cart")
    void testAddItemToCart_CartNotFound() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findById("nonexistentId")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                shopCartService.addItemToCart("nonexistentId", new GroceryItem("Test", "1"))
        );
        verify(shopCartRepository, never()).save(any(ShopCart.class));
    }

    // --- markItemAsPurchased tests ---

    @Test
    @DisplayName("Should mark an existing item as purchased")
    void testMarkItemAsPurchased_Success() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findById(shopCart.getId())).thenReturn(Optional.of(shopCart));
        when(shopCartRepository.save(any(ShopCart.class))).thenReturn(shopCart);

        shopCartService.markItemAsPurchased(shopCart.getId(), item1.getDesignation());

        assertTrue(shopCart.getItems().get(0).isPurchased());
        verify(shopCartRepository, times(1)).save(any(ShopCart.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when marking item in a non-existent cart")
    void testMarkItemAsPurchased_CartNotFound() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findById("nonexistentId")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                shopCartService.markItemAsPurchased("nonexistentId", "Apples")
        );
        verify(shopCartRepository, never()).save(any(ShopCart.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when item not found in cart")
    void testMarkItemAsPurchased_ItemNotFound() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findById(shopCart.getId())).thenReturn(Optional.of(shopCart));

        assertThrows(IllegalArgumentException.class, () ->
                shopCartService.markItemAsPurchased(shopCart.getId(), "Nonexistent Item")
        );
        verify(shopCartRepository, never()).save(any(ShopCart.class));
    }

    // --- deleteShopCart tests ---

    @Test
    @DisplayName("Should delete a cart and update associated shoppers")
    void testDeleteShopCart_Success() throws ExecutionException, InterruptedException {
        // Setup shopper with the cart ID
        shopper1.getShopCartIds().add(shopCart.getId());
        when(shopCartRepository.findById(shopCart.getId())).thenReturn(Optional.of(shopCart));
        when(shopperRepository.findById(shopper1.getId())).thenReturn(Optional.of(shopper1));

        shopCartService.deleteShopCart(shopCart.getId());

        // Verify that delete was called and shopper was updated
        verify(shopCartRepository, times(1)).deleteById(shopCart.getId());
        verify(shopperRepository, times(1)).findById(shopper1.getId());
        verify(shopperRepository, times(1)).save(shopper1);
        assertTrue(shopper1.getShopCartIds().isEmpty());
    }

    @Test
    @DisplayName("Should not throw an error if cart to be deleted is not found")
    void testDeleteShopCart_CartNotFound() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findById("nonexistentId")).thenReturn(Optional.empty());

        // The method should complete without throwing an exception
        shopCartService.deleteShopCart("nonexistentId");

        // Verify no repository actions were taken after the findById call
        verify(shopCartRepository, never()).deleteById(any());
        verify(shopperRepository, never()).findById(any());
    }

    // --- getShopCartTemplateById tests ---

    @Test
    @DisplayName("Should retrieve a template by ID if it exists")
    void testGetShopCartTemplateById_Found() throws ExecutionException, InterruptedException {
        // Create a template cart
        ShopCart templateCart = new ShopCart();
        templateCart.setId("templateId1");
        templateCart.setName("Weekly Grocery Template");
        templateCart.convertToTemplate("Weekly Grocery Template");
        templateCart.setItems(Arrays.asList(item1, item2));

        when(shopCartRepository.findTemplateById("templateId1")).thenReturn(Optional.of(templateCart));

        Optional<ShopCart> result = shopCartService.getShopCartTemplateById("templateId1");

        assertTrue(result.isPresent());
        assertEquals("templateId1", result.get().getId());
        assertTrue(result.get().isTemplate());
        assertEquals("Weekly Grocery Template", result.get().getName());
        verify(shopCartRepository, times(1)).findTemplateById("templateId1");
    }

    @Test
    @DisplayName("Should return empty optional if template is not found")
    void testGetShopCartTemplateById_NotFound() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findTemplateById("nonexistentTemplateId")).thenReturn(Optional.empty());

        Optional<ShopCart> result = shopCartService.getShopCartTemplateById("nonexistentTemplateId");

        assertTrue(result.isEmpty());
        verify(shopCartRepository, times(1)).findTemplateById("nonexistentTemplateId");
    }

    // --- createCartFromTemplate tests ---

    @Test
    @DisplayName("Should create a new cart from an existing template")
    void testCreateCartFromTemplate_Success() throws ExecutionException, InterruptedException {
        // Create a template cart
        ShopCart templateCart = new ShopCart();
        templateCart.setId("templateId1");
        templateCart.setName("Weekly Grocery Template");
        templateCart.convertToTemplate("Weekly Grocery Template");
        templateCart.setItems(new ArrayList<>(Arrays.asList(item1, item2)));

        // Create the expected new cart that would be created from the template
        ShopCart newCartFromTemplate = new ShopCart();
        newCartFromTemplate.setId("newCartFromTemplateId");
        newCartFromTemplate.setName("Weekly Grocery Template");
        newCartFromTemplate.setShopperIds(Arrays.asList("creatorId"));
        newCartFromTemplate.setItems(new ArrayList<>(Arrays.asList(item1, item2)));
        newCartFromTemplate.setTemplate(false);

        when(shopCartRepository.findTemplateById("templateId1")).thenReturn(Optional.of(templateCart));
        when(shopCartRepository.save(any(ShopCart.class))).thenReturn(newCartFromTemplate);

        ShopCart result = shopCartService.createCartFromTemplate("templateId1", "creatorId");

        assertNotNull(result);
        assertFalse(result.isTemplate());
        assertEquals("Weekly Grocery Template", result.getName());
        assertTrue(result.getShopperIds().contains("creatorId"));
        assertEquals(2, result.getItems().size());
        verify(shopCartRepository, times(1)).findTemplateById("templateId1");
        verify(shopCartRepository, times(1)).save(any(ShopCart.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when template is not found")
    void testCreateCartFromTemplate_TemplateNotFound() throws ExecutionException, InterruptedException {
        when(shopCartRepository.findTemplateById("nonexistentTemplateId")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                shopCartService.createCartFromTemplate("nonexistentTemplateId", "creatorId")
        );

        verify(shopCartRepository, times(1)).findTemplateById("nonexistentTemplateId");
        verify(shopCartRepository, never()).save(any(ShopCart.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when cart is not a template")
    void testCreateCartFromTemplate_NotATemplate() throws ExecutionException, InterruptedException {
        // Create a regular cart (not a template)
        ShopCart regularCart = new ShopCart();
        regularCart.setId("regularCartId");
        regularCart.setName("Regular Cart");
        regularCart.setTemplate(false);

        when(shopCartRepository.findTemplateById("regularCartId")).thenReturn(Optional.of(regularCart));

        assertThrows(IllegalStateException.class, () ->
                shopCartService.createCartFromTemplate("regularCartId", "creatorId")
        );

        verify(shopCartRepository, times(1)).findTemplateById("regularCartId");
        verify(shopCartRepository, never()).save(any(ShopCart.class));
    }

    // --- saveShopCartAsTemplate tests ---

    @Test
    @DisplayName("Should save a shop cart as a template")
    void testSaveShopCartAsTemplate_Success() throws ExecutionException, InterruptedException {
        // Create a regular cart to convert to template
        ShopCart regularCart = new ShopCart();
        regularCart.setId("regularCartId");
        regularCart.setName("My Grocery List");
        regularCart.setItems(new ArrayList<>(Arrays.asList(item1, item2)));
        regularCart.setShopperIds(Arrays.asList("shopperId1", "shopperId2"));
        regularCart.setTemplate(false);

        // Create the expected template cart that would be saved
        ShopCart savedTemplate = new ShopCart();
        savedTemplate.setId("templateId");
        savedTemplate.setName("My Grocery List");
        savedTemplate.convertToTemplate("My Grocery List");
        savedTemplate.setItems(new ArrayList<>(Arrays.asList(item1, item2)));
        savedTemplate.setShopperIds(new ArrayList<>());
        savedTemplate.setSharePermissions(new ArrayList<>());

        when(shopCartRepository.saveTemplate(any(ShopCart.class))).thenReturn(savedTemplate);

        ShopCart result = shopCartService.saveShopCartAsTemplate(regularCart);

        assertNotNull(result);
        assertTrue(result.isTemplate());
        assertEquals("My Grocery List", result.getName());
        assertTrue(result.getShopperIds().isEmpty());
        assertTrue(result.getSharePermissions().isEmpty());
        assertEquals(2, result.getItems().size());
        verify(shopCartRepository, times(1)).saveTemplate(any(ShopCart.class));

        // Verify that the original cart was modified to be a template
        assertTrue(regularCart.isTemplate());
        assertTrue(regularCart.getShopperIds().isEmpty());
        assertTrue(regularCart.getSharePermissions().isEmpty());
    }

    @Test
    @DisplayName("Should handle cart with null shopper IDs and share permissions when saving as template")
    void testSaveShopCartAsTemplate_WithNullCollections() throws ExecutionException, InterruptedException {
        // Create a cart with null collections
        ShopCart cartWithNulls = new ShopCart();
        cartWithNulls.setId("cartId");
        cartWithNulls.setName("Test Cart");
        cartWithNulls.setItems(new ArrayList<>(Arrays.asList(item1)));
        cartWithNulls.setShopperIds(null);
        cartWithNulls.setSharePermissions(null);

        ShopCart savedTemplate = new ShopCart();
        savedTemplate.setId("templateId");
        savedTemplate.setName("Test Cart");
        savedTemplate.convertToTemplate("Test Cart");

        when(shopCartRepository.saveTemplate(any(ShopCart.class))).thenReturn(savedTemplate);

        ShopCart result = shopCartService.saveShopCartAsTemplate(cartWithNulls);

        assertNotNull(result);
        assertTrue(result.isTemplate());
        assertNotNull(cartWithNulls.getShopperIds());
        assertNotNull(cartWithNulls.getSharePermissions());
        assertTrue(cartWithNulls.getShopperIds().isEmpty());
        assertTrue(cartWithNulls.getSharePermissions().isEmpty());
        verify(shopCartRepository, times(1)).saveTemplate(any(ShopCart.class));
    }


}