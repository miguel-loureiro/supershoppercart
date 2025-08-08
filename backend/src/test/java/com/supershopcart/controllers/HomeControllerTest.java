package com.supershopcart.controllers;

import io.jsonwebtoken.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
        * Unit tests for HomeController that serves HTML content
 *
         * Uses standalone MockMvc setup to test the controller in isolation
 * without loading the Spring application context or security configurations.
 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @InjectMocks
    private HomeController homeController;

    @Test
    void testHealthCheckEndpoint_ShouldReturnHtmlContent() throws Exception {
        // Given: Standalone MockMvc setup
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Perform GET request to "/" and verify the response
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML_VALUE));
    }

    @Test
    void testHealthCheckEndpoint_WithAcceptHeader() throws Exception {
        // Given: Standalone MockMvc setup with Accept header
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Verify the endpoint works with HTML Accept headers
        mockMvc.perform(get("/")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML_VALUE));
    }

    @Test
    void testHealthCheckEndpoint_MultipleRequests() throws Exception {
        // Given: Standalone MockMvc setup
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Perform multiple requests to verify consistency
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.TEXT_HTML_VALUE));
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
                .andExpect(header().string("Content-Type", MediaType.TEXT_HTML_VALUE))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "Thu, 01 Jan 1970 00:00:00 GMT"))
                .andExpect(header().exists("Content-Length"));
    }

    @Test
    void testDirectMethodCall_ShouldReturnHtmlResource() throws IOException {
        // Given: Direct controller instance

        // When: Call the method directly
        ResponseEntity<ClassPathResource> result = homeController.home();

        // Then: Verify the response
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(MediaType.TEXT_HTML, result.getHeaders().getContentType());
        assertEquals("no-cache, no-store, must-revalidate", result.getHeaders().getCacheControl());
        assertEquals("no-cache", result.getHeaders().getPragma());
        assertEquals(0L, result.getHeaders().getExpires());
    }

    @Test
    void testDirectMethodCall_WhenHtmlFileExists() throws IOException {
        // Given: Direct controller instance

        // When: Call the method directly
        ResponseEntity<ClassPathResource> result = homeController.home();

        // Then: Verify the response contains the HTML resource
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().getPath().contains("health-check.html"));
    }

    @Test
    void testHealthCheckEndpoint_CacheControlHeaders() throws Exception {
        // Given: Standalone MockMvc setup
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Verify cache control headers are set for health check
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "Thu, 01 Jan 1970 00:00:00 GMT"));
    }

    @Test
    void testHealthCheckEndpoint_ContentTypeProducesHtml() throws Exception {
        // Given: Standalone MockMvc setup
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();

        // When & Then: Verify the endpoint produces HTML content type
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}