package com.supershopcart.dtos;


import com.supershopcart.enums.ShopCartState;
import com.supershopcart.models.GroceryItem;
import com.supershopcart.models.SharePermissionEntry;
import com.supershopcart.models.ShopCart;
import com.supershopcart.models.Shopper;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Apply the DTO pattern to expose only the necessary ShopCart fields in your API,
 * especially the identifier and dateKey for filtering, along with associated items and shoppers.
 */
@Setter
@Getter
public class ShopCartDetailDTO {
    // Getters and setters...
    private String identifier;
    private String name;
    private String dateKey;
    private List<GroceryItem> items;
    private List<String> shopperIds;
    private List<ShopperSummaryDTO> shoppers; // Include shopper details for detailed view
    private String createdBy;
    private List<SharePermissionEntry> sharePermissions; // This is the field that uses SharePermissionEntry
    private ShopCartState state;
    private Date createdAt;
    private Date lastModified;
    private Date lastInteraction;
    private boolean isTemplate;
    private String templateName;

    // Constructor to handle identifier from document ID
    public ShopCartDetailDTO(String identifier, ShopCart shopCart) {
        this.identifier = identifier;
        this.name = shopCart.getName();
        this.dateKey = shopCart.getDateKey();
        this.items = shopCart.getItems();
        this.shopperIds = shopCart.getShopperIds();
        this.createdBy = shopCart.getCreatedBy();
        this.sharePermissions = shopCart.getSharePermissions();
        this.state = shopCart.getState();
        this.createdAt = shopCart.getCreatedAt();
        this.lastModified = shopCart.getLastModified();
        this.lastInteraction = shopCart.getLastInteraction();
        this.isTemplate = shopCart.isTemplate();
        this.templateName = shopCart.getTemplateName();
        this.shoppers = new ArrayList<>(); // Populated by service later
    }
}