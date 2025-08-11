package com.supershoppercart.services;

import com.google.cloud.firestore.DocumentReference;
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
}
