package com.supershoppercart.controllers;

import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopCartRepository;
import com.supershoppercart.repositories.ShopperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ShopperRepository shopperRepository;

    @Mock
    private ShopCartRepository shopCartRepository;

    @InjectMocks
    private AdminController adminController;

    private Shopper testShopper1;
    private Shopper testShopper2;
    private ShopCart testShopCart1;
    private ShopCart testShopCart2;

    @BeforeEach
    void setUp() {
        // Initialize dummy shopper and shop cart objects for testing
        testShopper1 = new Shopper("shopper1@example.com", "Shopper One");
        testShopper1.setId("s1");
        testShopper2 = new Shopper("shopper2@example.com", "Shopper Two");
        testShopper2.setId("s2");

        // Use the no-arg constructor and setters based on the new ShopCart class definition
        testShopCart1 = new ShopCart();
        testShopCart1.setId("c1");
        testShopCart1.setName("Cart One");
        testShopCart1.setCreatedBy("s1");
        testShopCart1.setShopperIds(List.of("s1"));

        testShopCart2 = new ShopCart();
        testShopCart2.setId("c2");
        testShopCart2.setName("Cart Two");
        testShopCart2.setCreatedBy("s2");
        testShopCart2.setShopperIds(List.of("s2"));
    }

    // --- Shopper Endpoints Tests ---

    @Test
    void getAllShoppers_ReturnsOkAndListOfShoppers() throws ExecutionException, InterruptedException {
        // Arrange
        List<Shopper> shoppers = List.of(testShopper1, testShopper2);
        when(shopperRepository.findAll()).thenReturn(shoppers);

        // Act
        ResponseEntity<List<Shopper>> response = adminController.getAllShoppers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(testShopper1, response.getBody().get(0));
        assertEquals(testShopper2, response.getBody().get(1));
        verify(shopperRepository, times(1)).findAll();
    }

    @Test
    void getAllShoppers_RepositoryThrowsException_ReturnsInternalServerError() throws ExecutionException, InterruptedException {
        // Arrange
        when(shopperRepository.findAll()).thenThrow(new ExecutionException(new Throwable("Test Exception")));

        // Act
        ResponseEntity<List<Shopper>> response = adminController.getAllShoppers();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(null, response.getBody());
        verify(shopperRepository, times(1)).findAll();
    }

    @Test
    void getShopperById_ShopperFound_ReturnsOkAndShopper() throws ExecutionException, InterruptedException {
        // Arrange
        when(shopperRepository.findById("s1")).thenReturn(Optional.of(testShopper1));

        // Act
        ResponseEntity<?> response = adminController.getShopperById("s1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testShopper1, response.getBody());
        verify(shopperRepository, times(1)).findById("s1");
    }

    @Test
    void getShopperById_ShopperNotFound_ReturnsNotFound() throws ExecutionException, InterruptedException {
        // Arrange
        when(shopperRepository.findById("nonexistent_id")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = adminController.getShopperById("nonexistent_id");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Collections.singletonMap("error", "Shopper not found."), response.getBody());
        verify(shopperRepository, times(1)).findById("nonexistent_id");
    }

    @Test
    void getShopperById_RepositoryThrowsException_ReturnsInternalServerError() throws ExecutionException, InterruptedException {
        // Arrange
        String testId = "s1";
        when(shopperRepository.findById(testId)).thenThrow(new ExecutionException(new Throwable("Test Exception")));

        // Act
        ResponseEntity<?> response = adminController.getShopperById(testId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, String> expectedBody = Collections.singletonMap(
                "error", "Failed to retrieve shopper: java.lang.Throwable: Test Exception"
        );
        assertEquals(expectedBody, response.getBody());
        verify(shopperRepository, times(1)).findById(testId);
    }

    @Test
    void deleteShopper_SuccessfulDeletion_ReturnsNoContent() throws ExecutionException, InterruptedException {
        // Arrange
        String testId = "s1";
        doNothing().when(shopperRepository).deleteById(testId);

        // Act
        ResponseEntity<Void> response = adminController.deleteShopper(testId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(shopperRepository, times(1)).deleteById(testId);
    }

    @Test
    void deleteShopper_RepositoryThrowsException_ReturnsInternalServerError() throws ExecutionException, InterruptedException {
        // Arrange
        String testId = "s1";
        doThrow(new ExecutionException(new Throwable("Test Exception"))).when(shopperRepository).deleteById(testId);

        // Act
        ResponseEntity<Void> response = adminController.deleteShopper(testId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(shopperRepository, times(1)).deleteById(testId);
    }

    // --- ShopCart Endpoints Tests ---

    @Test
    void getAllShopCarts_ReturnsOkAndListOfShopCarts() throws ExecutionException, InterruptedException {
        // Arrange
        List<ShopCart> shopCarts = List.of(testShopCart1, testShopCart2);
        when(shopCartRepository.findAll()).thenReturn(shopCarts);

        // Act
        ResponseEntity<List<ShopCart>> response = adminController.getAllShopCarts();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(testShopCart1, response.getBody().get(0));
        assertEquals(testShopCart2, response.getBody().get(1));
        verify(shopCartRepository, times(1)).findAll();
    }

    @Test
    void getAllShopCarts_RepositoryThrowsException_ReturnsInternalServerError() throws ExecutionException, InterruptedException {
        // Arrange
        when(shopCartRepository.findAll()).thenThrow(new ExecutionException(new Throwable("Test Exception")));

        // Act
        ResponseEntity<List<ShopCart>> response = adminController.getAllShopCarts();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(shopCartRepository, times(1)).findAll();
    }

    @Test
    void getShopCartById_ShopCartFound_ReturnsOkAndShopCart() throws ExecutionException, InterruptedException {
        // Arrange
        when(shopCartRepository.findById("c1")).thenReturn(Optional.of(testShopCart1));

        // Act
        ResponseEntity<?> response = adminController.getShopCartById("c1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testShopCart1, response.getBody());
        verify(shopCartRepository, times(1)).findById("c1");
    }

    @Test
    void getShopCartById_ShopCartNotFound_ReturnsNotFound() throws ExecutionException, InterruptedException {
        // Arrange
        when(shopCartRepository.findById("nonexistent_id")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = adminController.getShopCartById("nonexistent_id");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Collections.singletonMap("error", "Shop cart not found."), response.getBody());
        verify(shopCartRepository, times(1)).findById("nonexistent_id");
    }

    @Test
    void getShopCartById_RepositoryThrowsException_ReturnsInternalServerError() throws ExecutionException, InterruptedException {
        // Arrange
        String testId = "c1";
        when(shopCartRepository.findById(testId))
                .thenThrow(new ExecutionException(new Throwable("Test Exception")));

        // Act
        ResponseEntity<?> response = adminController.getShopCartById(testId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, String> expectedBody = Collections.singletonMap(
                "error", "Failed to retrieve shop cart: java.lang.Throwable: Test Exception"
        );
        assertEquals(expectedBody, response.getBody());
        verify(shopCartRepository, times(1)).findById(testId);
    }
}