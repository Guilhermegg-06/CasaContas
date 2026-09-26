# ADR 0004 — Transações financeiras e histórico das cotas

- Data: 2026-09-25
- Estado: aceita para implementação; validação registrada em `docs/implementation/STATUS.md`
- Requisitos: RN-009, RN-012, RN-013; RF-027; CA-005.

## Problemas reproduzidos

A API criava a despesa e registrava o pagamento inicial em transações distintas.
Um pagador inválido deixava a despesa gravada apesar da resposta de erro.
A edição substituía as cotas com exclusão física. Confirmações simultâneas com a
mesma chave consultavam a idempotência antes do bloqueio da despesa e retornavam
200/422 para a mesma operação, em vez de devolver o mesmo comprovante.

## Decisão

O método de criação com pagador de `ExpenseService` coordena criação e pagamento
numa única transação, usando os serviços de aplicação existentes.
O controller apenas converte o contrato HTTP e delega ao serviço de aplicação.
Pagamentos adquirem o bloqueio da despesa antes de consultar a idempotência.
Edição e cancelamento devem usar o mesmo bloqueio antes de ler o estado financeiro.
Assim, cada operação observa o resultado já confirmado da operação anterior.

A migration V2 acrescenta `active` às cotas e um índice único somente para as
cotas ativas. Cotas substituídas são preservadas como inativas; consultas, filtros
e painéis consideram apenas as ativas. Índices únicos também impedem dois
pagamentos principais por despesa ou duas liquidações da mesma cota.
V1 permanece intacta. As restrições falham se um banco antigo já contiver
duplicações: investigar os dados; nunca apagá-los para forçar a atualização.

## Consequências e retorno de versão

O histórico físico das cotas permanece consultável no banco e os eventos de edição
continuam na auditoria. Não se acrescenta estorno ou pagamento parcial ao P0.
Depois de editar uma despesa com V2, a versão original não pode ser restaurada
como aplicação: ela não filtra cotas inativas. O retorno exige imagem compatível
com V2 ou restauração isolada do backup anterior, com análise dos dados posteriores.
Não executar downgrade ou limpeza automática do banco.

## Evidência

`FinancialIntegrationTest` usa PostgreSQL real e bloqueios observados no banco para
coordenar concorrência. `MigrationUpgradeTest` aplica V1, insere dados sintéticos,
atualiza para V2 e compara valores, auditoria e restrições de unicidade.
