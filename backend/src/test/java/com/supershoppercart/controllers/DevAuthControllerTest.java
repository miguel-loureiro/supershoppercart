package com.supershoppercart.controllers;

import com.supershoppercart.auth.dto.DevLoginRequestDTO;
import com.supershoppercart.auth.dto.DevLoginResponseDTO;
import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.services.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class DevAuthControllerTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private ShopperRepository shopperRepository;

    @InjectMocks
    private DevAuthController devAuthController;

    private DevLoginRequestDTO validRequest;
    private Shopper mockShopper;
    private final String mockAccessToken = "mock-dev-jwt-token";

    @BeforeEach
    void setUp() {
        // Inicializa o pedido e o shopper mock
        validRequest = new DevLoginRequestDTO();
        validRequest.setEmail("dev@example.com");
        validRequest.setPassword("password");

        mockShopper = new Shopper("dev@example.com", "Dev User");
        mockShopper.setId("dev-shopper-123");
    }

    @Test
    void devLogin_ExistingUser_ReturnsSuccess() throws Exception {
        // Cenário: Utilizador de desenvolvimento já existe
        when(shopperRepository.findByEmail(validRequest.getEmail()))
                .thenReturn(Optional.of(mockShopper));
        when(jwtTokenService.generateAccessToken(anyString(), anyString()))
                .thenReturn(mockAccessToken);

        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequest);

        // Verificação:
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockAccessToken, response.getBody().getToken());

        // Verificação dos mocks: garantir que os métodos corretos foram chamados
        verify(shopperRepository, times(1)).findByEmail(validRequest.getEmail());
        verify(shopperRepository, never()).save(any(Shopper.class)); // O save não deve ser chamado
        verify(jwtTokenService, times(1)).generateAccessToken(mockShopper.getId(), "dev-device-id");
    }

    @Test
    void devLogin_NewUser_ReturnsSuccess() throws Exception {
        // Cenário: Novo utilizador de desenvolvimento
        when(shopperRepository.findByEmail(validRequest.getEmail()))
                .thenReturn(Optional.empty()); // findByEmail retorna vazio
        when(shopperRepository.save(any(Shopper.class)))
                .thenReturn(mockShopper); // Retorna o shopper salvo
        when(jwtTokenService.generateAccessToken(anyString(), anyString()))
                .thenReturn(mockAccessToken);

        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequest);

        // Verificação:
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockAccessToken, response.getBody().getToken());

        // Verificação dos mocks: garantir que os métodos corretos foram chamados
        verify(shopperRepository, times(1)).findByEmail(validRequest.getEmail());
        verify(shopperRepository, times(1)).save(any(Shopper.class)); // O save deve ser chamado
        verify(jwtTokenService, times(1)).generateAccessToken(mockShopper.getId(), "dev-device-id");
    }

    @Test
    void devLogin_InternalServerError() throws Exception {
        // Cenário: Uma exceção ocorre durante a geração do token
        when(shopperRepository.findByEmail(validRequest.getEmail()))
                .thenReturn(Optional.of(mockShopper));
        when(jwtTokenService.generateAccessToken(anyString(), anyString()))
                .thenThrow(new RuntimeException("Test exception"));

        // Ação: Chamar o método do controlador
        ResponseEntity<DevLoginResponseDTO> response = devAuthController.devLogin(validRequest);

        // Verificação:
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error: Test exception", response.getBody().getToken());

        verify(shopperRepository, times(1)).findByEmail(validRequest.getEmail());
        verify(jwtTokenService, times(1)).generateAccessToken(anyString(), anyString());
    }
}