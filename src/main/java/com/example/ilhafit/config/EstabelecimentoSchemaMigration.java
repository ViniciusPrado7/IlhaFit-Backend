package com.example.ilhafit.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EstabelecimentoSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE estabelecimentos ADD COLUMN IF NOT EXISTS razao_social varchar(255)");

        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_name = 'estabelecimentos'
                          AND column_name = 'nome'
                    ) THEN
                        UPDATE estabelecimentos
                        SET nome_fantasia = nome
                        WHERE (nome_fantasia IS NULL OR trim(nome_fantasia) = '')
                          AND nome IS NOT NULL;

                        UPDATE estabelecimentos
                        SET razao_social = nome
                        WHERE (razao_social IS NULL OR trim(razao_social) = '')
                          AND nome IS NOT NULL;

                        ALTER TABLE estabelecimentos DROP COLUMN nome;
                    END IF;
                END $$;
                """);

        jdbcTemplate.execute("""
                UPDATE estabelecimentos
                SET razao_social = COALESCE(NULLIF(trim(razao_social), ''), NULLIF(trim(nome_fantasia), ''), cnpj)
                WHERE razao_social IS NULL OR trim(razao_social) = ''
                """);

        jdbcTemplate.execute("""
                UPDATE estabelecimentos
                SET nome_fantasia = COALESCE(NULLIF(trim(nome_fantasia), ''), NULLIF(trim(razao_social), ''), cnpj)
                WHERE nome_fantasia IS NULL OR trim(nome_fantasia) = ''
                """);

        jdbcTemplate.execute("ALTER TABLE estabelecimentos ALTER COLUMN nome_fantasia SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE estabelecimentos ALTER COLUMN razao_social SET NOT NULL");

      
    }
}
