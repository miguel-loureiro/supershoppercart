package com.supershopcart.enums;

public enum SharePermission {
    VIEW,    // Can view the cart
    EDIT,         // Can add/remove items
    ADMIN         // Can share with others, delete cart
    ;

    public String toLowerCase() {

        return name().toLowerCase();
    }
}
