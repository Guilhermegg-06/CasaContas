# Ambiente de teste: Render, Neon e Vercel

Configuração preparada no repositório. Cadastrar valores nos provedores exige
acesso às contas e as URLs reais. Arquivos versionados e CI não cadastram segredos
automaticamente. O resultado de cada revisão fica no PR e em `STATUS.md`.

## Render e Neon

O `render.yaml` define o backend Docker, branch `main`, plano gratuito,
`autoDeployTrigger: checksPass` e healthcheck `/actuator/health`. Ele não cria
recursos até ser importado no painel. Não autoriza contratar plano pago.
O contexto de build é `backend/`, usando seu Dockerfile Java 21.

Selecionar um banco Neon exclusivo para teste, sem dados reais, e usar o endpoint
direto inicialmente. O Hikari já limita o processo a cinco conexões, com uma
conexão ociosa mínima. Flyway usa a mesma configuração de conexão e aplica V1/V2;
não executar `clean` nem editar migrations aplicadas.

Em **Render → serviço → Environment**, cadastrar somente ali os quatro segredos:

| Nome | Valor a obter/configurar no painel |
|---|---|
| `DATABASE_URL` | URL JDBC do banco Neon de teste, no formato abaixo |
| `POSTGRES_USER` | Papel do banco Neon |
| `POSTGRES_PASSWORD` | Senha desse papel |
| `JWT_SECRET` | Chave aleatória exclusiva, com pelo menos 32 bytes |

Formato ilustrativo, sem usuário ou senha na URL:

```text
jdbc:postgresql://HOST_NEON:5432/NOME_BANCO
```

Não usar a URI `postgresql://usuario:senha@...` diretamente em JDBC. Não copiar
parâmetros `sslmode=require/disable` da URI do painel: o perfil `render` configura
`sslmode=verify-full` e `org.postgresql.ssl.DefaultJavaSSLFactory`. Isso valida o
certificado e o hostname com as autoridades confiáveis do Java, além de criptografar
o tráfego. Não usar a CA sintética do teste fora do CI.

As quatro entradas usam `sync: false` no Blueprint: o Render solicita seus valores
no cadastro inicial. Em serviço existente, conferir/cadastrar no painel; não
presumir que sincronizar o YAML preenche valores ausentes. Não colocar segredos
no Git, em `.env` versionado, em GitHub Actions, no build da Vercel ou nas evidências.

Outras variáveis do Render:

- `SPRING_PROFILES_ACTIVE=render` (obrigatório para TLS e configuração sem valores locais).
- `DB_POOL_SIZE=5`, configurado no Blueprint; o limite é por instância.
- `PORT` fornecido pelo Render: Spring usa `${PORT:8080}` e escuta em `0.0.0.0`.
- `CORS_ALLOWED_ORIGINS`: **uma única origem HTTPS exata** do frontend, sem barra
  final, caminho, lista ou curinga. Exemplo fictício: `https://frontend.example.test`.

O healthcheck é público e não expõe detalhes do banco. Retorna 200 quando saudável
e 503 quando o banco está indisponível. Documentação Swagger fica desativada nesse perfil.

## Vercel

Usar o projeto existente ou importar o repositório, com **Root Directory = frontend**,
framework Vite, build `npm run build`, saída `dist` e branch de produção `main`.
`frontend/vercel.json` entrega `index.html` nas rotas React, permitindo abrir ou
recarregar diretamente caminhos como `/app/despesas`.

Em **Project → Settings → Environment Variables**, cadastrar `VITE_API_URL` com
a origem HTTPS real do backend Render, sem `/api/v1`. É uma configuração pública:
Vite a incorpora no JavaScript durante o build. Nunca colocar credenciais em
variáveis `VITE_*`. Após alterar o valor, reconstruir a aplicação; mudar só a
variável não altera um build já existente.

Definir o valor para o ambiente que será utilizado. Uma URL temporária de preview
não deve motivar um CORS com `*.vercel.app`: escolher uma URL estável de teste e
cadastrar exatamente essa origem no Render. Confirmar as duas URLs antes de publicar.

## Verificação e promoção

1. Abrir PR de `deploy/ambiente-teste` para `main`; o evento do PR inicia todos os checks.
2. Aguardar o CI do SHA enviado. Se falhar, corrigir na branch e repetir; não reduzir gates.
3. Após todos os checks, integrar o PR e acompanhar o workflow da `main`.
4. Configurar os provedores e validar `/actuator/health`, cadastro/login, convite,
   despesa, reembolso, logout e recarga em rota interna com dados sintéticos.
5. Registrar os endereços públicos e a revisão publicada, sem segredos. Seguir o
   plano de backup/retorno de [HOMOLOGACAO.md](HOMOLOGACAO.md) antes de atualizar dados existentes.

`node scripts/test-render.mjs` usa somente Docker local e dados sintéticos: PORT
10000, PostgreSQL com certificado de teste verificado, até cinco conexões, CORS
exato e healthcheck com banco disponível/indisponível. Não acessa Neon nem publica.
Esse ensaio é obrigatório no workflow, além dos testes financeiros e da jornada real.
Os dois testes de `DeploymentConfigurationTest` cobrem porta/pool/perfil; os testes
de `api.deployment.test.ts` cobrem login, chamadas e refresh usando `VITE_API_URL`.

## Referências oficiais

- [Render: Blueprint, segredos e healthcheck](https://render.com/docs/blueprint-spec).
- [pgJDBC: TLS e validação de certificados](https://jdbc.postgresql.org/documentation/ssl/).
- [Neon: níveis de verificação TLS](https://neon.com/blog/postgres-needs-better-connection-security-defaults).
- [Vercel: Vite e fallback de SPA](https://vercel.com/docs/frameworks/frontend/vite).
