# AGENTS.md

## Estrutura

- Backend em `backend/`, organizado por funcionalidade e camadas `api`, `application`, `domain`, `infrastructure`.
- Frontend em `frontend/`, com páginas, componentes, serviços e testes próximos ao código.
- Documentação viva em `docs/`; decisões estruturais exigem ADR.

## Comandos

- Verificação completa: `make verify`.
- Ambiente local: `docker compose up --build`.
- Backend: `cd backend && ./mvnw verify` (Windows: `mvnw.cmd verify`).
- Frontend: `cd frontend && npm ci && npm run verify`.

## Convenções e restrições

- Java 21, TypeScript estrito, PostgreSQL e migrations Flyway; não usar H2.
- Dinheiro somente com `BigDecimal`/`NUMERIC(19,2)` e moeda BRL no MVP.
- Instantes em UTC; apresentação no fuso da casa.
- Nunca versionar segredos, tokens ou credenciais reais.
- Nunca excluir fisicamente dados financeiros; manter auditoria e idempotência.
- Controllers não contêm regra de negócio e entidades JPA não saem pela API.
- Implementar apenas P0; P1/P2 permanecem no backlog.
- Não adicionar `TODO`, `FIXME`, testes ignorados ou mocks de produção.

## Definição de pronto

Código formatado, análises e testes verdes; migration e autorização validadas; OpenAPI e documentação atualizados; nenhuma credencial ou dado sensível em logs; comportamento P0 rastreado a requisito.

