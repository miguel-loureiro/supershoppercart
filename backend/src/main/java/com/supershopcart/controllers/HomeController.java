package com.supershopcart.controllers;



import io.jsonwebtoken.io.IOException;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Home Controller for Super Shopper Cart API
 * Provides health check endpoint
 */
@RestController
public class HomeController {

    /**
     * Health check endpoint that serves an HTML page
     * @return HTML content as ResponseEntity
     * @throws IOException if the HTML file cannot be read
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<ClassPathResource> home() throws IOException {
        try {
            // Load the HTML file from classpath (src/main/resources/)
            ClassPathResource htmlResource = new ClassPathResource("static/health-check.html");

            // Check if the file exists
            if (!htmlResource.exists()) {
                // Fallback: return 404 if HTML file not found
                return ResponseEntity.notFound().build();
            }

            // Set appropriate headers for HTML content
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.setPragma("no-cache");
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(htmlResource);

        } catch (Exception e) {
            // Return 500 Internal Server Error if something goes wrong
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
