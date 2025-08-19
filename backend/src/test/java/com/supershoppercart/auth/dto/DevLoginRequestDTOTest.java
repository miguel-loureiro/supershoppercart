package com.supershoppercart.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DevLoginRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        // Inicializa o validador para os testes
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidDevLoginRequest() {
        // Testa um DTO com dados válidos
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        assertEquals(0, validator.validate(request).size(), "O DTO válido não deve ter erros de validação");
    }

    @Test
    public void testInvalidEmail() {
        // Testa um email com formato inválido
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail("invalid-email");
        request.setPassword("password123");

        assertEquals(1, validator.validate(request).size(), "Deve haver 1 erro de validação para um email inválido");
        assertEquals("Email should be valid", validator.validate(request).iterator().next().getMessage());
    }

    @Test
    public void testBlankEmail() {
        // Testa um email vazio
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail("");
        request.setPassword("password123");

        assertEquals(1, validator.validate(request).size(), "Deve haver 1 erro de validação para um email vazio");
        assertEquals("Email cannot be blank", validator.validate(request).iterator().next().getMessage());
    }

    @Test
    public void testBlankPassword() {
        // Testa uma password vazia
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("");

        assertEquals(1, validator.validate(request).size(), "Deve haver 1 erro de validação para uma password vazia");
        assertEquals("Password cannot be blank", validator.validate(request).iterator().next().getMessage());
    }

    @Test
    public void testBlankEmailAndPassword() {
        // Testa ambos os campos vazios
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail("");
        request.setPassword("");

        assertEquals(2, validator.validate(request).size(), "Deve haver 2 erros de validação quando ambos os campos estão vazios");
    }

    @Test
    public void testNullEmail() {
        // Testa um email nulo
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail(null);
        request.setPassword("password123");

        assertEquals(1, validator.validate(request).size(), "Deve haver 1 erro de validação para um email nulo");
        assertEquals("Email cannot be blank", validator.validate(request).iterator().next().getMessage());
    }

    @Test
    public void testNullPassword() {
        // Testa uma password nula
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword(null);

        assertEquals(1, validator.validate(request).size(), "Deve haver 1 erro de validação para uma password nula");
        assertEquals("Password cannot be blank", validator.validate(request).iterator().next().getMessage());
    }

    @Test
    public void testNullEmailAndNullPassword() {
        // Testa ambos os campos nulos
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail(null);
        request.setPassword(null);

        assertEquals(2, validator.validate(request).size(), "Deve haver 2 erros de validação quando ambos os campos são nulos");
    }

    @Test
    public void testWhitespaceEmail() {
        // Testa um email com apenas espaços em branco
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail("   ");
        request.setPassword("password123");

        // O validador irá gerar 2 erros: um para @NotBlank e outro para @Email.
        assertEquals(2, validator.validate(request).size(), "Deve haver 2 erros de validação: um para 'blank' e outro para 'email inválido'");
    }

    @Test
    public void testWhitespacePassword() {
        // Testa uma password com apenas espaços em branco
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("   ");

        assertEquals(1, validator.validate(request).size(), "Deve haver 1 erro de validação para uma password com espaços");
        assertEquals("Password cannot be blank", validator.validate(request).iterator().next().getMessage());
    }

    @Test
    public void testCombinedInvalidFields() {
        // Testa uma combinação de campos inválidos
        DevLoginRequestDTO request = new DevLoginRequestDTO();
        request.setEmail("invalid-email-format");
        request.setPassword("");

        assertEquals(2, validator.validate(request).size(), "Deve haver 2 erros para uma combinação de campos inválidos");
    }
}