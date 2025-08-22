package com.supershoppercart.repositories;

import com.supershoppercart.models.Shopper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Interface for Shopper data access operations.
 */
public interface ShopperRepository {

    String COLLECTION_NAME = "shoppers";

    /**
     * Saves a new Shopper or updates an existing one.
     * @param shopper The Shopper object to save.
     * @return The saved Shopper with its Firestore document ID.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    Shopper save(Shopper shopper) throws ExecutionException, InterruptedException;

    /**
     * Finds a Shopper by its document ID.
     * @param id The ID of the Shopper.
     * @return An Optional containing the Shopper if found, or empty if not.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    Optional<Shopper> findById(String id) throws ExecutionException, InterruptedException;

    /**
     * Finds a Shopper by their email address.
     * @param email The email address of the Shopper.
     * @return An Optional containing the Shopper if found, or empty if not.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    Optional<Shopper> findByEmail(String email) throws ExecutionException, InterruptedException;

    /**
     * Retrieves all Shoppers from the collection.
     * @return A list of all Shopper objects.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    List<Shopper> findAll() throws ExecutionException, InterruptedException;

    /**
     * Deletes a Shopper by its document ID.
     * @param id The ID of the Shopper to delete.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    void deleteById(String id) throws ExecutionException, InterruptedException;

    /**
     * Clears all documents from the Shopper collection.
     * Primarily used for testing purposes.
     * @throws ExecutionException If an execution error occurs during the Firestore operation.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    void deleteAll() throws ExecutionException, InterruptedException;

    /**
     * @param email
     * @return
     */
    CompletableFuture<Optional<Shopper>> findByEmailAsync(String email);
}
