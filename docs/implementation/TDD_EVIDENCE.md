# Evidências de qualidade e TDD

Atualizado em 2026-08-28.

## Ciclos observados

- Red: os primeiros testes do núcleo financeiro foram escritos antes da implementação e falharam por comportamento/compilação ausente.
- Green: as regras de divisão, liquidação, convite, refresh e isolamento passaram contra PostgreSQL real.
- Refactor: a consulta mensal, o caso de morador único, a imutabilidade de DTOs e o tratamento de JSON malformado foram corrigidos mantendo a suíte verde.
- Validação dos testes: o PIT alterou o bytecode do domínio e confirmou que as asserções matam 46 de 51 mutantes.

## Resultado local

| Verificação | Resultado |
|---|---|
| Backend | 18 testes, 0 falhas, 0 ignorados |
| Arquitetura | 3 regras ArchUnit verdes |
| Banco | PostgreSQL 17.6 via Testcontainers e Flyway V1 |
| Cobertura | 852/1064 linhas globais, 80,08% |
| Ramos do domínio | 52/70, 74,3% |
| Mutação do domínio | 46/51 mortos, 90%; força 98% |
| Análise estática | SpotBugs com 0 avisos e 0 erros não filtrados |
| Frontend | 4 arquivos, 8 testes Vitest |
| E2E | 2 jornadas Playwright: desktop e celular |
| Dependências JS | `npm audit`: 0 vulnerabilidades |
| Contêineres | imagens backend/frontend construídas; 3 serviços saudáveis |
| GHCR | backend b9689ee7 e frontend c4fbd219 publicados com tags de branch e commit |
| Workflows | 4 arquivos aprovados pelo actionlint; primeira publicação concluída |

O relatório global de branches é 47,28%. O gate de 70% é aplicado ao domínio, onde decisões de negócio se concentram; ampliar branches de aplicação e adaptadores permanece melhoria de qualidade sem reduzir a cobertura global de linhas de 80%.
