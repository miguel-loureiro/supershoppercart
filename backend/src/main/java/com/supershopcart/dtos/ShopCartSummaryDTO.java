package com.supershopcart.dtos;

import com.supershopcart.enums.ShopCartState;
import com.supershopcart.models.ShopCart;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;

public class ShopCartSummaryDTO {
    @NotBlank private String identifier;
    @NotBlank private String dateKey;
    @NotNull private int itemCount;
    @NotNull private int purchasedCount;
    private List<String> shopperIds;
    @NotBlank private String createdBy;
    @NotBlank private ShopCartState state;
    @NotBlank private Date createdAt;
    private Date lastModified;
    @NotNull private boolean isTemplate;
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

    public List<String> getShopperIds() {
        return shopperIds;
    }

    public void setShopperIds(List<String> shopperIds) {
        this.shopperIds = shopperIds;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}
