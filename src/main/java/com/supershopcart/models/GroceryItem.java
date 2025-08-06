package com.supershopcart.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

// Make the class public so it can be accessed from other packages
@Setter
@Getter
public class GroceryItem {

    private String designation;
    private String quantity;
    private boolean purchased;

    public GroceryItem() {
        // No-argument constructor required by Firestore for object mapping
    }

    public GroceryItem(String designation, String quantity) {
        this.designation = designation;
        this.quantity = quantity;
        this.purchased = false;
    }

    public GroceryItem(String designation, String quantity, boolean purchased) {
        this.designation = designation;
        this.quantity = quantity;
        this.purchased = purchased;
    }

    // --- Getters and Setters ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroceryItem that = (GroceryItem) o;
        return purchased == that.purchased &&
                Objects.equals(designation, that.designation) &&
                Objects.equals(quantity, that.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(designation, quantity, purchased);
    }

    @Override
    public String toString() {
        return "GroceryItem{" +
                "designation='" + designation + '\'' +
                ", quantity='" + quantity + '\'' +
                ", purchased=" + purchased +
                '}';
    }
}