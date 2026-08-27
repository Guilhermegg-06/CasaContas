# ADR 0002 — Sessões curtas, idempotência e auditoria persistente

- Status: aceito
- Data: 2026-08-27

## Contexto

O sistema manipula confirmações financeiras que não podem duplicar, e tokens roubados devem ter janela curta de uso.

## Decisão

Emitir JWT de acesso por 10 minutos e refresh token opaco por 30 dias, rotacionado a cada uso e armazenado somente por hash. Operações financeiras exigem `Idempotency-Key`, persistida junto ao resultado. Mudanças relevantes geram evento de auditoria.

## Consequências

O backend permanece stateless para acesso comum, mas refresh e idempotência dependem do PostgreSQL. Repetições seguras devolvem o mesmo resultado; reutilização de refresh expirado ou revogado é recusada.
