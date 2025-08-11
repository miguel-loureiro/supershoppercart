package com.supershoppercart.repositories;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.supershoppercart.models.Shopper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore implementation of the ShopperRepository.
 */
@Repository
public class ShopperRepositoryImpl implements ShopperRepository {

    private final CollectionReference shoppersCollection;

    public ShopperRepositoryImpl(Firestore firestore) {
        this.shoppersCollection = firestore.collection(COLLECTION_NAME);
    }

    @Override
    public Shopper save(Shopper shopper) throws ExecutionException, InterruptedException {
        if (shopper.getId() == null || shopper.getId().isEmpty()) {
            // Create new document with auto-generated ID
            DocumentReference docRef = shoppersCollection.add(shopper).get();
            shopper.setId(docRef.getId()); // Set the generated ID back to the object
        } else {
            // Update existing document
            shoppersCollection.document(shopper.getId()).set(shopper).get();
        }
        return shopper;
    }

    @Override
    public Optional<Shopper> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = shoppersCollection.document(id).get().get();
        if (snapshot.exists()) {
            Shopper shopper = snapshot.toObject(Shopper.class);
            if (shopper != null) {
                shopper.setId(snapshot.getId()); // Ensure ID is set
            }
            return Optional.ofNullable(shopper);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Shopper> findByEmail(String email) throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = shoppersCollection.whereEqualTo("email", email).limit(1).get().get();
        if (!snapshot.isEmpty()) {
            DocumentSnapshot doc = snapshot.getDocuments().get(0);
            Shopper shopper = doc.toObject(Shopper.class);
            shopper.setId(doc.getId());
            return Optional.ofNullable(shopper);
        }
        return Optional.empty();
    }

    @Override
    public List<Shopper> findAll() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = shoppersCollection.get().get();
        return snapshot.getDocuments().stream()
                .map(doc -> {
                    Shopper shopper = doc.toObject(Shopper.class);
                    shopper.setId(doc.getId());
                    return shopper;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        shoppersCollection.document(id).delete().get();
    }

    @Override
    public void deleteAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> snapshot = shoppersCollection.get();
        List<QueryDocumentSnapshot> docs = snapshot.get().getDocuments();
        for (DocumentSnapshot doc : docs) {
            doc.getReference().delete().get();
        }
    }
}
