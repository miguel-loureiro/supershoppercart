package com.supershopcart.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Home Controller for Super Shopper Cart API
 * Provides health check endpoint
 */
@RestController
public class HomeController {

    /**
     * Health check endpoint
     * @return status message indicating the API is running
     */
    @GetMapping("/")
    public String home() {
        return "Super Shopper Cart API is alive and kicking";
    }
}
