package com.supershopcart.services;

import com.supershopcart.models.Shopper;
import com.supershopcart.repositories.ShopCartRepository;
import com.supershopcart.repositories.ShopperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ShopperService class using JUnit 5 and Mockito.
 * This class validates the business logic of the service by mocking its
 * repository dependencies to ensure each method behaves as expected under
 * various conditions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShopperService Unit Tests")
public class ShopperServiceTest {

    @Mock
    private ShopperRepository shopperRepository;

    @Mock
    private ShopCartRepository shopCartRepository;

    @InjectMocks
    private ShopperService shopperService;

    private Shopper testShopper;

    @BeforeEach
    void setUp() {
        // Initialize a mock shopper for consistent test data
        testShopper = new Shopper();
        testShopper.setId("shopperId123");
        testShopper.setEmail("test@example.com");
        testShopper.setName("Test Shopper");
        testShopper.setPassword("hashedPassword");
    }

    // --- registerShopper tests ---

    @Test
    @DisplayName("Should successfully register a new shopper")
    void testRegisterShopper_Success() throws ExecutionException, InterruptedException {
        // Mock the findByEmail call to return an empty optional, simulating a new shopper
        when(shopperRepository.findByEmail(testShopper.getEmail())).thenReturn(Optional.empty());
        // Mock the save call to return the shopper object after it's saved
        when(shopperRepository.save(any(Shopper.class))).thenReturn(testShopper);

        // Call the service method
        Shopper registeredShopper = shopperService.registerShopper(
                testShopper.getEmail(), testShopper.getName(), "password123");

        // Verify the results
        assertNotNull(registeredShopper);
        assertEquals(testShopper.getEmail(), registeredShopper.getEmail());
        assertEquals(testShopper.getName(), registeredShopper.getName());

        // Verify the interactions with the mock repository
        verify(shopperRepository, times(1)).findByEmail(testShopper.getEmail());
        verify(shopperRepository, times(1)).save(any(Shopper.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if shopper email already exists")
    void testRegisterShopper_EmailExists() throws ExecutionException, InterruptedException {
        // Mock the findByEmail call to return the existing shopper, simulating a duplicate email
        when(shopperRepository.findByEmail(testShopper.getEmail())).thenReturn(Optional.of(testShopper));

        // Verify that the correct exception is thrown
        assertThrows(IllegalArgumentException.class, () ->
                shopperService.registerShopper(testShopper.getEmail(), testShopper.getName(), "password123"));

        // Verify that the save method was never called
        verify(shopperRepository, never()).save(any(Shopper.class));
    }

    // --- getShopperById tests ---

    @Test
    @DisplayName("Should retrieve shopper by ID if found")
    void testGetShopperById_Found() throws ExecutionException, InterruptedException {
        when(shopperRepository.findById(testShopper.getId())).thenReturn(Optional.of(testShopper));

        Optional<Shopper> result = shopperService.getShopperById(testShopper.getId());

        assertTrue(result.isPresent());
        assertEquals(testShopper.getId(), result.get().getId());
        verify(shopperRepository, times(1)).findById(testShopper.getId());
    }

    @Test
    @DisplayName("Should return empty optional if shopper ID not found")
    void testGetShopperById_NotFound() throws ExecutionException, InterruptedException {
        when(shopperRepository.findById("nonexistentId")).thenReturn(Optional.empty());

        Optional<Shopper> result = shopperService.getShopperById("nonexistentId");

        assertTrue(result.isEmpty());
        verify(shopperRepository, times(1)).findById("nonexistentId");
    }

    // --- getShopperByEmail tests ---

    @Test
    @DisplayName("Should retrieve shopper by email if found")
    void testGetShopperByEmail_Found() throws ExecutionException, InterruptedException {
        when(shopperRepository.findByEmail(testShopper.getEmail())).thenReturn(Optional.of(testShopper));

        Optional<Shopper> result = shopperService.getShopperByEmail(testShopper.getEmail());

        assertTrue(result.isPresent());
        assertEquals(testShopper.getEmail(), result.get().getEmail());
        verify(shopperRepository, times(1)).findByEmail(testShopper.getEmail());
    }

    @Test
    @DisplayName("Should return empty optional if shopper email not found")
    void testGetShopperByEmail_NotFound() throws ExecutionException, InterruptedException {
        when(shopperRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<Shopper> result = shopperService.getShopperByEmail("nonexistent@example.com");

        assertTrue(result.isEmpty());
        verify(shopperRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    // --- getAllShoppers tests ---

    @Test
    @DisplayName("Should retrieve all shoppers when they exist")
    void testGetAllShoppers_Success() throws ExecutionException, InterruptedException {
        Shopper shopper2 = new Shopper();
        shopper2.setId("shopperId456");
        List<Shopper> allShoppers = Arrays.asList(testShopper, shopper2);
        when(shopperRepository.findAll()).thenReturn(allShoppers);

        List<Shopper> result = shopperService.getAllShoppers();

        assertEquals(2, result.size());
        assertTrue(result.contains(testShopper));
        assertTrue(result.contains(shopper2));
    }

    @Test
    @DisplayName("Should return an empty list if no shoppers exist")
    void testGetAllShoppers_Empty() throws ExecutionException, InterruptedException {
        when(shopperRepository.findAll()).thenReturn(Collections.emptyList());

        List<Shopper> result = shopperService.getAllShoppers();

        assertTrue(result.isEmpty());
    }

    // --- deleteShopper tests ---

    @Test
    @DisplayName("Should delete a shopper by ID")
    void testDeleteShopper_Success() throws ExecutionException, InterruptedException {
        shopperService.deleteShopper(testShopper.getId());

        // Verify that the deleteById method was called exactly once with the correct ID
        verify(shopperRepository, times(1)).deleteById(testShopper.getId());
    }

    // --- clearAllData tests ---

    @Test
    @DisplayName("Should clear all data from both repositories")
    void testClearAllData() throws ExecutionException, InterruptedException {
        shopperService.clearAllData();

        // Verify that the deleteAll methods were called on both repositories
        verify(shopCartRepository, times(1)).deleteAll();
        verify(shopperRepository, times(1)).deleteAll();
    }
}