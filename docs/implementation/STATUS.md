# Continuidade — contribuições legítimas em fork

Atualizado em 2026-10-14. Resultados pertencem à revisão e ao ambiente indicados.

- Objetivo: corrigir auditorias, testar convites e melhorar colaboração/apresentação.
- Trabalho isolado em `.data/achievements-work`; branch `fix/auditoria-backend`.
- Commit frontend: `fb3b1a0`; backend depende dessa branch para auditoria JavaScript verde.
- Base: `2063ed054099e78189b26ffff87aa5b928129f39`, main local/remota na auditoria.
- O usuário autorizou implementar as cinco melhorias e fazer os commits em um fork.
- Nenhum push para o original, merge, ajuste de proteção ou configuração de produção.
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
- Pull Shark e YOLO confirmados no perfil público, inclusive detalhes dos eventos.
  https://github.com/Guilhermegg-06
- Outras conquistas não confirmadas; não atribuir coautoria sem participação humana real.

## Bloqueio do fork
- Conta do conector e credencial Git: Guilhermegg-06, proprietária do original.
- POST de fork com nome diferente retornou o original, com `fork=false`.
- Não há forks existentes. O original não será tratado como fork nem receberá pushes.
- Pergunta enviada: organização existente autorizada ou URL de fork em outra conta.
- A cópia local tem apenas remoto `upstream`; nenhum remoto de publicação configurado.
- PRs, CI remoto desta entrega e confirmação posterior de conquistas dependem do destino.

## Pendências e próximo passo
1. Frontend validado e commit local criado; publicar quando o fork estiver disponível.
2. Backend validado; registrar commit local, sem aprovação remota presumida.
   O feed retornou 200 nesta sessão; não presumir URL permanentemente inválida.
3. Cobrir convites concorrentes/expirados na API com PostgreSQL isolado.
4. Atualizar CONTRIBUTING, checklist de conquistas e orientação de coautoria.
5. Acrescentar screenshots reais com dados sintéticos e exemplos ao README.
- Usar branches específicas; registrar dependências quando uma entrega usar outra.
- Testes e auditorias continuam obrigatórios; nenhum merge com checks falhando.
- Configurar proteção da main exige aprovação específica; licença exige decisão do dono.
- Render/Neon/Vercel e seus segredos não fazem parte das alterações desta missão.

## Continuidade do P0
- V1/V2 preservam histórico financeiro; retorno entre versões segue ADR 0004.
- Testes existentes distinguem interface simulada de jornada real com API/PostgreSQL.
- Testes isolados não comprovam a configuração ou a saúde dos provedores públicos.
