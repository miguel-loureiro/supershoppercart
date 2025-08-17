package com.supershoppercart.dtos;

import com.supershoppercart.enums.ShopCartState;
import com.supershoppercart.models.ShopCart;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
public class ShopCartSummaryDTO {
    // Getter methods
    @NotBlank private String identifier;
    @NotBlank private String dateKey;
    @NotNull private int itemCount;
    @NotNull private int purchasedCount;
    // Existing getter/setter methods
    @Setter
    private List<String> shopperIds;
    @NotBlank private String createdBy;
    @NotBlank private ShopCartState state;
    @NotBlank private Date createdAt;
    @Setter
    private Date lastModified;
    @NotNull private boolean isTemplate;
    @Setter
    private String templateName;

    public ShopCartSummaryDTO(ShopCart shopCart) {
        this.identifier = shopCart.getId();
        this.dateKey = shopCart.getDateKey();
        this.itemCount = shopCart.getItems().size();
        this.purchasedCount = (int) shopCart.getItems().stream().mapToLong(item -> item.isPurchased() ? 1 : 0).sum();
        this.shopperIds = shopCart.getShopperIds();
        this.createdBy = shopCart.getCreatedBy();
        this.state = shopCart.getState();
        this.createdAt = shopCart.getCreatedAt();
        this.lastModified = shopCart.getLastModified();
        this.isTemplate = shopCart.isTemplate();
        this.templateName = shopCart.getTemplateName();
    }
}
