# Continuidade — ambiente de teste Render/Neon/Vercel

Atualizado em 2026-09-25 (horário local). Evidências valem para a revisão indicada.

- Objetivo atual: preparar Render/Neon/Vercel, validar CI e promover para main somente verde.
- Branch: `deploy/ambiente-teste`; base `7ffccc7`; commits de configuração `0f8aaa3`/`aaa2640`.
- PR #13 da estabilização já integrado; main local/remota conferidas e CI aprovado:
  https://github.com/Guilhermegg-06/CasaContas/actions/runs/36210428507
- Preservar mudanças do usuário em `backend/mvnw.cmd` e pasta não rastreada `CasaContas/`.
- Pilar: docs/requirements/engenharia-de-requisitos.md; somente P0.
- Usuário autorizou push da branch, acompanhamento/correção do CI e merge após aprovação.

## Implementado nesta etapa
- Spring usa `${PORT:8080}`; pool Hikari padrão 5, com mínimo ocioso 1.
- Perfil render: JDBC com TLS verify-full/CA do Java, segredos/origem sem fallback local.
- Render Blueprint: Docker backend, plano gratuito, checksPass e /actuator/health.
- Quatro segredos referenciados por sync:false; nenhum valor real versionado.
- Vercel: frontend/vercel.json com fallback para React e guia de VITE_API_URL.
- Cliente normaliza barra final de VITE_API_URL também nas chamadas de refresh.
- Ensaio de CI com PostgreSQL TLS real, PORT=10000, CORS exato e saúde 200/503.
- Guia operacional: docs/operations/RENDER-NEON-VERCEL.md.

## Evidências locais
- deploy-config-red.log: configuração de PORT ausente; dois testes falharam antes da mudança.
- deploy-config-green.log: dois testes de porta/pool/TLS/configuração aprovados.
- deploy-api-red.log: URL com barra final gerava //api; regressão reproduzida.
- deploy-frontend-green.log: lint, Prettier, 21 testes em sete arquivos e build aprovados.
- deploy-actionlint.log: aprovado com apenas workflows isolados e rede desativada.
- deploy-render-local.log aprovado: TLS real, porta 10000, CORS e healthcheck 200/503.
- Falta inicial de Docker e ajuste de formatação não foram contados como defeitos funcionais.

## Bloqueio externo
- URLs reais de backend/frontend e nomes dos serviços ainda não fornecidos.
- Sem CLI/credenciais Render/Vercel/Neon e sem navegador conectado disponível.
- Perguntas enviadas sobre URLs e cadastro/acesso aos segredos; não solicitar valores no chat.
- Cadastro real dos quatro segredos no Render e VITE_API_URL/CORS nos painéis não executado.
- Nenhum recurso contratado, deploy público realizado ou banco Neon acessado.
- Teste sintético local/CI não comprova a conexão com uma instância Neon real.

## Próximo passo
- Ensaio TLS concluído; enviar commits e criar PR para main.
- Aguardar todos os checks da revisão; corrigir na branch se necessário.
- Após CI verde, merge autorizado e atualização local por fast-forward, preservando trabalho.
- Configurar os painéis quando os acessos e endereços indispensáveis estiverem disponíveis.

## Continuidade da estabilização
- PR/issue anteriores: https://github.com/Guilhermegg-06/CasaContas/pull/13 e issues/12.
- V1 intacta; V2 arquiva cotas e restringe duplicações. Não retornar à imagem anterior
  após edição com V2 sem plano de restauração/reconciliação (ADR 0004).
- Base: 32 testes backend, 19 frontend, duas jornadas simuladas e uma real aprovados.
- Concorrência financeira, migrations V1→V2, reinício e backup/restauração comprovados.
- Main sem proteção; Dependabot pendente permanece fora do escopo deste ambiente.
- Host Java 8/26: backend validado em Docker Java 21; CI usa Node 22.20.0.
- GitHub: REST com credencial Git local; conector recusou escritas anteriormente.
