package com.supershoppercart.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DevLoginRequestDTO {
    // Getters and setters
    @Email
    @NotBlank
    private String email;

    private String deviceId; // Optional for dev

    public DevLoginRequestDTO() {}

    public DevLoginRequestDTO(String email, String deviceId) {
        this.email = email;
        this.deviceId = deviceId;
    }
}
