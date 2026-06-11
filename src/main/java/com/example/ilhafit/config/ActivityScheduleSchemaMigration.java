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
        adaptarColunaCategoriaLegada();
        jdbcTemplate.execute("ALTER TABLE grade_atividades ADD COLUMN IF NOT EXISTS atividade_normalizada varchar(255)");
        jdbcTemplate.execute("""
                UPDATE grade_atividades
                SET atividade_normalizada = lower(regexp_replace(btrim(atividade), '\\s+', ' ', 'g'))
                WHERE atividade IS NOT NULL
                  AND (atividade_normalizada IS NULL OR atividade_normalizada = '')
                """);

        criarIndiceUnicoSeNaoHouverDuplicidade(
                "profissional_id",
                "uq_grade_atividades_profissional_categoria"
        );
        criarIndiceUnicoSeNaoHouverDuplicidade(
                "estabelecimento_id",
                "uq_grade_atividades_estabelecimento_categoria"
        );
    }

    private void adaptarColunaCategoriaLegada() {
        Boolean categoriaIdExiste = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_name = 'grade_atividades'
                      AND column_name = 'categoria_id'
                )
                """, Boolean.class);

        if (!Boolean.TRUE.equals(categoriaIdExiste)) {
            return;
        }

        jdbcTemplate.execute("""
                UPDATE grade_atividades ga
                SET atividade = c.nome
                FROM categorias c
                WHERE ga.categoria_id = c.id
                  AND (ga.atividade IS NULL OR btrim(ga.atividade) = '')
                """);

        jdbcTemplate.execute("ALTER TABLE grade_atividades ALTER COLUMN categoria_id DROP NOT NULL");
        jdbcTemplate.execute("ALTER TABLE grade_atividades DROP CONSTRAINT IF EXISTS ck_grade_atividades_responsavel");
    }

    private void criarIndiceUnicoSeNaoHouverDuplicidade(String colunaDono, String nomeIndice) {
        Integer duplicidades = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM (
                    SELECT %s, atividade_normalizada
                    FROM grade_atividades
                    WHERE %s IS NOT NULL
                      AND atividade_normalizada IS NOT NULL
                    GROUP BY %s, atividade_normalizada
                    HAVING COUNT(*) > 1
                ) duplicadas
                """.formatted(colunaDono, colunaDono, colunaDono), Integer.class);

        if (duplicidades != null && duplicidades > 0) {
            log.warn("[ActivityScheduleSchemaMigration] Indice {} nao foi criado porque a base ja possui categorias duplicadas para {}.", nomeIndice, colunaDono);
            return;
        }

        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS %s
                ON grade_atividades (%s, atividade_normalizada)
                WHERE %s IS NOT NULL
                """.formatted(nomeIndice, colunaDono, colunaDono));
    }
}

