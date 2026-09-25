# Rastreabilidade do MVP P0

| Requisitos | Implementação principal | Interface | Evidência automatizada |
|---|---|---|---|
| RF-001 a RF-003 | `identity`, JWT, refresh rotativo e autorização por casa | cadastro, login e sessão | `CriticalFlowsIntegrationTest`, `RefreshSessionTest`, `AuthPage.test.tsx` |
| RF-010 a RF-015 | `household`, papéis, convite de uso único e remoção lógica | criação/entrada de casa e moradores | `InvitationTest`, fluxo crítico e E2E |
| RF-020 a RF-029 P0 | `expense`, criação, edição, cancelamento e status calculado | lista, filtros, formulário e detalhe | `ExpenseTest`, `SplitCalculatorTest`, fluxo crítico e E2E |
| RF-030 a RF-035 | partes e `settlement`, pagamento principal, reembolso e idempotência | confirmações e histórico financeiro | `SingleResidentExpenseTest`, fluxo crítico e PIT |
| RF-040 a RF-044 | `reporting`, filtros paginados, auditoria e `notification` | painel, histórico e copiar cobrança | fluxo crítico, testes de formatação e E2E |

## Regras financeiras

- RN-001 a RN-006: `Expense`, `ExpenseShare` e `SplitCalculator`.
- RN-007: `HouseholdAccessService` e filtro obrigatório por `householdId`.
- RN-008 a RN-013: `SettlementService`, idempotência e auditoria.
- RN-014 a RN-016: membros lógicos e identidade única no schema.
- RN-017 e RN-018: UTC, fuso IANA da casa e BRL fixo.

Os itens P1/P2 não foram misturados ao MVP e permanecem no [`BACKLOG.md`](BACKLOG.md).

## Estabilização

| Regras | Regressões e evidências |
|---|---|
| RN-008 a RN-013, RF-027 | `FinancialIntegrityTest`, `FinancialIntegrationTest`: concorrência real, atomicidade, duplicação, cancelamento e cotas preservadas |
| RN-017 | `HouseholdDateIntegrationTest` e `format.test.ts`: fuso consistente em detalhe, filtros, painel e histórico |
| RF-001 a RF-003 | `api.test.ts`, `IntegrationStates.test.tsx`: refresh compartilhado, logout durante refresh e cache separado por identidade |
| RF-010 a RF-015 | `IntegrationStates.test.tsx`: contrato MEMBER e estados de erro da casa/moradores |
| Persistência incremental | `MigrationUpgradeTest`: banco V1 com dados sintéticos atualizado para V2, sem perda de linhas ou auditoria |
| RF-001 a RF-044 P0, RN-007 a RN-013 | `system.spec.ts`: jornada real, repetição sem duplicação, outra casa recusada e reinício preservando volume |
| Operação de homologação | `test-homolog.mjs`, `DisabledDocumentationIntegrationTest` e `database-backup.mjs`: configuração, rotas desativadas e restauração isolada |

Os Playwright originais (`critical-flow.spec.ts`) simulam a API. Testes Spring usam
PostgreSQL real. A jornada `system.spec.ts` usa navegador, API e PostgreSQL reais;
seu resultado não pode ser inferido pelos outros testes. Consulte `STATUS.md`
para resultados da revisão atual e eventuais bloqueios.
