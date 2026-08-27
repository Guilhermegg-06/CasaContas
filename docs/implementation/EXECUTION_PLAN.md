# Plano de execução do MVP

Atualizado em 2026-08-27. A marcação reflete evidência executada, não intenção.

## Gate 0 — fundação e governança

- [x] Remoto `Guilhermegg-06/CasaContas` confirmado e commit inicial publicado em `main`.
- [x] Requisitos versionados sem alteração de conteúdo.
- [x] Convenções, segurança, contribuição e templates criados.
- [x] Desenvolvimento separado em branches e commits curtos.

## Gate 1 — fundação técnica

- [x] Spring Boot 4.1.1/Java 21 modular e Maven Wrapper.
- [x] PostgreSQL 17.6, Flyway e Testcontainers; nenhum H2.
- [x] erros padronizados, correlation ID, métricas e OpenAPI.
- [x] React 19/Vite 8/TypeScript estrito e cliente HTTP.

## Gate 2 — identidade e casas

- [x] registro, login, JWT, refresh rotativo e logout.
- [x] casas, papéis, membros, convite de uso único e remoção lógica.
- [x] autorização e isolamento entre casas testados.

## Gate 3 — núcleo financeiro

- [x] despesas e divisões iguais/personalizadas com centavos determinísticos.
- [x] pagamento principal, liquidação, reembolso, cancelamento e auditoria.
- [x] idempotência persistente e transações.
- [x] 18 testes, cobertura 80%, branches de domínio acima de 70% e PIT em 90%.
- [ ] teste específico de concorrência sob carga; classificado no backlog operacional.

## Gate 4 — consultas e experiência web

- [x] painel mensal/individual, filtros, paginação, histórico e cobrança.
- [x] telas P0 em pt-BR, responsivas e com estados de uso.
- [x] E2E Playwright em desktop e celular; API do backend validada separadamente com banco real.

## Gate 5 — operação e CI

- [x] Dockerfiles multi-stage, Compose, proxy e health checks.
- [x] lint, testes, JaCoCo, PIT, SpotBugs, CodeQL e auditoria de dependências.
- [x] relatórios de qualidade preservados como artefatos do PR.
- [x] runbook, ADRs, arquitetura, exemplos de API, ameaças, rastreabilidade e backlog.
- [ ] publicação de imagens no GHCR; exige autorização explícita para publicação externa.

## Gate 6 — GitHub e revisão

- [x] revisão local de diff, segredos, marcadores, skips e arquivos gerados.
- [ ] enviar branches para o remoto público.
- [ ] criar labels, milestone, issues e draft PR.
- [ ] configurar proteção de `main` após os checks existirem no remoto.

Os quatro itens externos aguardam confirmação explícita do proprietário. Nenhuma branch foi mesclada automaticamente.
