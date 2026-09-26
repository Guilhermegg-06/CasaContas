# ADR 0005 — Ambiente de teste em Render, Neon e Vercel

Status: aceito para preparação técnica, por solicitação do proprietário.
Data: 2026-09-25.

## Contexto

O ambiente de teste terá o backend Java 21 no Render, PostgreSQL na Neon e o
frontend Vite/React na Vercel. A aplicação permanece um monólito modular; muda
a configuração de operação e a origem das chamadas do navegador.

## Decisão

- Perfil Spring `render` com porta recebida em `PORT`, TLS PostgreSQL `verify-full`
  e confiança nas autoridades do Java. Credenciais e JWT obrigatórios no ambiente.
- Hikari inicialmente com máximo de cinco conexões por instância e mínimo de uma;
  usar endpoint direto da Neon para o único pool da aplicação e migrations Flyway.
- Os quatro segredos existem somente no Render. O Blueprint contém apenas nomes
  e `sync: false`; valores reais não entram no Git nem no frontend.
- A Vercel recebe somente a origem pública do backend em `VITE_API_URL`. O Render
  permite no CORS somente a origem HTTPS exata do frontend escolhido.
- Fallback de rotas React no `vercel.json`; nenhuma troca de framework ou redesign.
- Healthcheck `/actuator/health` verifica também o banco; indisponibilidade retorna 503.
- A promoção para main depende de todos os checks. O Blueprint aguarda checks
  antes de deploy automático quando for importado e configurado pelo operador.

## Consequências

Uma alteração de `VITE_API_URL` exige novo build. Previews com URLs diferentes
não recebem autorização CORS automaticamente. Suspensão e limites dos planos de
teste exigem verificação no provedor; aprovação do CI não comprova latência real.

Os ensaios usam PostgreSQL sintético com TLS e não acessam a Neon. Validar conexão,
domínios, backups e recuperação do banco hospedado quando os acessos existirem.
O procedimento Docker de backup local não deve ser aplicado como se a Neon fosse
um contêiner local. A compatibilidade de retorno de V2 segue o ADR 0004.

Não criar recursos pagos ou publicar valores sensíveis. Cadastro efetivo nos
provedores permanece uma etapa externa e deve ser relatado separadamente.
