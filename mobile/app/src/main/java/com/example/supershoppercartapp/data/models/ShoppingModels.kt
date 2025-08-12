package com.example.supershoppercartapp.data.models

data class ShopCart(
    val id: String,
    val dateKey: String,
    val items: List<GroceryItem>,
    val shopperIds: List<String>
)

data class ShopCartDetailDTO(
    val id: String,
    val dateKey: String,
    val items: List<GroceryItem>,
    val shopperIds: List<String>,
    val sharedWith: List<String> = emptyList()
)

data class GroceryItem(
    val designation: String,
    val quantity: Int = 1,
    val isPurchased: Boolean = false,
    val category: String? = null
)

data class ShareCartRequest(
    val targetShopperEmail: String,
    val permission: String = "READ_WRITE"
)