package com.supershoppercart.auth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DevLoginResponseDTOTest {

    @Test
    public void testConstructorAndGetter() {
        // Testa se o token é armazenado corretamente pelo construtor
        String expectedToken = "test_access_token_12345";
        DevLoginResponseDTO response = new DevLoginResponseDTO(expectedToken);

        assertNotNull(response, "O objeto DevLoginResponseDTO não deve ser nulo");
        assertEquals(expectedToken, response.getToken(), "O token recuperado deve ser o mesmo que o token definido no construtor");
    }

    @Test
    public void testSetter() {
        // Testa se o setter atualiza corretamente o valor do token
        String initialToken = "initial_token";
        String newToken = "new_and_updated_token";

        DevLoginResponseDTO response = new DevLoginResponseDTO(initialToken);
        response.setToken(newToken);

        assertEquals(newToken, response.getToken(), "O token deve ser atualizado para o novo valor após o uso do setter");
    }
}