package com.example.ilhafit.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityScheduleSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE categorias ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL");
        jdbcTemplate.execute("ALTER TABLE grade_atividades ADD COLUMN IF NOT EXISTS categoria_id BIGINT NULL");

        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uq_categorias_nome_ativo
                ON categorias (LOWER(nome))
                WHERE deleted_at IS NULL
                """);

        jdbcTemplate.execute("DROP INDEX IF EXISTS uq_grade_atividades_profissional_categoria");
        jdbcTemplate.execute("DROP INDEX IF EXISTS uq_grade_atividades_estabelecimento_categoria");
        criarIndiceUnicoFkSeNaoHouverDuplicidade("profissional_id",   "uq_grade_prof_cat");
        criarIndiceUnicoFkSeNaoHouverDuplicidade("estabelecimento_id", "uq_grade_estab_cat");
    }

    private void criarIndiceUnicoFkSeNaoHouverDuplicidade(String colunaDono, String nomeIndice) {
        Boolean colunaExiste = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'grade_atividades'
                      AND column_name = 'categoria_id'
                )
                """, Boolean.class);

        if (!Boolean.TRUE.equals(colunaExiste)) {
            return;
        }

        Integer duplicidades = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM (
                    SELECT %s, categoria_id
                    FROM grade_atividades
                    WHERE %s IS NOT NULL
                      AND categoria_id IS NOT NULL
                    GROUP BY %s, categoria_id
                    HAVING COUNT(*) > 1
                ) duplicadas
                """.formatted(colunaDono, colunaDono, colunaDono), Integer.class);

        if (duplicidades != null && duplicidades > 0) {
            log.warn("[GradeAtividadeSchemaMigration] Indice {} nao criado: ainda existem duplicatas para {}.", nomeIndice, colunaDono);
            return;
        }

        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS %s
                ON grade_atividades (%s, categoria_id)
                WHERE %s IS NOT NULL
                """.formatted(nomeIndice, colunaDono, colunaDono));
    }
}
