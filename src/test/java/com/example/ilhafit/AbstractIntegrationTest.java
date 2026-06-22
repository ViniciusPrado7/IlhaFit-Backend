package com.example.ilhafit;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres = startPostgres();

    @SuppressWarnings("resource")
    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> c = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("ilhafit_test")
                .withUsername("test")
                .withPassword("test");
        c.start();
        return c;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    JavaMailSender mailSender;
}
