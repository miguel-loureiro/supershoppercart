package com.supershoppercart.services;

import com.google.cloud.firestore.DocumentReference;
import com.supershoppercart.dtos.ShopCartDetailDTO;
import com.supershoppercart.enums.SharePermission;
import com.supershoppercart.models.GroceryItem;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopCartRepository;
import com.supershoppercart.repositories.ShopperRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Service layer for managing ShopCarts and their interactions with Shoppers.
 */
@Service
public class ShopCartService {

    private final ShopCartRepository shopCartRepository;
    private final ShopperRepository shopperRepository;

    public ShopCartService(ShopCartRepository shopCartRepository, ShopperRepository shopperRepository) {
        this.shopCartRepository = shopCartRepository;
        this.shopperRepository = shopperRepository;
    }

    /**
     * Creates a new shopping cart and optionally links it to existing shoppers.
     *
     * @param dateKey The date key for the cart.
     * @param items Initial grocery items for the cart.
     * @param shopperEmails Emails of shoppers to link to this cart.
     * @return The created ShopCart.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     * @throws IllegalArgumentException If a shopper email is not found.
     */
    public ShopCart createShopCart(String dateKey, List<GroceryItem> items, List<String> shopperEmails)
            throws ExecutionException, InterruptedException, IllegalArgumentException {
        ShopCart newCart = new ShopCart();
        newCart.setDateKey(dateKey);
        if (items != null) {
            newCart.setItems(new ArrayList<>(items));
        }

        List<String> shopperIds = new ArrayList<>();
        for (String email : shopperEmails) {
            Optional<Shopper> shopperOptional = shopperRepository.findByEmail(email);
            if (shopperOptional.isPresent()) {
                shopperIds.add(shopperOptional.get().getId());
            } else {
                throw new IllegalArgumentException("Shopper with email " + email + " not found.");
            }
        }
        newCart.setShopperIds(shopperIds);

        ShopCart savedCart = shopCartRepository.save(newCart);

        // Update shoppers to include the new cart's ID
        for (String shopperId : shopperIds) {
            shopperRepository.findById(shopperId).ifPresent(shopper -> {
                if (!shopper.getShopCartIds().contains(savedCart.getId())) {
                    shopper.getShopCartIds().add(savedCart.getId());
                    try {
                        shopperRepository.save(shopper);
                    } catch (ExecutionException | InterruptedException e) {
                        System.err.println("Error updating shopper's cart list: " + e.getMessage());
                        // Consider more robust error handling / rollback
                    }
                }
            });
        }

        return savedCart;
    }

    /**
     * Retrieves a ShopCart by its ID.
     *
     * @param cartId The ID of the shop cart.
     * @return An Optional containing the ShopCart if found.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    public Optional<ShopCart> getShopCartById(String cartId) throws ExecutionException, InterruptedException {
        return shopCartRepository.findById(cartId);
    }

    /**
     * Retrieves all shop carts.
     *
     * @return A list of all shop carts.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    public List<ShopCart> getAllShopCarts() throws ExecutionException, InterruptedException {
        return shopCartRepository.findAll();
    }

    /**
     * Adds an item to an existing shopping cart.
     *
     * @param cartId The ID of the shop cart to update.
     * @param item The grocery item to add.
     * @return The updated ShopCart.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     * @throws IllegalArgumentException If the cart is not found.
     */
    public ShopCart addItemToCart(String cartId, GroceryItem item)
            throws ExecutionException, InterruptedException, IllegalArgumentException {
        Optional<ShopCart> cartOptional = shopCartRepository.findById(cartId);
        if (cartOptional.isPresent()) {
            ShopCart cart = cartOptional.get();
            cart.getItems().add(item);
            return shopCartRepository.save(cart);
        } else {
            throw new IllegalArgumentException("ShopCart with ID " + cartId + " not found.");
        }
    }

