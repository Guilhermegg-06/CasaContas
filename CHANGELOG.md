# Changelog

Todas as mudanças relevantes seguem Keep a Changelog e Versionamento Semântico.

## [Unreleased]

### Added

- MVP P0 completo de identidade, casas, despesas, rateios, pagamentos, painéis e cobranças.
- Interface React responsiva com jornadas desktop e celular.
- PostgreSQL/Flyway, Docker Compose, health checks, OpenAPI e métricas.
- CI de PR, CodeQL, auditoria de dependências, JaCoCo, PIT e SpotBugs.
- Arquitetura, ADRs, exemplos HTTP, runbook, modelo de ameaças e rastreabilidade.

### Changed

- Testes Spring compartilham um único contexto e banco efêmero.
- Vitest e Playwright possuem descoberta de arquivos separada.
- DTOs de coleção usam cópias defensivas.

### Fixed

- JSON malformado retorna erro 400 padronizado.
- `CurrentUser` trata autenticação e claim `sub` ausentes.
- Consulta mensal evita soma duplicada por joins.
- Despesa de morador único liquida corretamente o próprio pagamento.

### Security

- JWT curto, refresh opaco rotativo, BCrypt 12, autorização por casa e idempotência persistente.
- Logs estruturados com correlation ID e sem credenciais.
