# ADR 0001 — Monólito modular no MVP

- Status: aceito
- Data: 2026-08-27

## Contexto

O produto precisa de transações consistentes entre despesas, partes, pagamentos e auditoria, mas ainda não possui escala ou equipes que justifiquem serviços distribuídos.

## Decisão

Usar um processo Spring Boot e um banco PostgreSQL, organizando o código por funcionalidade e pelas camadas `api`, `application`, `domain` e `infrastructure`. ArchUnit verifica os limites.

## Consequências

Deploy, transações e depuração ficam simples. Os módulos ainda podem ser extraídos no futuro, porém compartilham ciclo de release e capacidade enquanto o produto for um monólito.
