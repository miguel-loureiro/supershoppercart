package com.supershopcart.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HomeController
 *
 * Uses standalone MockMvc setup to test the controller in isolation
 * without loading the Spring application context or security configurations.
 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @InjectMocks
    private HomeController homeController;

    @Test
    void testHealthCheckEndpoint_ShouldReturnAliveMessage() throws Exception {
        // Given: Standalone MockMvc setup
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Perform GET request to "/" and verify the response
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("Super Shopper Cart API is alive and kicking"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE + ";charset=ISO-8859-1"));
    }

    @Test
    void testHealthCheckEndpoint_WithAcceptHeader() throws Exception {
        // Given: Standalone MockMvc setup with Accept header
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Verify the endpoint works with specific Accept headers
        mockMvc.perform(get("/")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("Super Shopper Cart API is alive and kicking"));
    }

    @Test
    void testHealthCheckEndpoint_MultipleRequests() throws Exception {
        // Given: Standalone MockMvc setup
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Perform multiple requests to verify consistency
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Super Shopper Cart API is alive and kicking"));
        }
    }

    @Test
    void testHealthCheckEndpoint_OnlySupportsGetMethod() throws Exception {
        // Given: Standalone MockMvc setup
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Verify that POST method returns 405 Method Not Allowed
        mockMvc.perform(post("/"))
                .andExpect(status().isMethodNotAllowed());

        // Verify that PUT method returns 405 Method Not Allowed
        mockMvc.perform(put("/"))
                .andExpect(status().isMethodNotAllowed());

        // Verify that DELETE method returns 405 Method Not Allowed
        mockMvc.perform(delete("/"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testHealthCheckEndpoint_ResponseHeaders() throws Exception {
        // Given: Standalone MockMvc setup
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Verify response headers are correct
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain;charset=ISO-8859-1"))
                .andExpect(header().exists("Content-Length"));
    }

    @Test
    void testDirectMethodCall_ShouldReturnCorrectMessage() {
        // Given: Direct controller instance
        // When: Call the method directly
        String result = homeController.home();

        // Then: Verify the response
        assert result.equals("Super Shopper Cart API is alive and kicking");
    }
}