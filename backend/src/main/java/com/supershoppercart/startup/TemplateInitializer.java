package com.supershoppercart.startup;

import com.supershoppercart.models.GroceryItem;
import com.supershoppercart.models.ShopCart;
import com.supershoppercart.services.ShopCartService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class TemplateInitializer implements CommandLineRunner {

    private final ShopCartService shopCartService;

    public TemplateInitializer(ShopCartService shopCartService) {
        this.shopCartService = shopCartService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Define your dummy template cart
        ShopCart template = new ShopCart();
        template.setName("Weekly Shopping Template");

        // Add some example grocery items
        // The GroceryItem constructor now expects a String for quantity.
        List<GroceryItem> items = List.of(
                new GroceryItem("Milk", "1"),
                new GroceryItem("Bread", "1"),
                new GroceryItem("Eggs", "12"),
                new GroceryItem("Cheese", "1")
        );
        template.setItems(items);

        try {
            // Save the template using the new service method
            shopCartService.saveShopCartAsTemplate(template);
            System.out.println("Dummy shopcart template created and saved successfully.");
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to save dummy shopcart template: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
