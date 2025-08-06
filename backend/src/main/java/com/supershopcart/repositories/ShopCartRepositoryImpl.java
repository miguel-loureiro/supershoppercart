package com.supershopcart.repositories;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.supershopcart.models.ShopCart;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore implementation of the ShopCartRepository.
 */
@Repository
public class ShopCartRepositoryImpl implements ShopCartRepository {

    private final CollectionReference shopCartsCollection;

    public ShopCartRepositoryImpl(Firestore firestore) {
        this.shopCartsCollection = firestore.collection(COLLECTION_NAME);
    }

    @Override
    public ShopCart save(ShopCart shopCart) throws ExecutionException, InterruptedException {
        if (shopCart.getId() == null || shopCart.getId().isEmpty()) {
            // Create new document with auto-generated ID
            DocumentReference docRef = shopCartsCollection.add(shopCart).get();
            shopCart.setId(docRef.getId()); // Set the generated ID back to the object
        } else {
            // Update existing document
            shopCartsCollection.document(shopCart.getId()).set(shopCart).get();
        }
        return shopCart;
    }

    @Override
    public Optional<ShopCart> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = shopCartsCollection.document(id).get().get();
        if (snapshot.exists()) {
            ShopCart shopCart = snapshot.toObject(ShopCart.class);
            if (shopCart != null) {
                shopCart.setId(snapshot.getId()); // Ensure the ID is set from the document ID
            }
            return Optional.ofNullable(shopCart);
        }
        return Optional.empty();
    }

    @Override
    public List<ShopCart> findAll() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = shopCartsCollection.get().get();
        return snapshot.getDocuments().stream()
                .map(doc -> {
                    ShopCart shopCart = doc.toObject(ShopCart.class);
                    if (shopCart != null) {
                        shopCart.setId(doc.getId()); // Ensure ID is populated
                    }
                    return shopCart;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        shopCartsCollection.document(id).delete().get();
    }

    @Override
    public void deleteAll() throws ExecutionException, InterruptedException { // <--- ADD THIS METHOD
        ApiFuture<QuerySnapshot> future = shopCartsCollection.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (!documents.isEmpty()) {
            for (QueryDocumentSnapshot doc : documents) {
                doc.getReference().delete().get();
            }
        }
    }
}
