package com.example.ilhafit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LoginDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        private String email;
        private String senha;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String nome;
        private String email;
        private String role;
    }
}
