package com.example.ilhafit.config;

import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (administradorRepository.count() > 0) {
            return;
        }

        Administrador admin = new Administrador();
        admin.setNome("Admin");
        admin.setEmail("admin@ilhafit.com");
        admin.setSenha(passwordEncoder.encode("Adm@1234"));
        admin.setRole(Role.ADMIN);

        administradorRepository.save(admin);
    }
}
