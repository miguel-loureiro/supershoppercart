package com.supershopcart.controllers;

import com.supershopcart.models.ShopCart;
import com.supershopcart.models.Shopper;
import com.supershopcart.repositories.ShopCartRepository;
import com.supershopcart.repositories.ShopperRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * REST controller for administrative tasks.
 * Provides endpoints for managing shoppers and shop carts that are not part of the normal user flow.
 * These endpoints should be secured with appropriate administrative roles.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ShopperRepository shopperRepository;
    private final ShopCartRepository shopCartRepository;

    public AdminController(ShopperRepository shopperRepository, ShopCartRepository shopCartRepository) {
        this.shopperRepository = shopperRepository;
        this.shopCartRepository = shopCartRepository;
    }

    // --- Shopper Administrative Endpoints ---
    // These are for auditing, debugging, and account management.

    /**
     * Retrieves all shoppers in the system.
     * Useful for auditing and administrative oversight.
     */
    @GetMapping("/shoppers")
    public ResponseEntity<List<Shopper>> getAllShoppers() {
        try {
            List<Shopper> shoppers = shopperRepository.findAll();
            return ResponseEntity.ok(shoppers);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves a single shopper by their ID.
     * Useful for troubleshooting or viewing specific user data.
     */
    @GetMapping("/shoppers/{id}")
    public ResponseEntity<?> getShopperById(@PathVariable String id) {
        try {
            Optional<Shopper> shopper = shopperRepository.findById(id);
            if (shopper.isPresent()) {
                return ResponseEntity.ok(shopper.get());
            } else {
                // Consistent error response for NOT_FOUND
                Map<String, String> errorResponse = Collections.singletonMap("error", "Shopper not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            // Consistent error response for INTERNAL_SERVER_ERROR
            Map<String, String> errorResponse = Collections.singletonMap("error", "Failed to retrieve shopper: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Deletes a specific shopper account.
     * This is a critical administrative function for account management and GDPR compliance.
     */
    @DeleteMapping("/shoppers/{id}")
    public ResponseEntity<Void> deleteShopper(@PathVariable String id) {
        try {
            shopperRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- ShopCart Administrative Endpoints ---
    // These are primarily for auditing and manual cleanup.

    /**
     * Retrieves all shop carts in the system.
     * This is for auditing and identifying stale or problematic carts.
     */
    @GetMapping("/shopcarts")
    public ResponseEntity<List<ShopCart>> getAllShopCarts() {
        try {
            List<ShopCart> shopCarts = shopCartRepository.findAll();
            return ResponseEntity.ok(shopCarts);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves a single shop cart by its ID.
     * Useful for troubleshooting issues with a specific user's cart.
     */
    @GetMapping("/shopcarts/{id}")
    public ResponseEntity<?> getShopCartById(@PathVariable String id) {
        try {
            Optional<ShopCart> shopCart = shopCartRepository.findById(id);
            if (shopCart.isPresent()) {
                return ResponseEntity.ok(shopCart.get());
            } else {
                // Return a consistent error body, e.g., a Map
                Map<String, String> errorResponse = Collections.singletonMap("error", "Shop cart not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            Map<String, String> errorResponse = Collections.singletonMap("error", "Failed to retrieve shop cart: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
