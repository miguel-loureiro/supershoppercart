package com.supershoppercart.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DevLoginResponseDTO {
    // Getters and setters
    private String accessToken;
    private String refreshToken; // Add refresh token
    private String error;

    // Success constructor
    public DevLoginResponseDTO(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public DevLoginResponseDTO(String error) {
        this.error = error;
    }
}