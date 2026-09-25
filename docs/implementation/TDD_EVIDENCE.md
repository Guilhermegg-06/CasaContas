# Evidências de qualidade e regressões

Atualizado em 2026-09-25. O estado e a revisão final estão em [STATUS.md](STATUS.md).
Resultados históricos do MVP não aprovam alterações posteriores.

## Reproduções desta estabilização

Os testes foram executados antes da correção. Logs completos locais ficam em
`.data/evidence/` (ignorado pelo Git); os relatórios da revisão ficam no CI do PR.

| Defeito reproduzido | Resultado anterior | Regressão |
|---|---|---|
| Duas respostas 401 renovavam a mesma sessão | Duas renovações concorrentes | `api.test.ts`: uma renovação compartilhada |
| Refresh atrasado após logout | Sessão reaparecia | `api.test.ts`: resposta antiga descartada |
| Falhas da API e detalhe editado | Erro oculto/cache antigo | `IntegrationStates.test.tsx`: mensagem, retry e dado atualizado |
| Despesa com pagador inválido | Despesa persistia apesar do erro | `FinancialIntegrationTest`: criação e pagamento atômicos |
| Confirmações simultâneas | Mesmo identificador retornava 200/422 | `FinancialIntegrationTest`: mesmo registro para repetição; chaves distintas sem duplicação |
| Edição/cancelamento simultâneo ao pagamento | 500 por estado desatualizado | `FinancialIntegrationTest`: edição recusada e cancelamento preserva pagamento |
| Edição antes de pagamento | Cotas antigas eram excluídas | `FinancialIntegrationTest`: cotas arquivadas, auditoria preservada |
| Parte personalizada nula | 500 | `FinancialIntegrationTest`: 422 sem persistência |
| Valor válido na faixa NUMERIC(19,2) | Overflow de `long` | `FinancialIntegrityTest`: centavos com `BigInteger` |
| Data UTC diferente da casa | Pendente aparecia vencida | `HouseholdDateIntegrationTest`: detalhe, filtro e painel coerentes |
| Swagger após atualização do WebJar | 500 | `DocumentationAccessIntegrationTest`: recursos resolvidos com a mesma versão |
| Documentação desativada na homologação | 500 em `/docs` | `DisabledDocumentationIntegrationTest`: 404 padronizado; Swagger habilitado segue funcionando |

Reproduções financeiras usam PostgreSQL e bloqueios reais. Erros iniciais de
fixture (cache das estatísticas do PostgreSQL), falta de binding nativo no
verificador Alpine e contenção local foram diagnosticados separadamente; não
foram contados como defeitos funcionais nem justificaram redução dos gates.
Na jornada real, o trace mostrou renovação concorrente ao logout ainda em curso;
o teste passou a aguardar o 204 do logout antes de exigir 401 na renovação.

## Validação executada

- Revisão funcional `fc29949`: [CI completo](https://github.com/Guilhermegg-06/CasaContas/actions/runs/36180856299)
  e [CodeQL](https://github.com/Guilhermegg-06/CasaContas/actions/runs/36180856029) aprovados.
- Backend no CI: `verify` com 32 testes, sem falhas/erros/ignorados; JaCoCo e
  SpotBugs aprovados. PIT detectou 47/52 mutações (90%), com 86% de cobertura
  nas classes mutadas. A suíte local anterior tinha 31 testes; a regressão de
  documentação foi acrescentada e passou com o teste de Swagger habilitado.
- Frontend local: lint, Prettier, 19 testes em seis arquivos e build aprovados
  (`frontend-verify.log`). CI também executou auditoria npm e NVD obrigatórias.
- `MigrationUpgradeTest`: banco com V1 e dados sintéticos atualizado para V2,
  preservando valores e auditoria, com reaplicação sem novas migrations.
- Jornada real local em `system-resumed.log`: uma jornada aprovada, sem
  interceptações, cobrindo três moradores, R$ 100,00 em 33,34/33,33/33,33,
  personalizada inválida/válida, edição, pagamento, reembolso, idempotência,
  refresh/logout/login, cancelamento, filtros, isolamento e reinício com volume.
- Backup dessa jornada restaurado em PostgreSQL isolado: contagens e resumos
  de todas as linhas das 11 tabelas coincidiram, inclusive histórico e valores.
- CI `36180856299`: jornada real (35,4 s), backup/restauração e configuração de
  homologação aprovados: segredo obrigatório, saúde, cadastro, CORS, rotas
  desativadas, migrations e ausência de portas públicas de API/banco.
- A consolidação posterior sincroniza a asserção de logout com a resposta real.
  Os checks de cada nova revisão devem passar novamente no PR.

## O que cada teste comprova

`critical-flow.spec.ts` valida a interface responsiva com API simulada. Os testes
Spring/Testcontainers validam API, autorização, transações e banco reais.
`system.spec.ts` une navegador, backend e PostgreSQL reais. Reiniciar serviços e
restaurar um dump são verificações adicionais; nenhuma delas comprova TLS,
durabilidade do disco ou monitoramento de um provedor ainda não escolhido.
