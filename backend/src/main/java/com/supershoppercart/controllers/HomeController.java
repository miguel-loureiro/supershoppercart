package com.supershoppercart.controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Home Controller for Super Shopper Cart API
 * Provides health check endpoints
 */
@RestController
public class HomeController {

    private final Supplier<ClassPathResource> resourceSupplier;

    public HomeController() {
        this(() -> new ClassPathResource("static/health-check.html"));
    }

    public HomeController(Supplier<ClassPathResource> resourceSupplier) {
        this.resourceSupplier = resourceSupplier;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> home() {
        try {
            ClassPathResource htmlResource = resourceSupplier.get();

            if (!htmlResource.exists()) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.setPragma("no-cache");
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setExpires(0);

            return ResponseEntity.ok().headers(headers).body(htmlResource);

        } catch (Exception e) {
            System.err.println("Error while loading health-check.html: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> statusCheck() {
        return ResponseEntity.ok(Collections.singletonMap("status", "OK"));
    }
}
