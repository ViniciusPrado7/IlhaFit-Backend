package com.example.ilhafit.config;

import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[DataInitializer] email configurado: {}", adminProperties.getEmail());
        log.info("[DataInitializer] total de admins no banco: {}", administradorRepository.count());

        administradorRepository.findByEmail(adminProperties.getEmail()).ifPresentOrElse(
            admin -> {
                if (!passwordEncoder.matches(adminProperties.getSenha(), admin.getSenha())) {
                    log.warn("[DataInitializer] Senha do admin padrão diverge da configuração. Sincronizando...");
                    admin.setSenha(passwordEncoder.encode(adminProperties.getSenha()));
                    administradorRepository.save(admin);
                    log.info("[DataInitializer] Senha sincronizada com sucesso.");
                } else {
                    log.info("[DataInitializer] Admin padrão OK. Nenhuma ação necessária.");
                }
            },
            () -> {
                if (administradorRepository.count() == 0) {
                    Administrador admin = new Administrador();
                    admin.setNome(adminProperties.getNome());
                    admin.setEmail(adminProperties.getEmail());
                    admin.setSenha(passwordEncoder.encode(adminProperties.getSenha()));
                    admin.setRole(Role.ADMIN);
                    administradorRepository.save(admin);
                    log.info("[DataInitializer] Admin padrão criado: {}", adminProperties.getEmail());
                } else {
                    log.warn("[DataInitializer] Existem admins no banco mas nenhum com o email padrão. Nenhum admin criado.");
                }
            }
        );
    }
}
