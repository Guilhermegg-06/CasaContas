# Contratos P0 verificados na estabilização

O schema OpenAPI é gerado em `/api-docs` e a interface em `/docs` no ambiente de
desenvolvimento. Homologação desativa esses endpoints pelo Compose específico.
Os exemplos manuais permanecem em `examples.http`.

- Convite de morador usa `role: "MEMBER"`; gestores usam `ADMIN`. `RESIDENT` não faz parte do contrato.
- Valores são BRL com no máximo duas casas decimais e 17 dígitos inteiros.
  O backend calcula com `BigDecimal`, centavos inteiros sem overflow e armazena `NUMERIC(19,2)`.
- Na divisão igual, os centavos restantes vão aos primeiros participantes na ordem
  recebida. R$ 100,00 / 3 gera 33,34 + 33,33 + 33,33. A ordem não muda durante o cálculo.
- Divisão personalizada exige valor positivo em cada parte e soma exatamente igual
  ao total; valores ausentes ou soma divergente são recusados, sem persistência parcial.
- Criar uma despesa com `paidByMemberId` inclui seu pagamento na mesma transação.
  Pagador inválido ou chave de idempotência ausente não deixa despesa nem auditoria parcial.
- Pagamentos exigem `Idempotency-Key`. Repetir a mesma chave e o mesmo conteúdo,
  inclusive simultaneamente, retorna o mesmo comprovante (200). Reutilizar chave
  com outro conteúdo resulta em conflito (409). Outra chave para cota já liquidada
  é recusada (422), mantendo um único pagamento.
- Pagamento principal exige despesa pendente sem pagamento individual anterior.
  Sem pagador principal, confirmar cota gera `SHARE_PAYMENT`; com pagador principal,
  gera `REIMBURSEMENT`. Cota do pagador principal fica `COVERED`.
- Cancelamento mantém despesa, cotas, pagamentos e eventos, e impede novos pagamentos.
  Edição de valor/participantes após movimento financeiro retorna conflito.
  Cotas anteriores à edição permanecem no banco, inativas, fora dos saldos atuais.
- `PENDING` e `OVERDUE` são calculados com a data no fuso da casa. Lista, detalhe
  e painel usam a mesma referência; status de filtro desconhecido recebe 400.
- Acesso de usuário sem vínculo ativo com a casa recebe 404; nenhuma informação
  da despesa é retornada. Moradores confirmam somente a própria cota/pagamento.

Respostas de erro expõem mensagem útil, código e correlation ID. Tokens, senhas
e corpos de autenticação não devem ser incluídos nos logs ou nas evidências.
Os resultados efetivamente executados ficam em `docs/implementation/STATUS.md`.
