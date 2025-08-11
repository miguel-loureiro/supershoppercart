package com.supershoppercart.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.supershoppercart.models.RefreshToken;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.security.GoogleTokenVerifier;
import com.supershoppercart.services.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api/v1//auth")
public class AuthController {

    private final JwtTokenService jwtTokenService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final ShopperRepository shopperRepository;
    private final Firestore firestore; // The Firestore instance

    public AuthController(JwtTokenService jwtTokenService, GoogleTokenVerifier googleTokenVerifier, ShopperRepository shopperRepository, Firestore firestore) {
        this.jwtTokenService = jwtTokenService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.shopperRepository = shopperRepository;
        this.firestore = firestore;
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestHeader("Authorization") String authHeader,
                                             @RequestHeader(value = "X-Device-Id", required = false) String deviceId)
            throws Exception {

        // --- Validate input headers ---
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body("Missing X-Device-Id header");
        }

        String idToken = authHeader.substring(7).trim();
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);

        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google token");
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // --- Find or create shopper ---
        Optional<Shopper> existing = shopperRepository.findByEmail(email);
        Shopper shopper;

        if (existing.isPresent()) {
            shopper = existing.get();
        } else {
            // Create new shopper first
            shopper = new Shopper(email, name);
            try {
                shopper = shopperRepository.save(shopper); // Save to Firestore
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to create new shopper: " + e.getMessage());
            }
        }

        // --- Generate tokens ---
        String accessToken = jwtTokenService.generateAccessToken(shopper.getId(), deviceId);
        String refreshToken = jwtTokenService.generateRefreshToken(shopper.getId());

        RefreshToken tokenRecord = new RefreshToken(
                refreshToken,
                shopper.getId(),
                deviceId,
                System.currentTimeMillis() + jwtTokenService.getRefreshTokenExpiration()
        );

        try {
            // Save refresh token (per device)
            firestore.collection("refresh_tokens").document(refreshToken).set(tokenRecord).get();
        } catch (Exception e) {
            // Rollback shopper creation if it was just created
            if (existing.isEmpty()) {
                shopperRepository.deleteById(shopper.getId()); // Cleanup
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to complete login: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody Map<String, String> body) throws Exception {
        String refreshToken = body.get("refreshToken");
        String deviceId = body.get("deviceId");

        if (refreshToken == null || deviceId == null) {
            return ResponseEntity.badRequest().body("Missing refreshToken or deviceId");
        }

        // Ensure this operation completes
        DocumentSnapshot doc = firestore.collection("refresh_tokens").document(refreshToken).get().get();
        if (!doc.exists()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        RefreshToken stored = doc.toObject(RefreshToken.class);
        if (stored == null || !deviceId.equals(stored.getDeviceId()) || stored.getExpiry() < System.currentTimeMillis()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired or device mismatch");
        }

        // Invalidate old token - Ensure this operation completes
        firestore.collection("refresh_tokens").document(refreshToken).delete().get(); // Added .get() to ensure completion

        // Issue new tokens - Use instance methods
        String newAccessToken = jwtTokenService.generateAccessToken(stored.getShopperId(), deviceId);
        String newRefreshToken = jwtTokenService.generateRefreshToken(stored.getShopperId());

        RefreshToken rotated = new RefreshToken(
                newRefreshToken,
                stored.getShopperId(),
                deviceId,
                System.currentTimeMillis() + jwtTokenService.getRefreshTokenExpiration()
        );

        // Ensure this operation completes
        firestore.collection("refresh_tokens").document(newRefreshToken).set(rotated).get();

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody Map<String, String> body) throws Exception {
        String refreshToken = body.get("refreshToken");
        String deviceId = body.get("deviceId");

        if (refreshToken == null || deviceId == null) {
            return ResponseEntity.badRequest().body("Missing refreshToken or deviceId");
        }

        DocumentReference ref = firestore.collection("refresh_tokens").document(refreshToken);
        // Ensure this operation completes
        DocumentSnapshot doc = ref.get().get();

        if (doc.exists()) {
            RefreshToken stored = doc.toObject(RefreshToken.class);
            if (stored != null && deviceId.equals(stored.getDeviceId())) {
                ref.delete().get(); // Invalidate token for that device only - Added .get()
                return ResponseEntity.ok(Map.of("message", "Logged out from device"));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid logout request");
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllDevices(@Valid @RequestBody Map<String, String> body) throws Exception {
        String shopperId = body.get("shopperId");

        if (shopperId == null || shopperId.isBlank()) {
            return ResponseEntity.badRequest().body("Missing shopperId");
        }

        ApiFuture<QuerySnapshot> snapshot = firestore.collection("refresh_tokens")
                .whereEqualTo("shopperId", shopperId)
                .get();

        List<QueryDocumentSnapshot> docs = snapshot.get().getDocuments();
        for (DocumentSnapshot doc : docs) {
            doc.getReference().delete().get(); // Ensure each delete operation completes
        }

        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }
}
