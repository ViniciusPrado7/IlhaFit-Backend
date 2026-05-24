package com.example.ilhafit.config;

import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.entity.Categoria;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.CategoriaRepository;
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

    private static final String CATEGORIA_PADRAO = "Outros";

    private final AdministradorRepository administradorRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;
    private final CategoriaPendenteService categoriaPendenteService;

    @Override
    public void run(ApplicationArguments args) {
        if (administradorRepository.count() > 0) {
            administradorRepository.findByEmail(adminProperties.getEmail())
                    .filter(admin -> admin.getEmailConfirmado() == null)
                    .ifPresent(admin -> {
                        admin.setEmailConfirmado(true);
                        administradorRepository.save(admin);
                        log.info("[DataInitializer] Admin padrao marcado como email confirmado: {}", adminProperties.getEmail());
                    });
            log.info("[DataInitializer] Admin ja existe. Nenhuma acao necessaria.");
        } else {
            Administrador admin = new Administrador();
            admin.setNome(adminProperties.getNome());
            admin.setEmail(adminProperties.getEmail());
            admin.setSenha(passwordEncoder.encode(adminProperties.getSenha()));
            admin.setRole(Role.ADMIN);
            admin.setEmailConfirmado(true);
            administradorRepository.save(admin);
            log.info("[DataInitializer] Admin padrao criado: {}", adminProperties.getEmail());
        }

        if (categoriaRepository.existsByNomeIgnoreCase(CATEGORIA_PADRAO)) {
            log.info("[DataInitializer] Categoria padrao ja existe. Nenhuma acao necessaria.");
        } else {
            Categoria categoria = new Categoria();
            categoria.setNome(CATEGORIA_PADRAO);
            categoriaRepository.save(categoria);
            log.info("[DataInitializer] Categoria padrao criada: {}", CATEGORIA_PADRAO);
        }

        categoriaPendenteService.limparAtividadesLegadasCriadasAutomaticamente();
        log.info("[DataInitializer] Limpeza de atividades legadas de categorias pendentes concluida.");
    }
}
