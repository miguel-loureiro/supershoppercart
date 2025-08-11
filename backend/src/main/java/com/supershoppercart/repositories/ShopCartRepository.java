package com.supershoppercart.repositories;

import com.supershoppercart.models.ShopCart;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Interface for ShopCart data access operations.
 */
public interface ShopCartRepository {

    String COLLECTION_NAME = "shopCarts";

    /**
     * Saves a new ShopCart or updates an existing one.
     * If the ShopCart has an ID, it attempts to update. Otherwise, it creates a new document.
     * @param shopCart The ShopCart object to save.
     * @return The saved ShopCart with its Firestore document ID.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    ShopCart save(ShopCart shopCart) throws ExecutionException, InterruptedException;

    /**
     * Finds a ShopCart by its document ID.
     * @param id The ID of the ShopCart.
     * @return An Optional containing the ShopCart if found, or empty if not.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    Optional<ShopCart> findById(String id) throws ExecutionException, InterruptedException;

    /**
     * Retrieves all ShopCarts from the collection.
     * @return A list of all ShopCart objects.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    List<ShopCart> findAll() throws ExecutionException, InterruptedException;

    /**
     * Deletes a ShopCart by its document ID.
     * @param id The ID of the ShopCart to delete.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    void deleteById(String id) throws ExecutionException, InterruptedException;

    /**
     * Clears all documents from the ShopCart collection.
     * Primarily used for testing purposes.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    void deleteAll() throws ExecutionException, InterruptedException;

    // A new method to find a cart in the 'shopcartTemplates' collection
    Optional<ShopCart> findTemplateById(String templateId) throws ExecutionException, InterruptedException;

    // You may also want a method to find all templates
    List<ShopCart> findAllTemplates() throws ExecutionException, InterruptedException;

    ShopCart saveTemplate(ShopCart shopCart) throws ExecutionException, InterruptedException;
}
