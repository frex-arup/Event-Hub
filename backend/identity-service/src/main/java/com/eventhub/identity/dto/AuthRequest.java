package com.eventhub.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequest {

    @Data
    public static class Login {
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 6)
        private String password;
    }

    @Data
    public static class Signup {
        @NotBlank @Size(min = 2)
        private String name;
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 8)
        private String password;
        private String role; // USER or ORGANIZER
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;
    }
}