    /**
     * Marks an item in a shop cart as purchased.
     *
     * @param cartId The ID of the shop cart.
     * @param designation The designation of the item to mark.
     * @return The updated ShopCart.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     * @throws IllegalArgumentException If the cart or item is not found.
     */
    public ShopCart markItemAsPurchased(String cartId, String designation)
            throws ExecutionException, InterruptedException, IllegalArgumentException {
        Optional<ShopCart> cartOptional = shopCartRepository.findById(cartId);
        if (cartOptional.isPresent()) {
            ShopCart cart = cartOptional.get();
            boolean itemFound = false;
            for (GroceryItem item : cart.getItems()) {
                if (item.getDesignation().equals(designation)) {
                    item.setPurchased(true);
                    itemFound = true;
                    break;
                }
            }
            if (!itemFound) {
                throw new IllegalArgumentException("Grocery item with designation " + designation + " not found in cart " + cartId);
            }
            return shopCartRepository.save(cart);
        } else {
            throw new IllegalArgumentException("ShopCart with ID " + cartId + " not found.");
        }
    }

    /**
     * Deletes a shopping cart.
     *
     * @param cartId The ID of the shop cart to delete.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    public void deleteShopCart(String cartId) throws ExecutionException, InterruptedException {
        // Also consider removing this cart ID from any associated shoppers
        Optional<ShopCart> cartOptional = shopCartRepository.findById(cartId);
        if (cartOptional.isPresent()) {
            ShopCart cart = cartOptional.get();
            for (String shopperId : cart.getShopperIds()) {
                shopperRepository.findById(shopperId).ifPresent(shopper -> {
                    shopper.getShopCartIds().remove(cartId);
                    try {
                        shopperRepository.save(shopper);
                    } catch (ExecutionException | InterruptedException e) {
                        System.err.println("Error removing cart ID from shopper: " + e.getMessage());
                    }
                });
            }
            shopCartRepository.deleteById(cartId);
        } else {
            System.out.println("ShopCart with ID " + cartId + " not found for deletion.");
        }
    }

    /**
     * Retrieves a shop cart template by its ID.
     *
     * @param templateId The ID of the template.
     * @return An Optional containing the template ShopCart if found.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    public Optional<ShopCart> getShopCartTemplateById(String templateId) throws ExecutionException, InterruptedException {
        return shopCartRepository.findTemplateById(templateId);
    }

    /**
     * Creates a new active shopping cart from an existing template.
     *
     * @param templateId The ID of the template cart to use.
     * @param creatorId The ID of the shopper creating the new cart.
     * @return The newly created ShopCart.
     * @throws IllegalStateException if the specified cart is not a template.
     * @throws IllegalArgumentException if the template cart is not found.
     * @throws ExecutionException if a Firestore operation fails.
     * @throws InterruptedException if the thread is interrupted.
     */
    public ShopCart createCartFromTemplate(String templateId, String creatorId)
            throws IllegalStateException, IllegalArgumentException, ExecutionException, InterruptedException {
        Optional<ShopCart> templateCartOptional = getShopCartTemplateById(templateId);

        if (templateCartOptional.isEmpty()) {
            throw new IllegalArgumentException("Template with ID " + templateId + " not found.");
        }

        ShopCart templateCart = templateCartOptional.get();

        if (!templateCart.isTemplate()) {
            throw new IllegalStateException("The specified cart is not a template.");
        }

        // Use the existing logic in the ShopCart class to create a new instance
        ShopCart newCart = templateCart.createFromTemplate(creatorId, List.of(creatorId));

        // Save the new cart to Firestore
        return shopCartRepository.save(newCart);
    }

    // New method for saving a template
    //-------------------------------------------------------------------------
    /**
     * Saves a ShopCart as a reusable template. The cart's state is set to TEMPLATE,
     * and its purchase status for all items is reset.
     *
     * @param shopCart The ShopCart object to save as a template.
     * @return The newly generated ID of the saved template document.
     * @throws RuntimeException if there's an error during the Firestore operation.
     */
    public ShopCart saveShopCartAsTemplate(ShopCart shopCart) throws ExecutionException, InterruptedException {
        // Set the cart's state to TEMPLATE and mark it as a template
        shopCart.convertToTemplate(shopCart.getName());

        // Templates don't need shopper associations or permissions
        shopCart.setShopperIds(new ArrayList<>());
        shopCart.setSharePermissions(new ArrayList<>());

        return shopCartRepository.saveTemplate(shopCart);
    }

