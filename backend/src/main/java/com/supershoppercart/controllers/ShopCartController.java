package com.supershoppercart.controllers;

import com.supershoppercart.dtos.CreateShopCartRequestDTO;
import com.supershoppercart.dtos.ShareCartRequestDTO;
import com.supershoppercart.dtos.ShopCartDetailDTO;
import com.supershoppercart.enums.ShopCartState;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.services.FirestoreService;
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
@Validated   // Enable validation for path variables etc if needed
public class ShopCartController {

    private static final Logger logger = LoggerFactory.getLogger(ShopCartController.class);

    private final FirestoreService firestoreService;

    public ShopCartController(FirestoreService firestoreService) {
        this.firestoreService = firestoreService;
    }

    @Operation(summary = "Get the current authenticated shopper's carts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of carts retrieved successfully")
    })
    @GetMapping("/mine")
    public List<ShopCartDetailDTO> getMyCarts(@AuthenticationPrincipal Shopper shopper) throws Exception {
        logger.info("Fetching carts for shopper with ID: {}", shopper.getId());
        return firestoreService.getShopCartsByShopperId(shopper.getId());
    }

    @PostMapping
    public ResponseEntity<?> createCart(
            @AuthenticationPrincipal Shopper currentShopper,
            @Valid @RequestBody CreateShopCartRequestDTO request) {

        // Add a null check for the authenticated shopper
        if (currentShopper == null) {
            // Log the error and return a 401 Unauthorized response
            logger.warn("Attempt to create cart without a valid authenticated shopper.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required to create a cart."));
        }

        try {
            logger.info("Creating a new cart for shopper {} with name '{}'", currentShopper.getId(), request.getName());

            // Build the ShopCart entity
            ShopCart newCart = new ShopCart();
            newCart.setId(UUID.randomUUID().toString());
            newCart.setName(request.getName());
            newCart.setDateKey(request.getDateKey());
            newCart.setItems(request.getItems() != null ? request.getItems() : new ArrayList<>());
            newCart.setShopperIds(List.of(currentShopper.getId()));
            newCart.setCreatedBy(currentShopper.getId());
            newCart.setPublic(request.isPublic());
            newCart.setTemplate(request.isTemplate());
            newCart.setState(ShopCartState.ACTIVE);
            newCart.setCreatedAt(new Date());
            newCart.setLastModified(new Date());
            newCart.setLastInteraction(new Date());

            // Save to Firestore
            firestoreService.saveShopCart(newCart);

            // Convert to DTO for response
            ShopCartDetailDTO dto = new ShopCartDetailDTO(newCart.getId(), newCart);

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            logger.error("Error creating cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Share a cart with another shopper
     */
    @Operation(summary = "Share a cart with another shopper",
            description = "Grants sharing permission to another shopper by email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart shared successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Failed to share cart due to invalid input or permission"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })

    @PostMapping("/{cartId}/share")
    public ResponseEntity<?> shareCart(
            @PathVariable String cartId,
            @Valid @RequestBody ShareCartRequestDTO request,
            Authentication authentication) {
        try {
            Shopper currentShopper = (Shopper) authentication.getPrincipal();
            logger.info("Shopper {} is attempting to share cart {} with {} (permission: {})",
                    currentShopper.getId(), cartId, request.getTargetShopperEmail(), request.getPermission());

            // Here you may want to verify the currentShopper has rights on this cart before sharing
            // For example:
            // if (!firestoreService.canModifyCart(currentShopper.getId(), cartId)) {
            //     logger.warn("Shopper {} has no permission to share cart {}", currentShopper.getId(), cartId);
            //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
            //          .body(Map.of("error", "No permission to share this cart"));
            // }

            boolean success = firestoreService.shareCartWithShopper(
                    cartId,
                    currentShopper.getId(),
                    request.getTargetShopperEmail(),
                    request.getPermission()
            );

            if (success) {
                logger.info("Cart {} shared successfully by shopper {}", cartId, currentShopper.getId());
                return ResponseEntity.ok(Map.of("message", "Cart shared successfully"));
            } else {
                logger.warn("Failed to share cart {} by shopper {}", cartId, currentShopper.getId());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Failed to share cart. Check permissions and target user exists."));
            }

        } catch (Exception e) {
            logger.error("Error sharing cart {}: {}", cartId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Remove sharing permission
     */
    @Operation(summary = "Remove sharing permission from a cart for a target shopper")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sharing removed successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Failed to remove sharing"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{cartId}/share/{targetShopperId}")
    public ResponseEntity<?> removeSharing(
            @PathVariable String cartId,
            @PathVariable String targetShopperId,
            Authentication authentication) {
        try {
            Shopper currentShopper = (Shopper) authentication.getPrincipal();
            logger.info("Shopper {} is attempting to remove sharing of cart {} from shopper {}",
                    currentShopper.getId(), cartId, targetShopperId);

            // Optionally check permission similarly as in shareCart method

            boolean success = firestoreService.removeCartSharing(cartId, currentShopper.getId(), targetShopperId);

            if (success) {
                logger.info("Sharing removed successfully for cart {} by shopper {}", cartId, currentShopper.getId());
                return ResponseEntity.ok(Map.of("message", "Sharing removed successfully"));
            } else {
                logger.warn("Failed to remove sharing for cart {} by shopper {}", cartId, currentShopper.getId());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Failed to remove sharing"));
            }

        } catch (Exception e) {
            logger.error("Error removing sharing for cart {}: {}", cartId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}
