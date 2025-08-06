package com.supershopcart.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SharePermission {
    VIEW,    // Can view the cart
    EDIT,         // Can add/remove items
    ADMIN         // Can share with others, delete cart
    ;

    public String toLowerCase() {

        return name().toLowerCase();
    }
}
