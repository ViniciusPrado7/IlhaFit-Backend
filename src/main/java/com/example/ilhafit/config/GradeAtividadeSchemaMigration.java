package com.example.ilhafit.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GradeAtividadeSchemaMigration implements ApplicationRunner {

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

        Boolean migracaoPendente = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'grade_atividades'
                      AND column_name = 'atividade_normalizada'
                )
                """, Boolean.class);

        if (Boolean.TRUE.equals(migracaoPendente)) {
            log.warn("[GradeAtividadeSchemaMigration] *** RECOMENDA-SE BACKUP DO BANCO ANTES DESTA MIGRATION ***");
            executarMigracaoDados();
        }

        jdbcTemplate.execute("DROP INDEX IF EXISTS uq_grade_atividades_profissional_categoria");
        jdbcTemplate.execute("DROP INDEX IF EXISTS uq_grade_atividades_estabelecimento_categoria");
        criarIndiceUnicoFkSeNaoHouverDuplicidade("profissional_id",   "uq_grade_prof_cat");
        criarIndiceUnicoFkSeNaoHouverDuplicidade("estabelecimento_id", "uq_grade_estab_cat");
    }

    private void executarMigracaoDados() {
        jdbcTemplate.execute("""
                UPDATE grade_atividades ga
                SET categoria_id = (
                    SELECT c.id
                    FROM categorias c
                    WHERE LOWER(TRIM(c.nome)) = ga.atividade_normalizada
                    LIMIT 1
                )
                WHERE ga.categoria_id IS NULL
                  AND ga.atividade_normalizada IS NOT NULL
                """);

        List<Map<String, Object>> orfas = jdbcTemplate.queryForList("""
                SELECT id, atividade, profissional_id, estabelecimento_id
                FROM grade_atividades
                WHERE categoria_id IS NULL
                """);
        if (!orfas.isEmpty()) {
            log.warn("[GradeAtividadeSchemaMigration] {} grade(s) sem categoria correspondente serao removidas:", orfas.size());
            orfas.forEach(row -> log.warn("  -> id={} atividade='{}' profissional_id={} estabelecimento_id={}",
                    row.get("id"), row.get("atividade"), row.get("profissional_id"), row.get("estabelecimento_id")));
        }

        jdbcTemplate.execute("""
                DO $$
                DECLARE duplicata RECORD;
                BEGIN
                    FOR duplicata IN
                        SELECT MIN(id) AS manter_id, profissional_id, estabelecimento_id, categoria_id
                        FROM grade_atividades
                        WHERE categoria_id IS NOT NULL
                        GROUP BY profissional_id, estabelecimento_id, categoria_id
                        HAVING COUNT(*) > 1
                    LOOP
                        DELETE FROM grade_atividade_dias
                        WHERE grade_id IN (
                            SELECT id FROM grade_atividades
                            WHERE categoria_id = duplicata.categoria_id
                              AND (profissional_id IS NOT DISTINCT FROM duplicata.profissional_id)
                              AND (estabelecimento_id IS NOT DISTINCT FROM duplicata.estabelecimento_id)
                              AND id <> duplicata.manter_id
                        );
                        DELETE FROM grade_atividade_periodos
                        WHERE grade_id IN (
                            SELECT id FROM grade_atividades
                            WHERE categoria_id = duplicata.categoria_id
                              AND (profissional_id IS NOT DISTINCT FROM duplicata.profissional_id)
                              AND (estabelecimento_id IS NOT DISTINCT FROM duplicata.estabelecimento_id)
                              AND id <> duplicata.manter_id
                        );
                        DELETE FROM grade_atividades
                        WHERE categoria_id = duplicata.categoria_id
                          AND (profissional_id IS NOT DISTINCT FROM duplicata.profissional_id)
                          AND (estabelecimento_id IS NOT DISTINCT FROM duplicata.estabelecimento_id)
                          AND id <> duplicata.manter_id;
                    END LOOP;
                END $$;
                """);

        jdbcTemplate.execute("""
                DELETE FROM grade_atividade_dias
                WHERE grade_id IN (SELECT id FROM grade_atividades WHERE categoria_id IS NULL)
                """);
        jdbcTemplate.execute("""
                DELETE FROM grade_atividade_periodos
                WHERE grade_id IN (SELECT id FROM grade_atividades WHERE categoria_id IS NULL)
                """);
        jdbcTemplate.execute("DELETE FROM grade_atividades WHERE categoria_id IS NULL");

        jdbcTemplate.execute("ALTER TABLE grade_atividades ALTER COLUMN categoria_id SET NOT NULL");
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM information_schema.table_constraints
                        WHERE constraint_name = 'fk_grade_categoria'
                          AND table_name = 'grade_atividades'
                    ) THEN
                        ALTER TABLE grade_atividades
                        ADD CONSTRAINT fk_grade_categoria
                        FOREIGN KEY (categoria_id) REFERENCES categorias(id);
                    END IF;
                END $$;
                """);

        jdbcTemplate.execute("ALTER TABLE grade_atividades DROP COLUMN IF EXISTS atividade");
        jdbcTemplate.execute("ALTER TABLE grade_atividades DROP COLUMN IF EXISTS atividade_normalizada");

        log.info("[GradeAtividadeSchemaMigration] Migration de dados concluida com sucesso.");
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
