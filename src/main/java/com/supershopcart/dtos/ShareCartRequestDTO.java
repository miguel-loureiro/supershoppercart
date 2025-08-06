package com.supershopcart.dtos;

import com.supershopcart.enums.SharePermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ShareCartRequestDTO {

    @NotBlank(message = "Cart ID is required")
    private String cartId;

    @NotBlank(message = "Target shopper email is required")
    @Email(message = "Must be a valid email address")
    private String targetShopperEmail;

    @NotNull(message = "Permission is required")
    private SharePermission permission;

    // Constructors, getters, setters
    public ShareCartRequestDTO() {}

    public ShareCartRequestDTO(String cartId, String targetShopperEmail, SharePermission permission) {
        this.cartId = cartId;
        this.targetShopperEmail = targetShopperEmail;
        this.permission = permission;
    }

    public String getCartId() { return cartId; }
    public void setCartId(String cartId) { this.cartId = cartId; }

    public String getTargetShopperEmail() { return targetShopperEmail; }
    public void setTargetShopperEmail(String targetShopperEmail) { this.targetShopperEmail = targetShopperEmail; }

    public SharePermission getPermission() { return permission; }
    public void setPermission(SharePermission permission) { this.permission = permission; }
}
