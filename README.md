# CasaContas

CasaContas organiza, divide e acompanha despesas de casas compartilhadas com precisão de centavos, isolamento entre casas e histórico auditável.

O MVP v0.1.0 está implementado e validado localmente. A publicação das branches e de imagens permanece separada até autorização explícita para o repositório público `Guilhermegg-06/CasaContas` e para o GHCR.

## O que já funciona

- cadastro, login, JWT curto, refresh rotativo e logout;
- criação de casas, papéis, convites e remoção lógica de moradores;
- despesas iguais ou personalizadas, edição, cancelamento e filtros;
- pagamento principal, liquidação individual, reembolso e idempotência;
- painel mensal/individual, histórico de auditoria e mensagem de cobrança;
- interface responsiva para desktop e celular;
- PostgreSQL real, Flyway, OpenAPI, métricas, Docker e CI.

## Rodar com Docker

Pré-requisito: Docker Desktop com Compose v2.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Abra:

- aplicação: `http://localhost:3000`;
- API: `http://localhost:8080/api/v1`;
- Swagger UI: `http://localhost:8080/docs`;
- OpenAPI JSON: `http://localhost:8080/api-docs`;
- saúde: `http://localhost:8080/actuator/health`.

Os valores padrão são somente para desenvolvimento. Troque `POSTGRES_PASSWORD` e `JWT_SECRET` no `.env` antes de compartilhar qualquer ambiente.

## Verificar

```bash
make verify
make test-e2e
```

Sem `make`:

```powershell
cd backend
mvnw.cmd verify
cd ..\frontend
npm ci
npm run verify
npm run test:e2e
```

O backend exige Java 21 e Docker ativo porque os testes usam PostgreSQL 17.6 via Testcontainers. O frontend usa Node 22.20.0 no CI.

## Estrutura

- `backend/`: monólito modular Spring Boot, domínio e persistência;
- `frontend/`: React, TypeScript, páginas e testes;
- `docs/architecture/`: visão técnica e ADRs;
- `docs/api/examples.http`: chamadas executáveis;
- `docs/operations/`: procedimentos operacionais;
- `docs/security/`: modelo de ameaças;
- `docs/implementation/`: plano, evidências, rastreabilidade e backlog;
- `.github/`: templates, Dependabot e workflows.

Leia primeiro a [arquitetura](docs/architecture/README.md), a [rastreabilidade](docs/implementation/TRACEABILITY.md) e o [runbook](docs/operations/RUNBOOK.md).

## Qualidade comprovada

- 18 testes de backend e 3 regras arquiteturais;
- 80,08% de linhas globais e 74,3% de branches no domínio;
- mutation score de 90% no domínio financeiro;
- SpotBugs sem achados não filtrados;
- 8 testes Vitest e 2 jornadas Playwright;
- imagens Docker construídas e serviços saudáveis;
- workflows aprovados pelo actionlint.

Detalhes e ressalvas estão em [TDD_EVIDENCE.md](docs/implementation/TDD_EVIDENCE.md).

## Escopo

Somente requisitos P0 fazem parte deste MVP. Recorrência, pagamentos parciais, recuperação de senha, notificações automáticas e exportações estão no [backlog](docs/implementation/BACKLOG.md). A licença ainda depende de decisão explícita do proprietário.
