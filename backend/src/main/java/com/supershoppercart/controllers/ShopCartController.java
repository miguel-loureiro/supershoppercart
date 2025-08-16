package com.supershoppercart.controllers;

import com.supershoppercart.dtos.CreateShopCartRequestDTO;
import com.supershoppercart.dtos.ShareCartRequestDTO;
import com.supershoppercart.dtos.ShopCartDetailDTO;
import com.supershoppercart.enums.ShopCartState;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.services.FirestoreService;
import com.supershoppercart.services.ShopCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/carts")
@Validated
public class ShopCartController {

    private static final Logger logger = LoggerFactory.getLogger(ShopCartController.class);

    private final ShopCartService shopCartService;

    public ShopCartController(ShopCartService shopCartService) {
        this.shopCartService = shopCartService;
    }

    @Operation(summary = "Get the current authenticated shopper's carts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of carts retrieved successfully")
    })
    @GetMapping("/mine")
    public ResponseEntity<?> getMyCarts(@AuthenticationPrincipal Shopper shopper) {
        if (shopper == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        try {
            logger.info("Fetching carts for shopper with ID: {}", shopper.getId());
            List<ShopCartDetailDTO> carts = shopCartService.getShopCartsByShopperId(shopper.getId());
            return ResponseEntity.ok(carts);
        } catch (Exception e) {
            logger.error("Error fetching carts for shopper {}", shopper.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/new")
    public ResponseEntity<?> createCart(
            @AuthenticationPrincipal Shopper currentShopper,
            @Valid @RequestBody CreateShopCartRequestDTO request) {

        if (currentShopper == null) {
            logger.warn("Attempt to create cart without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        try {
            ShopCart savedCart = shopCartService.createShopCart(
                    request.getDateKey(),
                    request.getItems(),
                    List.of(currentShopper.getEmail())
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ShopCartDetailDTO(savedCart.getId(), savedCart));

        } catch (Exception e) {
            logger.error("Error creating cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Share a cart with another shopper")
    @PostMapping("/{cartId}/share")
    public ResponseEntity<?> shareCart(
            @PathVariable String cartId,
            @Valid @RequestBody ShareCartRequestDTO request,
            @AuthenticationPrincipal Shopper currentShopper) {
        if (currentShopper == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        try {
            logger.info("Shopper {} is attempting to share cart {} with {} (permission: {})",
                    currentShopper.getId(), cartId, request.getTargetShopperEmail(), request.getPermission());

            boolean success = shopCartService.shareShopCart(
                    cartId,
                    currentShopper.getId(),
                    request.getTargetShopperEmail(),
                    request.getPermission()
            );

            return success
                    ? ResponseEntity.ok(Map.of("message", "Cart shared successfully"))
                    : ResponseEntity.badRequest().body(Map.of("error", "Failed to share cart"));

        } catch (Exception e) {
            logger.error("Error sharing cart {}", cartId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Remove sharing permission from a cart for a target shopper")
    @DeleteMapping("/{cartId}/share/{targetShopperId}")
    public ResponseEntity<?> removeSharing(
            @PathVariable String cartId,
            @PathVariable String targetShopperId,
            @AuthenticationPrincipal Shopper currentShopper) {
        if (currentShopper == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        try {
            logger.info("Shopper {} is attempting to remove sharing of cart {} from shopper {}",
                    currentShopper.getId(), cartId, targetShopperId);

            boolean success = shopCartService.removeSharing(cartId, currentShopper.getId(), targetShopperId);

            return success
                    ? ResponseEntity.ok(Map.of("message", "Sharing removed successfully"))
                    : ResponseEntity.badRequest().body(Map.of("error", "Failed to remove sharing"));

        } catch (Exception e) {
            logger.error("Error removing sharing for cart {}", cartId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

