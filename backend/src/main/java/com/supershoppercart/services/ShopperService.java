package com.supershoppercart.services;

import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopCartRepository;
import com.supershoppercart.repositories.ShopperRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class ShopperService {

    private final ShopCartRepository shopCartRepository;
    private final ShopperRepository shopperRepository;

    public ShopperService(ShopCartRepository shopCartRepository, ShopperRepository shopperRepository) {
        this.shopCartRepository = shopCartRepository;
        this.shopperRepository = shopperRepository;
    }

    // Shopper-related service methods could go here or in a separate ShopperService
    /**
     * Registers a new shopper.
     * @param email Shopper's email.
     * @param name Shopper's name.
     * @param password Shopper's password (will be hashed).
     * @return The registered Shopper.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     * @throws IllegalArgumentException If a shopper with the given email already exists.
     */
    public Shopper registerShopper(String email, String name, String password)
            throws ExecutionException, InterruptedException, IllegalArgumentException {
        if (shopperRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Shopper with email " + email + " already exists.");
        }
        Shopper newShopper = new Shopper(email, name, password);
        return shopperRepository.save(newShopper);
    }

    /**
     * Gets a shopper by ID.
     * @param shopperId The ID of the shopper.
     * @return An Optional containing the Shopper if found.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    public Optional<Shopper> getShopperById(String shopperId) throws ExecutionException, InterruptedException {
        return shopperRepository.findById(shopperId);
    }

    /**
     * Gets a shopper by email.
     * @param email The email of the shopper.
     * @return An Optional containing the Shopper if found.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    public Optional<Shopper> getShopperByEmail(String email) throws ExecutionException, InterruptedException {
        return shopperRepository.findByEmail(email);
    }

    /**
     * Retrieves all shoppers.
     * @return A list of all shoppers.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    public List<Shopper> getAllShoppers() throws ExecutionException, InterruptedException {
        return shopperRepository.findAll();
    }

    /**
     * Deletes a shopper.
     * @param shopperId The ID of the shopper to delete.
     * @throws ExecutionException If a Firestore operation fails.
     * @throws InterruptedException If the thread is interrupted.
     */
    public void deleteShopper(String shopperId) throws ExecutionException, InterruptedException {
        // Potentially handle orphaned shop carts or set shopperIds to null in carts
        // For simplicity, we'll just delete the shopper here.
        shopperRepository.deleteById(shopperId);
    }

    /**
     * Clears all data in the repositories. Useful for testing.
     */
    public void clearAllData() throws ExecutionException, InterruptedException {
        shopCartRepository.deleteAll();
        shopperRepository.deleteAll();
    }
}
