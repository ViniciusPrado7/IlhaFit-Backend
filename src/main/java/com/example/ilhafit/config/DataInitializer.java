package com.example.ilhafit.config;

import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.CategoryRepository;
import com.example.ilhafit.service.PendingCategoryService;
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

    private final AdministratorRepository administradorRepository;
    private final CategoryRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;
    private final PendingCategoryService categoriaPendenteService;

    @Override
    public void run(ApplicationArguments args) {
        if (administradorRepository.count() > 0) {
            log.info("[DataInitializer] Admin ja existe. Nenhuma acao necessaria.");
        } else {
            Administrator admin = new Administrator();
            admin.setNome(adminProperties.getNome());
            admin.setEmail(adminProperties.getEmail());
            admin.setSenha(passwordEncoder.encode(adminProperties.getSenha()));
            admin.setRole(Role.ADMIN);
            admin.setEmailConfirmado(true);
            administradorRepository.save(admin);
            log.info("[DataInitializer] Admin padrao criado: {}", adminProperties.getEmail());
        }

        if (categoriaRepository.existsByNomeIgnoreCase(CATEGORIA_PADRAO)) {
            log.info("[DataInitializer] Category padrao ja existe. Nenhuma acao necessaria.");
        } else {
            Category categoria = new Category();
            categoria.setNome(CATEGORIA_PADRAO);
            categoriaRepository.save(categoria);
            log.info("[DataInitializer] Category padrao criada: {}", CATEGORIA_PADRAO);
        }

        categoriaPendenteService.limparAtividadesLegadasCriadasAutomaticamente();
        log.info("[DataInitializer] Limpeza de atividades legadas de categorias pendentes concluida.");
    }
}

