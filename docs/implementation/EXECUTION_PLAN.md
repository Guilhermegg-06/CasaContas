# Plano de Execução do MVP

Atualizado em 2026-08-26. A marcação só muda após evidência executada.

## Gate 0: fundação e governança

- [x] Confirmar o remoto `Guilhermegg-06/CasaContas` e o estado vazio.
- [x] Ler integralmente o prompt e o documento de engenharia.
- [x] Verificar versões centrais em fontes oficiais.
- [x] Definir convenções, segurança, contribuição e templates.
- [ ] Publicar o commit inicial em `main` e criar `feat/mvp-full-stack`.

Critério: repositório inicial reproduzível, requisitos versionados e sem implementação direta em `main`.

## Gate 1: fundação técnica

- [ ] Criar Spring Boot 4.1.1/Java 21 com wrapper Maven e módulos por funcionalidade.
- [ ] Criar schema PostgreSQL completo via Flyway e teste de migration.
- [ ] Criar erros padronizados, correlation ID, observabilidade e OpenAPI.
- [ ] Criar React 19/Vite 8/TypeScript estrito com testes e cliente HTTP.

Critério: backend e frontend compilam, banco vazio migra e testes arquiteturais passam.

## Gate 2: identidade e casas

- [ ] Registro, login, access JWT, refresh rotativo e logout.
- [ ] Casas multi-tenant, papéis, membros, convites de uso único e remoção lógica.
- [ ] Testes negativos de autenticação, autorização e isolamento.

Critério: fluxos P0 acessíveis apenas a membros ativos da casa correta.

## Gate 3: núcleo financeiro

- [ ] Despesas, divisões iguais/personalizadas e centavos determinísticos.
- [ ] Pagamento principal, liquidação, reembolso, cancelamento e auditoria.
- [ ] Idempotência persistente, locking e testes de concorrência.
- [ ] Red-Green-Refactor, mutação anti-falso-positivo e PIT >= 70%.

Critério: regras RN-001 a RN-018 aplicáveis protegidas por comportamento e banco real.

## Gate 4: consultas e experiência web

- [ ] Painéis mensal/individual, filtros, paginação, histórico e cobrança.
- [ ] Telas P0 em pt-BR, mobile-first, teclado, contraste e estados completos.
- [ ] E2E dos fluxos críticos com Playwright.

Critério: jornadas P0 completas contra a API real.

## Gate 5: operação, CI/CD e entrega

- [ ] Dockerfiles multi-stage, Compose e health checks.
- [ ] Qualidade, cobertura, segurança, SBOM e build de imagens no CI.
- [ ] Publicação imutável no GHCR e promoção manual documentada.
- [ ] Runbooks, ADRs, diagramas, API, rastreabilidade e backlog.

Critério: `make verify`, ambiente completo e imagens passam; documentação permite início do zero.

## Gate 6: GitHub e revisão final

- [ ] Milestone, labels, issues e draft PR vinculados.
- [ ] Revisar diff, segredos, marcadores, skips e artefatos indevidos.
- [ ] Configurar proteção de `main` com nomes reais dos checks.
- [ ] Marcar PR pronta, sem aprovar ou mesclar.

Critério: cada gate classificado com evidência e PR pronta para revisão.

