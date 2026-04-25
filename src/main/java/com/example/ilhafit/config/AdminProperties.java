package com.example.ilhafit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "admin.default")
public class AdminProperties {

    private String nome = "Admin";
    private String email = "admin@ilhafit.com";
    private String senha = "Adm@1234";
}
