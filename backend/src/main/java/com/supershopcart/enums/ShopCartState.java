package com.supershopcart.enums;

public enum ShopCartState {
    ACTIVE,           // Cart is being actively used
    SHOPPING,         // Someone is currently shopping (all items purchased but cart still active)
    COMPLETED,        // Shopping trip completed, but cart kept for reference/reuse
    ARCHIVED,         // Older completed carts, moved to archive
    TEMPLATE          // Reusable template cart (like "Weekly Groceries")
}
