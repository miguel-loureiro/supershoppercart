package com.supershoppercart.dtos;

import com.supershoppercart.models.Shopper;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Lightweight shopper DTO for when you need basic shopper info
@Setter
@Getter
public class ShopperSummaryDTO {
    @NotBlank private String id;
    @NotBlank private String email;
    @NotBlank private String name;

    public ShopperSummaryDTO() {
    }

    public ShopperSummaryDTO(Shopper shopper) {
        this.id = shopper.getId();
        this.email = shopper.getEmail();
        this.name = shopper.getName();
    }
}

