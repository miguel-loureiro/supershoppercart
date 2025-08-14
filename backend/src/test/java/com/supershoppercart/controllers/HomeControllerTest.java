package com.supershoppercart.controllers;

import io.jsonwebtoken.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
    * Unit tests for HomeController that serves HTML content
    * Uses standalone MockMvc setup to test the controller in isolation
    * without loading the Spring application context or security configurations.
 */

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeController Tests")
class HomeControllerTest {

    @Mock
    private Supplier<ClassPathResource> resourceSupplier;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();
    }

    // ---
    // Testes de caminho feliz (Happy Path) para o endpoint "/"
    // ---

    @Test
    @DisplayName("Should return 200 OK and HTML content when file exists")
    void shouldReturnHtmlContentWhenFileExists() throws Exception {
        // Dado (Given)
        ClassPathResource mockResource = mock(ClassPathResource.class);
        when(resourceSupplier.get()).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream("<h1>Test</h1>".getBytes()));

        // Quando & Então (When & Then)
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(content().string("<h1>Test</h1>")); // Verificar o conteúdo
    }

    @Test
    @DisplayName("Should set correct HTTP headers for HTML response")
    void shouldSetCorrectHttpHeadersForHtmlResponse() throws Exception {
        // Dado (Given)
        ClassPathResource mockResource = mock(ClassPathResource.class);
        when(resourceSupplier.get()).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(true);

        // Quando & Então (When & Then)
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.TEXT_HTML_VALUE))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Expires", "Thu, 01 Jan 1970 00:00:00 GMT"));
    }

    // ---
    // Testes para cenários de erro e exceção do endpoint "/"
    // ---

    @Test
    @DisplayName("Should return 404 Not Found when HTML file does not exist")
    void shouldReturn404WhenFileDoesNotExist() throws Exception {
        // Dado (Given)
        ClassPathResource mockResource = mock(ClassPathResource.class);
        when(resourceSupplier.get()).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(false);

        // Quando & Então (When & Then)
        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when an IOException occurs")
    void shouldReturn500WhenIOExceptionOccurs() throws Exception {
        // Dado (Given)
        ClassPathResource mockResource = mock(ClassPathResource.class);
        when(resourceSupplier.get()).thenReturn(mockResource);
        when(mockResource.exists()).thenThrow(new IOException("Simulated I/O Error"));

        // Quando & Então (When & Then)
        mockMvc.perform(get("/"))
                .andExpect(status().isInternalServerError());
    }

    // ---
    // Testes de métodos HTTP não suportados
    // ---

    @Test
    @DisplayName("Should return 405 Method Not Allowed for unsupported HTTP methods on '/'")
    void shouldReturn405ForUnsupportedMethodsOnHome() throws Exception {
        mockMvc.perform(post("/")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/")).andExpect(status().isMethodNotAllowed());
    }

    // ---
    // Testes para o endpoint "/status"
    // ---

    @Test
    @DisplayName("Should return 200 OK and JSON content for '/status'")
    void shouldReturnOkStatusAndJsonResponseForStatusCheck() throws Exception {
        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @DisplayName("Should return 405 Method Not Allowed for unsupported HTTP methods on '/status'")
    void shouldReturn405ForUnsupportedMethodsOnStatus() throws Exception {
        mockMvc.perform(post("/status")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/status")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/status")).andExpect(status().isMethodNotAllowed());
    }
}
