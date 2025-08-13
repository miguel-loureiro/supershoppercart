package com.supershoppercart.dtos;

import com.supershoppercart.models.GroceryItem;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateShopCartRequestDTO {
    @NotBlank
    private String name;

    @NotBlank
    private String dateKey;

    private List<GroceryItem> items = new ArrayList<>();
    private List<String> shopperIds = new ArrayList<>();
    private boolean isPublic = false;
    private boolean isTemplate = false;
}
