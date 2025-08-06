package com.supershopcart.models; // Ensure this matches your package structure

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import com.supershopcart.enums.SharePermission;
import com.supershopcart.enums.ShopCartState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a shopping cart in the system.
 * This model is designed for direct mapping to Firestore documents.
 * It stores IDs of associated shoppers, but not the full Shopper objects
 * to avoid complex nested object serialization issues in Firestore.
 */
@Setter
@Getter
public class ShopCart {

    @DocumentId // Maps this field to the Firestore document ID
    private String id; // The document ID of the shop cart
    private String name;

    // Default to empty lists to avoid NullPointerExceptions during Firestore deserialization
    private List<GroceryItem> items = new ArrayList<>();
    private List<String> shopperIds = new ArrayList<>(); // Store only shopper IDs for association
    private List<SharePermissionEntry> sharePermissions = new ArrayList<>(); // List of shopper permissions

    private String dateKey; // A key for the date, perhaps for grouping/filtering
    private String createdBy; // ID of the shopper who created the cart
    private boolean isPublic = false; // For future public sharing feature

    // Firestore will automatically populate these timestamps on creation/update
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date lastModified;

    // State management fields
    private ShopCartState state = ShopCartState.ACTIVE; // Sensible default state
    private Date lastInteraction;
    private Date completedAt; // When shopping trip was completed
    private String currentShopper; // Who is currently shopping (if any)
    private boolean isTemplate = false; // Reusable template cart
    private String templateName; // Name for template carts

    public ShopCart() {
        // No-argument constructor required by Firestore for object mapping.
        // Fields with @DocumentId or @ServerTimestamp are handled by Firestore.
        // Lists are initialized to empty to prevent NPEs.
    }

    // --- Getters and Setters ---

    // --- Helper methods to work with permissions ---

