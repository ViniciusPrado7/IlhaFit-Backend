package com.example.ilhafit.config;

import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.CategoryRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.PendingCategoryRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Correcao unica dos dados ja cadastrados: reaplica a normalizacao de nomes
 * proprios (Title Case) nos registros existentes que foram salvos em caixa
 * baixa antes da mudanca no {@link com.example.ilhafit.util.StringNormalizer}.
 *
 * <p>So roda quando {@code app.recapitalize=true}. Como o Title Case e
 * idempotente, reexecutar nao causa dano, mas o ideal e rodar uma vez e depois
 * remover a propriedade. Cada {@code save} dispara o {@code @PreUpdate} da
 * entidade, que ja aplica a normalizacao correta.
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.recapitalize", havingValue = "true")
public class DataRecapitalizeRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final AdministratorRepository administratorRepository;
    private final EstablishmentRepository establishmentRepository;
    private final CategoryRepository categoryRepository;
    private final PendingCategoryRepository pendingCategoryRepository;
    private final EvaluationRepository evaluationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[recapitalize] Iniciando recapitalizacao dos dados existentes...");

        int usuarios = recapitalizar("usuarios", userRepository);
        int profissionais = recapitalizar("profissionais", professionalRepository);
        int administradores = recapitalizar("administradores", administratorRepository);
        int estabelecimentos = recapitalizar("estabelecimentos", establishmentRepository);
        int categorias = recapitalizar("categorias", categoryRepository);
        int categoriasPendentes = recapitalizar("categorias pendentes", pendingCategoryRepository);
        int avaliacoes = recapitalizar("avaliacoes", evaluationRepository);

        log.info("[recapitalize] Concluido. Registros processados -> usuarios={}, profissionais={}, "
                        + "administradores={}, estabelecimentos={}, categorias={}, categoriasPendentes={}, avaliacoes={}",
                usuarios, profissionais, administradores, estabelecimentos, categorias, categoriasPendentes, avaliacoes);
    }

    private int recapitalizar(String rotulo, org.springframework.data.jpa.repository.JpaRepository<?, ?> repositorio) {
        var registros = repositorio.findAll();
        // saveAll dispara o @PreUpdate de cada entidade, que reaplica a normalizacao.
        salvarTodos(repositorio, registros);
        log.info("[recapitalize] {} registros de {} normalizados.", registros.size(), rotulo);
        return registros.size();
    }

    @SuppressWarnings("unchecked")
    private <T> void salvarTodos(org.springframework.data.jpa.repository.JpaRepository<?, ?> repositorio, java.util.List<?> registros) {
        ((org.springframework.data.jpa.repository.JpaRepository<T, ?>) repositorio).saveAll((java.util.List<T>) registros);
    }
}
