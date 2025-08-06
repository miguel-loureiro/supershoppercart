package com.supershopcart.dtos;

import com.supershopcart.models.Shopper;
import lombok.Getter;
import lombok.Setter;

// Lightweight shopper DTO for when you need basic shopper info
@Setter
@Getter
public class ShopperSummaryDTO {
    private String id;
    private String email;
    private String name;

    public ShopperSummaryDTO() {
    }

    public ShopperSummaryDTO(Shopper shopper) {
        this.id = shopper.getId();
        this.email = shopper.getEmail();
        this.name = shopper.getName();
    }
}

