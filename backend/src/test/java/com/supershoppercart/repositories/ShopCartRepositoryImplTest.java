package com.supershoppercart.repositories;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.supershoppercart.models.ShopCart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopCartRepositoryImplTest {

    @Mock
    private Firestore firestore;

    @Mock
    private CollectionReference templatesCollection;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private DocumentSnapshot documentSnapshot;

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private ApiFuture<DocumentReference> docRefFuture;

    @Mock
    private ApiFuture<DocumentSnapshot> docSnapFuture;

    @Mock
    private ApiFuture<QuerySnapshot> queryFuture;

    @InjectMocks
    private ShopCartRepositoryImpl shopCartRepository;

    @BeforeEach
    void setUp() {
            MockitoAnnotations.openMocks(this);

            // Mock the return value for shopcartTemplates collection immediately
            when(firestore.collection("shopcartTemplates")).thenReturn(templatesCollection);
            when(firestore.collection("shopcarts")).thenReturn(mock(CollectionReference.class));

            // Now instantiate repository AFTER mocks are configured
            shopCartRepository = new ShopCartRepositoryImpl(firestore);
        }

    @Test
    void testSaveTemplate() throws ExecutionException, InterruptedException {
        // Arrange
        ShopCart cart = new ShopCart();
        cart.setName("Test Template");

        when(templatesCollection.add(cart)).thenReturn(docRefFuture);
        when(docRefFuture.get()).thenReturn(documentReference);
        when(documentReference.getId()).thenReturn("template123");

        // Act
        ShopCart savedCart = shopCartRepository.saveTemplate(cart);

        // Assert
        assertEquals("template123", savedCart.getId());
        verify(templatesCollection).add(cart);
    }

    @Test
    void testFindTemplateById_Found() throws Exception {
        when(templatesCollection.document("template123")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(docSnapFuture);
        when(docSnapFuture.get()).thenReturn(documentSnapshot);

        ShopCart expectedCart = new ShopCart();
        expectedCart.setName("Template Name");

        when(documentSnapshot.exists()).thenReturn(true);
        when(documentSnapshot.toObject(ShopCart.class)).thenReturn(expectedCart);
        when(documentSnapshot.getId()).thenReturn("template123");

        Optional<ShopCart> result = shopCartRepository.findTemplateById("template123");

        assertTrue(result.isPresent());
        assertEquals("template123", result.get().getId());
        assertEquals("Template Name", result.get().getName());
    }

    @Test
    void testFindTemplateById_NotFound() throws Exception {
        when(templatesCollection.document("notfound")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(docSnapFuture);
        when(docSnapFuture.get()).thenReturn(documentSnapshot);

        when(documentSnapshot.exists()).thenReturn(false);

        Optional<ShopCart> result = shopCartRepository.findTemplateById("notfound");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindAllTemplates() throws Exception {
        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);

        ShopCart cart1 = new ShopCart();
        cart1.setName("Cart1");
        ShopCart cart2 = new ShopCart();
        cart2.setName("Cart2");

        when(doc1.toObject(ShopCart.class)).thenReturn(cart1);
        when(doc1.getId()).thenReturn("id1");
        when(doc2.toObject(ShopCart.class)).thenReturn(cart2);
        when(doc2.getId()).thenReturn("id2");

        when(templatesCollection.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(List.of(doc1, doc2));

        List<ShopCart> templates = shopCartRepository.findAllTemplates();

        assertEquals(2, templates.size());
        assertEquals("id1", templates.get(0).getId());
        assertEquals("id2", templates.get(1).getId());
    }
}