# Continuidade — estabilização para homologação

Atualizado em 2026-09-25. Evidências valem para a revisão indicada.

- Objetivo: estabilizar P0 e preparar homologação, sem deploy público/custos.
- Branch: `fix/estabilizacao-homologacao`; entrega funcional `fc29949`.
- A consolidação posterior ajusta sincronização do teste de logout e documentação.
- `3f71ef1` consolida evidências; correção posterior aguarda SQL/TCP antes de restaurar.
- Base: `3fefc28`; preservar mudanças do usuário em `backend/mvnw.cmd` e `CasaContas/`.
- Pilar: `docs/requirements/engenharia-de-requisitos.md`.
- Issue: https://github.com/Guilhermegg-06/CasaContas/issues/12
- PR/checks da revisão atual: https://github.com/Guilhermegg-06/CasaContas/pull/13

## Concluído
- Maven Wrapper 100755/LF; publicação depende dos checks da mesma revisão.
- Auditoria NVD obrigatória recuperada, sem reduzir gates ou suprimir CVEs.
- Tomcat 11.0.26 e Swagger UI 5.32.15; versão do WebJar alinhada ao Springdoc.
- Sessão: refresh compartilhado, logout não restaura sessão e cache separado por usuário.
- Interface preservada: contrato MEMBER, erros/retry, detalhe atualizado e fuso da casa.
- Finanças: criação/pagamento atômicos; bloqueio antes de idempotência/edição/cancelamento.
- Canceladas recusam pagamento; pagamento principal não sobrepõe confirmação individual.
- V2 preserva cotas antigas e restringe duplicações; V1 intacta.
- BigDecimal/NUMERIC(19,2), centavos sem overflow e validação antes de persistir.
- Detalhe, filtros e painel usam a data da casa; status inválido recebe 400.
- Documentação desativada retorna 404; Swagger habilitado segue funcionando.
- Jornada real obrigatória no CI, sem interceptações e com banco/volume próprios.
- Compose de homologação, exemplos sem segredos, portas privadas e rotação de logs.
- Backup binário e restauração isolada com comparação das 11 tabelas.
- ADR 0004, contratos, rastreabilidade, README e runbooks consolidados.

## Evidências
- CI completo de `fc29949`: https://github.com/Guilhermegg-06/CasaContas/actions/runs/36180856299
- CodeQL: https://github.com/Guilhermegg-06/CasaContas/actions/runs/36180856029
- Backend no CI: 32 testes, zero falhas/erros/ignorados; JaCoCo/SpotBugs aprovados.
- PIT: 47/52 mutações detectadas (90%); cobertura das classes mutadas 86%.
- Frontend: lint, Prettier, 19 testes e build; auditorias npm/NVD aprovadas.
- Playwright com API simulada: desktop/celular aprovados; não comprova integração real.
- Jornada real no CI: 35,4 s; moradores, 100/3, divisões, edição, pagamentos e reembolso,
  repetição, refresh/logout/login, cancelamento, filtros, isolamento e reinício com volume.
- CI comprovou restauração, configuração de homologação, CORS, saúde, migrations e portas.
- MigrationUpgradeTest: V1 com dados → V2, preservando valores/auditoria e reaplicação vazia.
- FinancialIntegrationTest: oito testes, com concorrência coordenada por bloqueios reais.
- Logs locais ignorados: backend-final, frontend-verify, disabled-docs-red/green,
  system-resumed, system-final (corrida na fixture de logout) e system-confirmed.
- Jornada local com logout sincronizado passou (1,8 min); ensaio completo em system-confirmed.
- system-confirmed revelou inicialização prematura no restore; mesmo dump passou
  após correção em restore-readiness-green.log. Homologação local em homolog-local-green.log.
- actionlint-final.log aprovado; frontend-new-tests-lint.log sem erros; Prettier aprovado.
- Reprodução/correção e distinção de falhas de infraestrutura em TDD_EVIDENCE.md.
- Novas revisões exigem todos os checks do PR novamente; não inferir aprovação do HEAD.

## Commits e próximos passos
- `46ebf3f`: sessão/interface; `0d6c8d6`: integridade financeira e histórico.
- `852dc37`: 404; `26b1c61`: operação/restore; `fc29949`: jornada real/gate de publicação.
- Revisar PR #13 e seus checks finais antes de merge; nenhuma alteração direta na main.
- Preparação técnica comprovada; deploy aguarda provedor, domínio/TLS, disco persistente,
  backups externos, monitoramento e responsáveis, conforme docs/operations/HOMOLOGACAO.md.
- Retorno à versão anterior à V2 é incompatível após edição: usar imagem compatível
  ou restauração em volume novo com reconciliação. Nunca fazer downgrade/limpeza automática.
- Main sem proteção; configurar checks obrigatórios antes de uso real.
- Dependabot #1/#4/#5/#6/#7/#8/#11 permanece para avaliação de compatibilidade própria.
- Host Java 8/26: backend validado com Java 21 no Docker; CI usa Node 22.20.0.
- GitHub escrito por REST com credencial Git local; conector recusou escritas (403).
- Volumes dos ensaios locais foram preservados; nenhum dado real usado ou apagado.
