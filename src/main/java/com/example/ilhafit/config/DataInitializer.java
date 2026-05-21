package com.example.ilhafit.config;

import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.service.CategoriaPendenteService;
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
    private final CategoriaPendenteService categoriaPendenteService;

    @Override
    public void run(ApplicationArguments args) {
        if (administradorRepository.count() > 0) {
            log.info("[DataInitializer] Admin já existe. Nenhuma ação necessária.");
        } else {
            Administrador admin = new Administrador();
            admin.setNome(adminProperties.getNome());
            admin.setEmail(adminProperties.getEmail());
            admin.setSenha(passwordEncoder.encode(adminProperties.getSenha()));
            admin.setRole(Role.ADMIN);
            administradorRepository.save(admin);
            log.info("[DataInitializer] Admin padrão criado: {}", adminProperties.getEmail());
        }

        categoriaPendenteService.limparAtividadesLegadasCriadasAutomaticamente();
        log.info("[DataInitializer] Limpeza de atividades legadas de categorias pendentes concluída.");
    }
}
