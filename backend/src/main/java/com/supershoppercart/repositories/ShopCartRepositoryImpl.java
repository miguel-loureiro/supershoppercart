package com.supershoppercart.repositories;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.supershoppercart.models.ShopCart;
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

    private static final String SHOPCARTS_COLLECTION_NAME = "shopcarts";
    private static final String TEMPLATES_COLLECTION_NAME = "shopcartTemplates";

    private final CollectionReference shopCartsCollection;
    private final CollectionReference templatesCollection;

    public ShopCartRepositoryImpl(Firestore firestore) {
        this.shopCartsCollection = firestore.collection(SHOPCARTS_COLLECTION_NAME);
        this.templatesCollection = firestore.collection(TEMPLATES_COLLECTION_NAME);
    }

    @Override
    public ShopCart save(ShopCart shopCart) throws ExecutionException, InterruptedException {
        if (shopCart.getId() == null || shopCart.getId().isEmpty()) {
            // Create new document with auto-generated ID in the regular shopcarts collection
            DocumentReference docRef = shopCartsCollection.add(shopCart).get();
            shopCart.setId(docRef.getId()); // Set the generated ID back to the object
        } else {
            // Update existing document in the regular shopcarts collection
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
                    shopCart.setId(doc.getId()); // Ensure ID is populated
                    return shopCart;
                })
                .filter(java.util.Objects::nonNull) // Filter out any potential null objects
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        shopCartsCollection.document(id).delete().get();
    }

    @Override
    public void deleteAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = shopCartsCollection.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (!documents.isEmpty()) {
            for (QueryDocumentSnapshot doc : documents) {
                doc.getReference().delete().get();
            }
        }
    }

    // --- Template-specific methods ---

    /**
     * Saves a ShopCart as a template in a separate collection.
     * This method always creates a new document.
     * @param templateCart The ShopCart object to save as a template.
     * @return The saved template with its Firestore ID.
     */
    public ShopCart saveTemplate(ShopCart templateCart) throws ExecutionException, InterruptedException {
        // Always create a new document for a template
        DocumentReference docRef = templatesCollection.add(templateCart).get();
        templateCart.setId(docRef.getId());
        return templateCart;
    }

    /**
     * Retrieves a shop cart from the templates collection by its ID.
     * @param id The ID of the template.
     * @return An Optional containing the template ShopCart if found.
     */
    public Optional<ShopCart> findTemplateById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = templatesCollection.document(id).get().get();
        if (snapshot.exists()) {
            ShopCart templateCart = snapshot.toObject(ShopCart.class);
            if (templateCart != null) {
                templateCart.setId(snapshot.getId());
            }
            return Optional.ofNullable(templateCart);
        }
        return Optional.empty();
    }

    /**
     * Finds all shop cart templates.
     * @return A list of all template ShopCarts.
     */
    public List<ShopCart> findAllTemplates() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = templatesCollection.get().get();
        return snapshot.getDocuments().stream()
                .map(doc -> {
                    ShopCart template = doc.toObject(ShopCart.class);
                    template.setId(doc.getId());
                    return template;
                })
                .filter(java.util.Objects::nonNull) // Filter out any potential null objects
                .collect(Collectors.toList());
    }
}
