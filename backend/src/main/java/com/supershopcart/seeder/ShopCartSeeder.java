package com.supershopcart.seeder;

import com.supershopcart.models.GroceryItem;
import com.supershopcart.models.ShopCart;
import com.supershopcart.models.Shopper;
import com.supershopcart.repositories.ShopCartRepository;
import com.supershopcart.repositories.ShopperRepository;
import com.supershopcart.services.ShopCartService;
// Import Qualifier
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

// Add a logger for this class
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Profile("dev-emulator") // Only runs when this profile is active
public class ShopCartSeeder implements CommandLineRunner {


    // go to http://127.0.0.1:4001/firestore/default/data/ to see the data populated !!!
    private static final Logger logger = LoggerFactory.getLogger(ShopCartSeeder.class);
    private final ShopCartService shopCartService;
    private final ShopCartRepository shopCartRepository; // Inject ShopCartRepository directly
    private final ShopperRepository shopperRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Updated constructor to inject both repositories
    public ShopCartSeeder(ShopCartService shopCartService,
                          ShopCartRepository shopCartRepository, // New injection
                          ShopperRepository shopperRepository) {
        this.shopCartService = shopCartService;
        this.shopCartRepository = shopCartRepository; // Assign new injection
        this.shopperRepository = shopperRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("🚀 Running ShopCartSeeder for 'dev-emulator' profile...");

        // Clear existing data before seeding to ensure a clean state for testing
        logger.info("Clearing existing data from Firestore emulator...");
        try {
            // Clear ShopCarts first, as they contain shopper IDs (though not strictly necessary for deletion order)
            shopCartRepository.deleteAll(); // Requires this method in ShopCartRepository
            logger.info("All ShopCart documents cleared.");

            // Then clear Shoppers
            shopperRepository.deleteAll(); // Requires this method in ShopperRepository
            logger.info("All Shopper documents cleared.");
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error clearing existing data: {}", e.getMessage());
            throw new RuntimeException("Failed to clear existing data before seeding", e);
        }
        logger.info("Existing data cleared successfully.");


        // 1. Seed Shoppers
        logger.info("Seeding shoppers...");
        Shopper shopper1;
        Shopper shopper2;
        Shopper shopper3;

        try {
            shopper1 = new Shopper("seeder.alice@example.com", "Alice Seeder", passwordEncoder.encode("pass123"));
            shopper1.setShopCartIds(new ArrayList<>()); // Initialize list
            shopper1 = shopperRepository.save(shopper1); // Save and get the ID back

            shopper2 = new Shopper("seeder.bob@example.com", "Bob Seeder", passwordEncoder.encode("securepass"));
            shopper2.setShopCartIds(new ArrayList<>()); // Initialize list
            shopper2 = shopperRepository.save(shopper2);

            shopper3 = new Shopper("seeder.charlie@example.com", "Charlie Seeder", passwordEncoder.encode("charliepass"));
            shopper3.setShopCartIds(new ArrayList<>()); // Initialize list
            shopper3 = shopperRepository.save(shopper3);

            logger.info("Seeded Shoppers: {}, {}, {}", shopper1.getEmail(), shopper2.getEmail(), shopper3.getEmail());
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed to seed shoppers: {}", e.getMessage());
            throw new RuntimeException("Failed to seed shoppers", e);
        }

        // Get today's date key
        String todayDateKey = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String yesterdayDateKey = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE);

        // 2. Seed ShopCarts
        logger.info("Seeding shop carts...");

        try {
            // Cart 1: Alice's cart for today
            List<GroceryItem> aliceItems = Arrays.asList(
                    new GroceryItem("Organic Milk", "1 liter"),
                    new GroceryItem("Whole Wheat Bread", "1 loaf"),
                    new GroceryItem("Eggs", "1 dozen")
            );
            // Use shopper IDs (from the created shoppers) for linking
            ShopCart cart1 = shopCartService.createShopCart(todayDateKey, aliceItems, Collections.singletonList(shopper1.getEmail())); // Pass email
            logger.info("Created ShopCart {} for Alice.", cart1.getId());

            // Cart 2: Bob's cart for today, with one item purchased
            List<GroceryItem> bobItems = Arrays.asList(
                    new GroceryItem("Chicken Breast", "500g"),
                    new GroceryItem("Broccoli", "1 head"),
                    new GroceryItem("Rice", "1 kg")
            );
            ShopCart cart2 = shopCartService.createShopCart(todayDateKey, bobItems, Collections.singletonList(shopper2.getEmail())); // Pass email
            shopCartService.markItemAsPurchased(cart2.getId(), "Broccoli"); // Mark one item as purchased
            logger.info("Created ShopCart {} for Bob, marked Broccoli as purchased.", cart2.getId());

            // Cart 3: Shared cart for yesterday (Alice & Charlie)
            List<GroceryItem> sharedItems = Arrays.asList(
                    new GroceryItem("Coffee Beans", "250g"),
                    new GroceryItem("Sugar", "500g")
            );
            ShopCart cart3 = shopCartService.createShopCart(yesterdayDateKey, sharedItems, Arrays.asList(shopper1.getEmail(), shopper3.getEmail())); // Pass emails
            logger.info("Created shared ShopCart {} for Alice and Charlie.", cart3.getId());

            // Cart 4: Charlie's individual cart for today
            List<GroceryItem> charlieItems = Arrays.asList(
                    new GroceryItem("Yogurt", "4 units"),
                    new GroceryItem("Fruit Mix", "1 pack")
            );
            ShopCart cart4 = shopCartService.createShopCart(todayDateKey, charlieItems, Collections.singletonList(shopper3.getEmail())); // Pass email
            logger.info("Created ShopCart {} for Charlie.", cart4.getId());

            logger.info("✅ ShopCartSeeder finished. {} shoppers and {} carts seeded.",
                    shopperRepository.findAll().size(), shopCartService.getAllShopCarts().size()); // Get actual counts
        } catch (ExecutionException | InterruptedException | IllegalArgumentException e) { // Catch IllegalArgumentException for shopper not found
            logger.error("Failed to seed shop carts: {}", e.getMessage());
            throw new RuntimeException("Failed to seed shop carts", e);
        }
    }
}