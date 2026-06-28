package com.example.ilhafit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da documentação da API (Swagger / OpenAPI).
 *
 * Interface: http://localhost:8080/swagger-ui.html
 * Especificação JSON: http://localhost:8080/v3/api-docs
 *
 * O esquema de segurança "bearerAuth" habilita o botão "Authorize" na interface,
 * onde basta colar o token JWT obtido no login para testar os endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ilhaFitOpenAPI() {
        final String esquemaSeguranca = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("IlhaFit API")
                        .description("API da plataforma IlhaFit: cadastro e busca de estabelecimentos e "
                                + "profissionais fitness, avaliações, denúncias, categorias e autenticação com 2FA.")
                        .version("1.0.0")
                        .contact(new Contact().name("Equipe IlhaFit").email("ilhafit3@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(esquemaSeguranca))
                .components(new Components().addSecuritySchemes(esquemaSeguranca,
                        new SecurityScheme()
                                .name(esquemaSeguranca)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Informe o token JWT obtido no login (sem o prefixo 'Bearer').")));
    }
}
