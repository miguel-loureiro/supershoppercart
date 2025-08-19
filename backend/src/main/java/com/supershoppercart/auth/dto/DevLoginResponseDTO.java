package com.supershoppercart.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DevLoginResponseDTO {
    private String token;

    public DevLoginResponseDTO(String accessToken) {
        this.token = accessToken;
    }
}
