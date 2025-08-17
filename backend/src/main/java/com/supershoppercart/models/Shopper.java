package com.supershoppercart.models;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
public class Shopper {

    @DocumentId
    private String id;  // Firestore document ID

    private String email;
    private String name;

    // Optional: used internally, but not required for Google-authenticated users
    private String password;

    // To identify auth provider: "google", "manual", etc.
    private String provider = "google";

    // Only storing cart IDs, not embedded cart documents
    private List<String> shopCartIds = new ArrayList<>();

    // Firestore requires a no-arg constructor
    public Shopper() {}

    public Shopper(String email, String name) {
        this.email = email;
        this.name = name;
        this.provider = "google";
        this.password = null; // No password needed for Google-authenticated users
    }

    public Shopper(String email, String name, String provider) {
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.password = null;
    }

    // --- Overrides ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Shopper shopper)) return false;
        return Objects.equals(id, shopper.id) &&
                Objects.equals(email, shopper.email) &&
                Objects.equals(name, shopper.name) &&
                Objects.equals(provider, shopper.provider) &&
                Objects.equals(shopCartIds, shopper.shopCartIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, name, provider, shopCartIds);
    }

    @Override
    public String toString() {
        return "Shopper{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", provider='" + provider + '\'' +
                ", shopCartIds=" + shopCartIds +
                '}';
    }
}