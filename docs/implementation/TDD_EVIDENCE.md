# Evidências de qualidade e regressões

Atualizado em 2026-10-14. O estado e a revisão final estão em [STATUS.md](STATUS.md).
Resultados históricos do MVP não aprovam alterações posteriores.

## Contribuições — auditorias, convites e apresentação

Os testes de `InvitationLifecycleIntegrationTest` acrescentam evidência de RF-012
pela API e PostgreSQL real: apenas um aceite concorrente cria vínculo, o vencedor
fica persistido e a repetição não altera moradores. Um convite expirado é recusado
sem consumo ou vínculo novo. Os dois cenários passaram na implementação existente;
são cobertura adicional, sem alegar descoberta de um defeito ou alterar código de produção.

- Frontend: `npm audit` reproduziu quatro alertas altos com saída 1. Após correções
  transitivas direcionadas, instalação limpa/auditoria em Node 22.20.0 passaram;
  lint, formatação, 21 testes, build e duas jornadas com API simulada passaram.
- Workflow Java: a fixture executa o comando extraído do YAML. Dois testes falharam
  porque a falha NVD interrompia a execução na primeira tentativa. O auditor novo
  passou cinco cenários, exigindo o perfil/goal de auditoria e mantendo o bloqueio
  por vulnerabilidade, inclusive quando há também um erro NVD.
- Auditoria real Java 21: Dependency-Check 13.0.0, fonte oficial NVD, CVSS 7,
  zero achados em 78 dependências, saída 0 após 29 minutos. Swagger UI 5.33.2
  incorpora DOMPurify 3.4.16; 34 testes, cobertura, SpotBugs e Actionlint passaram.
- Evidências completas: `.data/evidence/frontend-*`, `backend-audit-retry-*`,
  `backend-swagger-verify.log`, `backend-audit-real.log` e `backend-audit-result/`.
- Jornada real: o cadastro falhou na expectativa de navegação enquanto o POST ainda
  estava pendente; a sincronização passou a exigir resposta 201 antes da próxima tela.
  A jornada completa passou depois desse ajuste, sem interceptar API ou aumentar timeouts:
  divisão de centavos, permissões, pagamentos, reembolso, repetição e reinício.
- `node scripts/capture-demo.mjs` terminou com saída 0; restauração preservou todas as
  linhas/valores/históricos e o ensaio de homologação passou. As duas imagens de 1536×960
  foram inspecionadas: somente dados sintéticos, sem tokens. Lint/formatação passaram.
  Logs: `demo-capture-retry.log` (reprodução) e `demo-capture-verified.log` (aprovado).
- São resultados locais da cópia isolada. O fork e os checks remotos desta entrega
  ainda dependem da conta/organização de destino; nenhum merge foi executado.

## Preparação Render/Neon/Vercel

- `deploy-config-red.log`: dois testes falharam porque a configuração não respeitava PORT.
  `deploy-config-green.log`: porta, pool de cinco e perfil TLS aprovados.
- `deploy-api-red.log`: VITE_API_URL com barra final gerava //api nas chamadas e no refresh.
  Corrigida a normalização; `deploy-frontend-green.log`: lint, Prettier, 21 testes e build aprovados.
- `deploy-render-local.log`: backend na porta 10000, PostgreSQL com TLS real e certificado
  verificado, conexões limitadas, CORS exato, cadastro e healthcheck 200/503 aprovados.
- `deploy-actionlint.log`: workflows aprovados. A revisão automática recusou montar todo
  o repositório; a validação usou somente os YAMLs em montagem isolada, sem rede.
- O ensaio não usa Neon real nem configura painéis. URLs, credenciais e aprovação do CI
  da branch de deploy devem ser conferidas no registro de continuidade e no PR.

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
| Restauração antes do fim da inicialização | Banco de destino ainda não existia | `database-backup.mjs`: consulta real por TCP antes de restaurar; mesmo backup restaurado com sucesso |

Reproduções financeiras usam PostgreSQL e bloqueios reais. Erros iniciais de
fixture (cache das estatísticas do PostgreSQL), falta de binding nativo no
verificador Alpine e contenção local foram diagnosticados separadamente; não
foram contados como defeitos funcionais nem justificaram redução dos gates.
Na jornada real, o trace mostrou renovação concorrente ao logout ainda em curso;
o teste passou a aguardar o 204 do logout antes de exigir 401 na renovação.
O log `system-confirmed.log` reproduziu a corrida de inicialização na restauração;
`restore-readiness-green.log` confirmou a correção usando o mesmo dump. O servidor
temporário da imagem usa socket durante a inicialização. `pg_isready` sozinho
[não comprova a existência do banco](https://www.postgresql.org/docs/17/app-pg-isready.html).

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