    /**
     * Retrieves all carts associated with a specific shopper ID.
     *
     * @param shopperId the shopper ID
     * @return list of carts belonging to that shopper
     */
    public List<ShopCartDetailDTO> getShopCartsByShopperId(String shopperId) throws ExecutionException, InterruptedException {
        Optional<Shopper> shopperOpt = shopperRepository.findById(shopperId);
        if (shopperOpt.isEmpty()) {
            throw new IllegalArgumentException("Shopper with ID " + shopperId + " not found.");
        }

        Shopper shopper = shopperOpt.get();
        List<String> cartIds = shopper.getShopCartIds();
        List<ShopCartDetailDTO> result = new ArrayList<>();

        for (String cartId : cartIds) {
            shopCartRepository.findById(cartId).ifPresent(cart ->
                    result.add(new ShopCartDetailDTO(cartId, cart))
            );
        }
        return result;
    }

    /**
     * Shares a cart with another shopper by email.
     *
     * @param cartId          the cart to share
     * @param ownerShopperId  the shopper initiating the share
     * @param targetEmail     the email of the target shopper
     * @param permission      permission type (e.g. "VIEW", "EDIT")
     * @return true if shared successfully
     */
    public boolean shareShopCart(String cartId, String ownerShopperId, String targetEmail, SharePermission permission)
            throws ExecutionException, InterruptedException {
        Optional<ShopCart> cartOpt = shopCartRepository.findById(cartId);
        if (cartOpt.isEmpty()) {
            throw new IllegalArgumentException("Cart with ID " + cartId + " not found.");
        }

        ShopCart cart = cartOpt.get();
        if (!cart.getShopperIds().contains(ownerShopperId)) {
            throw new IllegalArgumentException("You do not have permission to share this cart.");
        }

        Optional<Shopper> targetOpt = shopperRepository.findByEmail(targetEmail);
        if (targetOpt.isEmpty()) {
            throw new IllegalArgumentException("Target shopper with email " + targetEmail + " not found.");
        }

        Shopper target = targetOpt.get();
        if (!cart.getShopperIds().contains(target.getId())) {
            cart.getShopperIds().add(target.getId());
        }

        // record share permission
        cart.addOrUpdatePermission(target.getId(), permission);

        // persist cart update
        shopCartRepository.save(cart);

        // also update shopper doc to include cart
        if (!target.getShopCartIds().contains(cartId)) {
            target.getShopCartIds().add(cartId);
            shopperRepository.save(target);
        }

        return true;
    }

    /**
     * Removes a shopper’s access from a shared cart.
     *
     * @param cartId         the cart ID
     * @param ownerShopperId the owner performing the removal
     * @param targetShopperId the shopper to remove
     * @return true if removed successfully
     */
    public boolean removeSharing(String cartId, String ownerShopperId, String targetShopperId)
            throws ExecutionException, InterruptedException {
        Optional<ShopCart> cartOpt = shopCartRepository.findById(cartId);
        if (cartOpt.isEmpty()) {
            throw new IllegalArgumentException("Cart with ID " + cartId + " not found.");
        }

        ShopCart cart = cartOpt.get();
        if (!cart.getShopperIds().contains(ownerShopperId)) {
            throw new IllegalArgumentException("You do not have permission to modify this cart.");
        }

        boolean removed = cart.getShopperIds().remove(targetShopperId);
        cart.removePermission(targetShopperId);

        shopCartRepository.save(cart);

        shopperRepository.findById(targetShopperId).ifPresent(shopper -> {
            shopper.getShopCartIds().remove(cartId);
            try {
                shopperRepository.save(shopper);
            } catch (Exception e) {
                throw new RuntimeException("Error updating shopper after removing sharing: " + e.getMessage(), e);
            }
        });

        return removed;
    }
}
