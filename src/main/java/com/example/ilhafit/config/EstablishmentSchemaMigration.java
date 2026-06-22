package com.example.ilhafit.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EstablishmentSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE estabelecimentos ADD COLUMN IF NOT EXISTS razao_social varchar(255)");
        jdbcTemplate.execute("ALTER TABLE estabelecimentos ADD COLUMN IF NOT EXISTS role varchar(50)");

        jdbcTemplate.execute("ALTER TABLE estabelecimentos ALTER COLUMN nome_fantasia SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE estabelecimentos ALTER COLUMN razao_social SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE estabelecimentos ALTER COLUMN role SET NOT NULL");
    }
}

