# Testes de carga

Este diretório contém uma base de testes de carga com `k6` para a API do backend.

## Pré-requisitos

- Backend executando em `http://localhost:8080`
- Banco PostgreSQL disponível para o backend
- `k6` instalado na máquina

## Perfis disponíveis

- `50`
- `100`
- `1000`
- `find-limit`

## Cenários simulados

- `PublicBrowse`: navegação pública em categorias, profissionais e estabelecimentos
- `AdminRead`: leitura administrativa em `/api/admin/users`

O mix padrão é:

- `80%` tráfego público
- `20%` leitura administrativa

## Exemplos de execução

### Windows PowerShell

```powershell
$env:BASE_URL="http://localhost:8080"
$env:ADMIN_EMAIL="admin@ilhafit.com"
$env:ADMIN_PASSWORD="Adm@1234"
$env:PROFILE="50"
k6 run performance/load-test.js
```

```powershell
$env:PROFILE="find-limit"
k6 run performance/load-test.js
```

## Métricas para documentar

Após cada execução, registre pelo menos:

- duração total
- pico de VUs
- quantidade total de requisições
- requisições por segundo
- taxa de erro
- `p95`
- `p99`
- distribuição de status HTTP
- indicação de aborto ou degradação

## Passo importante antes de rodar

Garanta que o backend tenha sido reiniciado com a versão atual do código antes da execução do `k6`.

Exemplo:

```powershell
./mvnw.cmd package
java -jar target/ilhafit-0.0.1-SNAPSHOT.jar
```

Depois valide rapidamente:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/categorias/categorias
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/profissionais/profissionais
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/estabelecimentos/estabelecimentos
```

## Sugestão de coleta

```powershell
k6 run --summary-export=performance/reports/summary-50.json performance/load-test.js
```

Troque o nome do arquivo para cada perfil executado.
