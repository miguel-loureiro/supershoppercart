package com.supershopcart.controllers;

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

/**
 * Home Controller for Super Shopper Cart API
 * Provides health check endpoints
 */
@RestController
public class HomeController {

    /**
     * Health check endpoint that serves the main HTML page.
     * @return HTML content as ResponseEntity
     * @throws IOException if the HTML file cannot be read
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> home() throws IOException {
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
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(htmlResource);

        } catch (Exception e) {
            // Return 500 Internal Server Error if something goes wrong
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint for API health check that returns a JSON response.
     * The HTML page uses this endpoint to check the backend status.
     * @return JSON object with status information.
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> statusCheck() {
        // This simple endpoint just confirms the API is running
        return ResponseEntity.ok()
                .body(Collections.singletonMap("status", "OK"));
    }
}
