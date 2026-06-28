# IlhaFit — Backend

API REST da plataforma **IlhaFit**: um diretório fitness onde alunos encontram **estabelecimentos** (academias, studios, espaços esportivos) e **profissionais** (personais, instrutores), com avaliações, denúncias, categorias e autenticação com confirmação de e‑mail (2FA).

Construído com **Spring Boot 3** e **Java 17**.

## Tecnologias

- **Java 17**
- **Spring Boot 3.2** (Web, Data JPA, Security, Validation, Mail)
- **PostgreSQL** + **Hibernate/JPA**
- **Spring Security** + **JWT** (biblioteca `jjwt`) e **BCrypt** para senhas
- **Spring Mail** (envio de e‑mails via SMTP — usado com o Brevo em produção)
- **Bean Validation** (Jakarta Validation)
- **Lombok**
- **Groq API** — moderação automática de conteúdo ofensivo em avaliações/denúncias
- **springdoc-openapi (Swagger UI)** — documentação interativa da API
- **JUnit 5**, **Testcontainers** e **JaCoCo** — testes e cobertura
- **Maven** (com wrapper `mvnw`) e **Docker**

## Pré‑requisitos

- **JDK 17**
- **Maven** (ou use o wrapper `./mvnw` que já acompanha o projeto)
- **PostgreSQL** rodando (local ou em nuvem)
- (Opcional) **Docker** e **Docker Compose**

## Configuração (variáveis de ambiente)

A aplicação lê as variáveis do ambiente ou de um arquivo `.env` na raiz do backend
(o arquivo já é importado automaticamente via `spring.config.import`). Todas têm um
valor padrão para desenvolvimento, exceto as marcadas como obrigatórias em produção.

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://localhost:5432/ilhafit` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `root` |
| `JWT_SECRET` | Chave secreta para assinar o JWT (defina em produção) | (chave de dev) |
| `JWT_EXPIRATION_MILLIS` | Validade do token em milissegundos | `86400000` (24h) |
| `APP_CORS_ALLOWED_ORIGIN_PATTERNS` | Origens liberadas no CORS (separadas por vírgula) | `http://localhost:*,...` |
| `FRONTEND_RESET_PASSWORD_URL` | URL do front para redefinição de senha | `http://localhost:5173/esqueci-senha` |
| `ADMIN_NOME` / `ADMIN_EMAIL` / `ADMIN_SENHA` | Admin padrão criado no primeiro boot | `Admin` / `admin@ilhafit.com` / `Adm@1234` |
| `MAIL_HOST` / `MAIL_PORT` | Servidor SMTP | `smtp.gmail.com` / `587` |
| `MAIL_USER` / `MAIL_PASSWORD` | Credenciais SMTP | — |
| `MAIL_FROM` | Remetente dos e‑mails | = `MAIL_USER` |
| `GROQ_API_KEY` | Chave da Groq para moderação de conteúdo (opcional) | — |

Exemplo de `.env`:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ilhafit
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=root
JWT_SECRET=uma-chave-secreta-bem-longa-e-aleatoria
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=2525
MAIL_USER=seu-login-smtp
MAIL_PASSWORD=sua-chave-smtp
MAIL_FROM=contato@seudominio.com
GROQ_API_KEY=sua-chave-groq
```

## Como rodar

1. Clone o repositório e entre na pasta do backend:
   ```bash
   cd IlhaFit-Backend
   ```
2. Suba um PostgreSQL e ajuste as variáveis de ambiente / `.env`.
3. Execute a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
   No Windows: `mvnw.cmd spring-boot:run`.

A API sobe em **http://localhost:8080**.

No primeiro boot, o sistema cria automaticamente o **administrador padrão**
(`admin@ilhafit.com` / `Adm@1234`) e a **categoria padrão "Outros"**. As tabelas são
criadas/atualizadas automaticamente pelo Hibernate (`ddl-auto=update`).

## Documentação da API (Swagger)

Com a aplicação rodando, acesse:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI (JSON):** http://localhost:8080/v3/api-docs

Para testar endpoints protegidos, faça login, copie o **token JWT** retornado e clique
em **"Authorize"** no topo da página do Swagger.

## Testes

```bash
./mvnw test
```

Os testes de integração usam **Testcontainers** (sobem um PostgreSQL em container —
requer Docker) e a cobertura é gerada pelo **JaCoCo** em `target/site/jacoco/index.html`.

## Docker

```bash
docker build -t ilhafit-backend .
docker run -p 8080:8080 --env-file .env ilhafit-backend
```

Há também um `docker-compose.yml` para subir backend + banco juntos:

```bash
docker compose up
```

## Estrutura do projeto

```
src/main/java/com/example/ilhafit
├── config/         # Segurança, CORS, OpenAPI, async, seed do admin/categoria
├── controller/     # Endpoints REST
├── dto/            # Objetos de requisição/resposta
├── entity/         # Entidades JPA
├── enums/          # Tipos (RegistrationType, Role, status...)
├── mapper/         # Conversão entidade <-> DTO
├── repository/     # Repositórios Spring Data JPA
├── security/       # JWT, filtro de autenticação
├── service/        # Regras de negócio
├── util/           # Utilitários (normalização de texto, etc.)
└── validation/     # Validações customizadas (senha forte, grupos)
```

## Principais funcionalidades

- Cadastro e autenticação de **aluno, profissional, estabelecimento e administrador**
- **2FA**: confirmação de e‑mail por código de 6 dígitos no primeiro login
- **Recuperação de senha** por código enviado por e‑mail
- **Avaliações** (1 a 5 + comentário) e **denúncias**, com moderação de conteúdo
- **Categorias** com fluxo de aprovação pelo administrador
- **Painel administrativo** (usuários, denúncias, categorias)
- Regras de unicidade (e‑mail, CPF, CNPJ, telefone, CREF, razão social por estado)
