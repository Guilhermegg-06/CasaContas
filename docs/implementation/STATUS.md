# Continuidade — estabilização para homologação

Atualizado em 2026-09-24. Este registro substitui afirmações históricas de aprovação.

- Objetivo: CI, integração real e operação reproduzível do P0, sem deploy público.
- Branch: `fix/estabilizacao-homologacao`; base `3fefc28` (main local e remota).
- Preservar alterações preexistentes: `backend/mvnw.cmd` e pasta `CasaContas/`.
- Requisitos: `docs/requirements/engenharia-de-requisitos.md`.

## Concluído e evidências
- Inspecionados requisitos, arquitetura, ADRs, workflows e runbook.
- Reconfirmado Git `100644` no Maven Wrapper; reprodução local falhou como esperado.
- Log do job 107535849819: `./mvnw: Permission denied`, saída 126.
- Execução: https://github.com/Guilhermegg-06/CasaContas/actions/runs/35969569821
- Playwright existente intercepta API; não comprova integração completa.
- Identificada publicação de imagens independente dos testes da revisão.

## Em andamento
- Corrigir permissão/terminações do wrapper e dependências entre workflows.
- Preparar ambiente Windows com Java 21/Node/Docker independente da IDE.

## Pendências
- Revisar e testar autenticação, autorização, centavos e concorrência financeira.
- Jornada navegador → API → PostgreSQL; reinício preservando volume.
- Migrations em banco vazio/existente, backup/restauração e configuração de homologação.
- Executar suítes, atualizar rastreabilidade e abrir PRs com evidências.
- PRs Dependabot abertos: #1, #4, #5, #6, #7, #8, #11; avaliar sem upgrades gerais.
- Verificar proteção da main; decisões de provedor/HTTPS permanecem para hospedagem.

## Bloqueios e limites
- Java padrão do host é 8; JDK adicional é 26. Java 21 precisa de ambiente isolado.
- Docker Desktop estava desligado; inicialização solicitada.
- Rede/Git/Docker exigem execução fora do sandbox neste host.
- Ainda não há revisão desta fase aprovada no GitHub nem PR aberto.

## Próximo passo
- Commit `9d44983` enviado; PR https://github.com/Guilhermegg-06/CasaContas/pull/13
- Issue https://github.com/Guilhermegg-06/CasaContas/issues/12
- CI 36069729049: backend (verify/PIT), frontend, navegador simulado, imagens e CodeQL verdes.
- Auditoria Java: defeito confirmado no Dependency-Check 13.0.0 sem chave NVD.
- Correção upstream prevista para 13.0.1, ainda não publicada no Maven Central.
- Mantido 13.0.0 com feed JSON 2.0 oficial da NVD; auditoria continua obrigatória.
- Local: actionlint aprovado; npm ci sem vulnerabilidades; Docker ativo.
- Checkout CRLF reprovou Spotless local; `.gitattributes` agora fixa LF para fontes.
- Conector GitHub sem escrita (403); autenticação Git local permitiu issue/PR/push.
- Próximo: regressões financeiras e jornada real; ainda não pronto para homologação.
