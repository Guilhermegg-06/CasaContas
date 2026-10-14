# Continuidade — contribuições legítimas

Atualizado em 2026-10-14. Resultados pertencem à revisão e ao ambiente indicados.

- Objetivo: corrigir auditorias, testar convites e melhorar colaboração/apresentação.
- Trabalho isolado em `.data/achievements-work`; branch `teste/contribuicoes`.
- Commit colaboração: `0dadbaa`; apresentação depende das quatro branches anteriores.
- Apresentação: `527b3fc`; os cinco commits estão reunidos na branch de teste.
- Commit convites: `b89015f`; documentação depende dele, de `e2234f0` e `fb3b1a0`.
- Commit backend: `e2234f0`; branch de convites depende dele.
- Commit frontend: `fb3b1a0`; backend depende dessa branch para auditoria JavaScript verde.
- Base: `2063ed054099e78189b26ffff87aa5b928129f39`, main local/remota na auditoria.
- O usuário autorizou implementar as cinco melhorias e fazer os commits em um fork.
- Publicação autorizada na branch de teste do original; main e produção preservadas.
- Preservar arquivos do usuário: `backend/mvnw.cmd` e pasta não rastreada `CasaContas/`.
- Escopo continua P0; não editar migrations aplicadas nem usar dados reais.

## Concluído e evidências
- Auditoria inicial de branches, PRs, Issues, Actions, proteção e perfil.
- PRs #13 e #14 integrados; último CI completo da main aprovado em 27/09:
  https://github.com/Guilhermegg-06/CasaContas/actions/runs/36282112418
- Auditoria semanal de 12/10 falhou; CodeQL de 13/10 passou. CI atual não está saudável.
- Frontend: reprodução automatizada com quatro vulnerabilidades altas e saída 1.
- Correções compatíveis apenas no lockfile: brace-expansion 5.0.12, fast-uri 3.1.8,
  source-map-js 1.2.2 e undici 7.30.0; nenhuma dependência direta alterada.
- Node 22.20.0: instalação limpa e auditoria aprovadas, zero vulnerabilidades.
- Node 22.20.0: lint, Prettier, 21 testes em sete arquivos e build aprovados.
- Navegador local (Node 24): duas jornadas com API simulada aprovadas, desktop e celular.
- Logs completos da cópia isolada: `.data/evidence/frontend-*`.
- Backend: dois testes reproduziram a ausência de repetição no workflow antigo.
- Cinco testes offline do auditor passaram; vulnerabilidade continua bloqueada sem repetição.
- Swagger UI 5.33.2 incorpora DOMPurify 3.4.16, confirmado no WebJar publicado.
- Java 21/PostgreSQL: 34 testes, cobertura e SpotBugs passaram; Actionlint passou.
- Encerramento do JVM de testes excedeu 30 s neste host; o build completo terminou com saída 0.
- Auditoria real backend aprovada em Java 21, em 29 min, com zero achados em 78 dependências.
- CVSS 7 preservado; relatórios em `.data/evidence/backend-audit-result` e `backend-audit-real.log`.
- Convites: dois testes novos com API/PostgreSQL real passaram; um único vencedor
  concorrente persistido e recusa de convite expirado sem consumo/vínculo.
- Suíte completa/mutação aprovadas: 36 testes; 47/52 mutações eliminadas (90%), força 98%.
- Pull Shark e YOLO confirmados no perfil público, inclusive detalhes dos eventos.
  https://github.com/Guilhermegg-06
- Outras conquistas não confirmadas; não atribuir coautoria sem participação humana real.
- README com duas capturas reais de 1536×960, inspecionadas, apenas dados sintéticos.
- Jornada real passou: centavos, isolamento, pagamentos/reembolso, repetição e reinício.
- Backup restaurado preservou linhas, valores e históricos; ensaio de homologação passou.
- `node scripts/capture-demo.mjs`: saída 0; lint/formatação da jornada aprovados.
- Cadastro sincronizado com resposta 201; log `demo-capture-verified.log` comprova resultado.

## Destino da publicação
- Conta do conector e credencial Git: Guilhermegg-06, proprietária do original.
- POST de fork com nome diferente retornou o original, com `fork=false`.
- Não há fork confirmado. O original não será tratado como fork.
- Usuário solicitou reunir os commits em uma branch de teste após o pedido online.
- Destino: `upstream`, Guilhermegg-06/CasaContas, branch `teste/contribuicoes`.
- Branch: https://github.com/Guilhermegg-06/CasaContas/tree/teste/contribuicoes

## Pendências e próximo passo
1. Cinco commits reunidos sem reescrita; este registro acompanha a publicação.
2. Publicar somente a branch de teste e acionar Verificação do PR por workflow_dispatch.
3. CI remoto ainda não aprovado; conferir a revisão exata e corrigir falhas reais.
- Usar branches específicas; registrar dependências quando uma entrega usar outra.
- Testes e auditorias continuam obrigatórios; nenhum merge com checks falhando.
- Configurar proteção da main exige aprovação específica; licença exige decisão do dono.
- Render/Neon/Vercel e seus segredos não fazem parte das alterações desta missão.

## Continuidade do P0
- V1/V2 preservam histórico financeiro; retorno entre versões segue ADR 0004.
- Testes existentes distinguem interface simulada de jornada real com API/PostgreSQL.
- Testes isolados não comprovam a configuração ou a saúde dos provedores públicos.
