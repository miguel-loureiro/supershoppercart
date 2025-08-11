package com.supershoppercart.dtos;

import com.supershoppercart.enums.SharePermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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
}
