package com.supershoppercart.controllers;

import com.supershoppercart.auth.dto.DevLoginRequestDTO;
import com.supershoppercart.auth.dto.DevLoginResponseDTO;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.services.JwtTokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/dev/auth")
@Profile("dev")
public class DevAuthController {

    private static final Logger logger = LoggerFactory.getLogger(DevAuthController.class);
    private final JwtTokenService jwtTokenService;
    private final ShopperRepository shopperRepository;

    public DevAuthController(JwtTokenService jwtTokenService, ShopperRepository shopperRepository) {
        this.jwtTokenService = jwtTokenService;
        this.shopperRepository = shopperRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<DevLoginResponseDTO> devLogin(@Valid @RequestBody DevLoginRequestDTO request) {
        logger.info("Dev login request received for email: {}", request.getEmail());

        try {
            // Find or create a shopper for this dev user
            Optional<Shopper> existing = shopperRepository.findByEmail(request.getEmail());
            Shopper shopper;

            if (existing.isPresent()) {
                shopper = existing.get();
                logger.debug("Found existing dev shopper: {}", shopper.getEmail());
            } else {
                // Create a new shopper if one doesn't exist for this dev user
                shopper = new Shopper(request.getEmail(), "Dev User");
                shopper = shopperRepository.save(shopper);
                logger.debug("Created new dev shopper: {}", shopper.getEmail());
            }

            // Generate an access token. For simplicity, we don't need a refresh token in dev.
            String accessToken = jwtTokenService.generateAccessToken(shopper.getId(), "dev-device-id");
            logger.info("Successfully generated JWT for dev user: {}", request.getEmail());

            return ResponseEntity.ok(new DevLoginResponseDTO(accessToken));

        } catch (Exception e) {
            logger.error("Failed to process dev login: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new DevLoginResponseDTO("Error: " + e.getMessage()));
        }
    }
}