    /**
     * Retrieves the SharePermission for a given shopper ID.
     *
     * @param shopperId The ID of the shopper to check permissions for.
     * @return The SharePermission if found, otherwise null.
     */
    public SharePermission getPermissionForShopper(String shopperId) {
        return sharePermissions.stream()
                .filter(entry -> entry.getShopperId().equals(shopperId))
                .map(SharePermissionEntry::getPermission)
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds or updates the sharing permission for a specific shopper.
     *
     * @param shopperId The ID of the shopper.
     * @param permission The permission level to set.
     */
    public void addOrUpdatePermission(String shopperId, SharePermission permission) {
        // Remove existing permission for this shopper if it exists
        sharePermissions.removeIf(entry -> entry.getShopperId().equals(shopperId));
        // Add new permission
        sharePermissions.add(new SharePermissionEntry(shopperId, permission));
        this.updateLastInteraction(); // Update interaction time on permission change
    }

    /**
     * Removes the sharing permission for a specific shopper.
     *
     * @param shopperId The ID of the shopper whose permission to remove.
     * @return true if a permission was removed, false otherwise.
     */
    public boolean removePermission(String shopperId) {
        boolean removed = sharePermissions.removeIf(entry -> entry.getShopperId().equals(shopperId));
        if (removed) {
            this.updateLastInteraction(); // Update interaction time if permission removed
        }
        return removed;
    }

    // --- State Management and Lifecycle Methods ---

    /**
     * Updates the `lastInteraction` field to the current date and time.
     * This should be called whenever the cart is modified.
     * Note: `lastModified` is handled by Firestore's `@ServerTimestamp`.
     */
    private void updateLastInteraction() {
        this.lastInteraction = new Date();
    }

    /**
     * Better state update logic for shared shopping, based on item purchase status.
     */
    public void updateStateBasedOnItems() {
        if (items == null || items.isEmpty()) {
            this.state = ShopCartState.ACTIVE;
            this.updateLastInteraction();
            return;
        }

        boolean allPurchased = items.stream().allMatch(GroceryItem::isPurchased);

        // Don't automatically mark as COMPLETED - let shoppers decide
        if (allPurchased && this.state == ShopCartState.ACTIVE) {
            this.state = ShopCartState.SHOPPING; // Intermediate state
        } else if (!allPurchased && this.state == ShopCartState.SHOPPING) {
            this.state = ShopCartState.ACTIVE; // Back to active if items added
        }
        this.updateLastInteraction();
    }

    /**
     * Marks the shopping trip as completed (manual action by a shopper).
     *
     * @param shopperId The ID of the shopper who completed the trip.
     */
    public void completeShoppingTrip(String shopperId) {
        this.state = ShopCartState.COMPLETED;
        this.completedAt = new Date();
        this.currentShopper = null; // No one is actively shopping anymore
        this.updateLastInteraction();
    }

    /**
     * Starts a new shopping session from a completed cart,
     * resetting item purchase status for reuse.
     */
    public void startNewShoppingSession() {
        // Reset purchase status but keep items for reuse
        if (this.items != null) {
            this.items.forEach(item -> item.setPurchased(false));
        }
        this.state = ShopCartState.ACTIVE;
        this.completedAt = null;
        this.currentShopper = null;
        this.updateLastInteraction();
    }

    /**
     * Converts the current cart into a reusable template.
     *
     * @param templateName The name for the new template.
     */
    public void convertToTemplate(String templateName) {
        this.isTemplate = true;
        this.templateName = templateName;
        this.state = ShopCartState.TEMPLATE;
        // Reset purchase status for template
        if (this.items != null) {
            this.items.forEach(item -> item.setPurchased(false));
        }
        this.updateLastInteraction();
    }

    /**
     * Creates a new active shopping cart based on this template cart.
     *
     * @param createdBy The ID of the shopper creating the new cart.
     * @param shopperIds A list of shopper IDs to initially share the new cart with.
     * @return A new `ShopCart` instance.
     * @throws IllegalStateException if the current cart is not a template.
     */
    public ShopCart createFromTemplate(String createdBy, List<String> shopperIds) {
        if (!this.isTemplate) {
            throw new IllegalStateException("Can only create from template carts");
        }

        ShopCart newCart = new ShopCart(); // Creates a new instance, ID, timestamps will be assigned by Firestore on save
        newCart.setId(UUID.randomUUID().toString());
        newCart.setCreatedBy(createdBy);
        newCart.setShopperIds(new ArrayList<>(shopperIds)); // Ensure a new mutable list
        newCart.setState(ShopCartState.ACTIVE); // New cart starts active
        newCart.setDateKey(LocalDate.now().toString()); // Set dateKey for the new cart

        // Copy items but reset purchase status
        List<GroceryItem> templateItems = this.items != null ? this.items.stream()
                .map(item -> {
                    GroceryItem newItem = new GroceryItem();
                    newItem.setDesignation(item.getDesignation());
                    newItem.setQuantity(item.getQuantity());
                    newItem.setPurchased(false); // Always start unpurchased
                    return newItem;
                })
                .collect(Collectors.toList()) : new ArrayList<>();

        newCart.setItems(templateItems);
        newCart.updateLastInteraction(); // Will set lastInteraction and lastModified for the new cart
        return newCart;
    }

    /**
     * Checks if the cart should be moved to an archived state.
     * Criteria: Must be in COMPLETED state and completed at least 6 months ago.
     *
     * @return true if the cart should be archived, false otherwise.
     */
    public boolean shouldBeArchived() {
        if (completedAt == null || state != ShopCartState.COMPLETED) {
            return false;
        }

        long sixMonthsInMillis = 180L * 24 * 60 * 60 * 1000L; // Approximately 6 months
        long timeSinceCompletion = System.currentTimeMillis() - completedAt.getTime();

        return timeSinceCompletion > sixMonthsInMillis;
    }

    /**
     * Archives the current completed cart.
     */
    public void archive() {
        this.state = ShopCartState.ARCHIVED;
        this.updateLastInteraction(); // Update interaction time when archiving
        // Keep shopper associations for archived carts
    }

    // --- Permission-aware Action Methods ---

    /**
     * Checks if the given shopper has permission to edit the cart.
     *
     * @param shopperId The ID of the shopper attempting to edit.
     * @return true if the shopper can edit, false otherwise.
     */
    public boolean canEdit(String shopperId) {
        // Cart creator implicitly has ADMIN permission
        if (shopperId.equals(this.createdBy)) {
            return true;
        }
        SharePermission permission = getPermissionForShopper(shopperId);
        return permission == SharePermission.EDIT || permission == SharePermission.ADMIN;
    }

    /**
     * Checks if the given shopper has permission to delete the cart.
     *
     * @param shopperId The ID of the shopper attempting to delete.
     * @return true if the shopper can delete, false otherwise.
     */
    public boolean canDelete(String shopperId) {
        // Only creator or ADMIN can delete
        if (shopperId.equals(this.createdBy)) {
            return true;
        }
        SharePermission permission = getPermissionForShopper(shopperId);
        return permission == SharePermission.ADMIN;
    }

    /**
     * Checks if the given shopper has permission to mark the shopping trip as complete.
     *
     * @param shopperId The ID of the shopper attempting to complete.
     * @return true if the shopper can complete, false otherwise.
     */
    public boolean canComplete(String shopperId) {
        // Anyone with EDIT or ADMIN permission can complete shopping
        return canEdit(shopperId);
    }

    // --- Standard Object Methods (equals, hashCode, toString) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopCart shopCart = (ShopCart) o;
        // For entities mapped to Firestore documents, 'id' is typically sufficient for equality.
        return Objects.equals(id, shopCart.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ShopCart{" +
                "id='" + id + '\'' +
                ", dateKey='" + dateKey + '\'' +
                ", items=" + (items != null ? items.size() : 0) + " items" +
                ", shopperIds=" + shopperIds.size() + " shoppers" +
                ", createdBy='" + createdBy + '\'' +
                ", sharePermissions=" + (sharePermissions != null ? sharePermissions.size() : 0) + " entries" +
                ", isPublic=" + isPublic +
                ", createdAt=" + createdAt +
                ", lastModified=" + lastModified +
                ", state=" + state +
                ", lastInteraction=" + lastInteraction +
                ", completedAt=" + completedAt +
                ", currentShopper='" + currentShopper + '\'' +
                ", isTemplate=" + isTemplate +
                ", templateName='" + templateName + '\'' +
                '}';
    }
}