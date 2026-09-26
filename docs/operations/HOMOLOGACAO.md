# Homologação: preparação e verificação

Este procedimento não escolhe provedor nem publica serviços. Só usar dados
sintéticos nesta fase. A aprovação técnica da revisão está em
[`STATUS.md`](../implementation/STATUS.md), com links para o CI e testes executados.

## Configuração

1. Copiar `.env.homolog.example` para `.env.homolog` e restringir sua leitura ao operador.
2. Gerar senha própria do PostgreSQL e chave JWT aleatória com pelo menos 32 bytes.
   Não reutilizar exemplos de desenvolvimento. Não registrar segredos em logs, tickets ou Git.
3. Fixar `BACKEND_IMAGE` e `FRONTEND_IMAGE` por digest, da mesma revisão validada.
   Preservar os digests anteriores e o backup correspondente.
4. Manter `POSTGRES_VOLUME` fixo entre atualizações. O banco não possui porta pública.
5. Definir `PUBLIC_ORIGIN` como a origem HTTPS exata, sem caminho ou barra final.
   A interface chama `/api/` na mesma origem. CORS não substitui autenticação.

```powershell
docker compose --env-file .env.homolog -f compose.homolog.yaml config --quiet
docker compose --env-file .env.homolog -f compose.homolog.yaml pull
docker compose --env-file .env.homolog -f compose.homolog.yaml up -d --wait --wait-timeout 240
```

O Compose recusa segredos/imagens ausentes. Somente o frontend é ligado ao host,
em `127.0.0.1:3101`; o provedor deve encaminhar HTTPS até esse endereço por proxy
privado. Swagger permanece disponível no desenvolvimento e desativado nesse ambiente.
O JWT de acesso dura 10 minutos; o refresh rotativo dura 30 dias. Logout revoga
o refresh, e tokens de acesso já emitidos expiram em até 10 minutos.

## Dependências da hospedagem

- Domínio, DNS, certificado TLS com renovação, redirecionamento HTTP → HTTPS e
  acesso restrito à homologação. Não expor banco/API diretamente à internet.
- Persistência do disco/volume entre recriações e acesso administrativo restrito.
- Armazenamento de backups criptografado, separado do host, com retenção e teste
  de recuperação. Definir com o responsável a perda de dados e prazo de recuperação aceitáveis.
- Monitorar saúde, espaço do disco, falhas de migration e erros 5xx. Logs JSON têm
  rotação local; agregar com controle de acesso, sem corpos de login ou tokens.
- Custos, capacidade, alertas e responsáveis operacionais antes de contratar/publicar.

## Atualização, backup e retorno

Flyway aplica V1/V2 em banco vazio e somente V2 em banco V1. Nunca editar V1 nem
executar `clean`. O teste de upgrade contém valores e histórico sintéticos.

Antes de atualizar: avisar os testadores, interromper escritas e parar o backend.
O PostgreSQL permanece ativo. Use um diretório novo para cada backup:

```powershell
docker compose --env-file .env.homolog -f compose.homolog.yaml stop backend
node scripts/database-backup.mjs backup compose.homolog.yaml casacontas-homolog .data/backups/antes-da-atualizacao .env.homolog
node scripts/database-backup.mjs verify .data/backups/antes-da-atualizacao
```

Criar primeiro `.data/backups` se não existir. O dump usa formato binário próprio
do PostgreSQL, copiado por `docker cp`, sem redirecionamento binário do PowerShell.
`verify` sempre cria um PostgreSQL isolado, restaura com erro fatal habilitado e
compara contagens e resumos de todas as linhas, inclusive cotas, pagamentos,
idempotência, usuários e auditoria. Remove somente sua cópia isolada após o teste.
O backup original e o volume principal permanecem intactos. Para a comparação
ser válida, manter escritas paradas durante sua captura. Em caso de falha, conservar
o backup, diagnosticar e reabrir a versão atual; não iniciar a atualização.

Após backup/restauração aprovados, alterar os digests e executar `up -d --wait`.
Anotar revisão, volume, versão Flyway, backup e horário. Nunca executar `down -v`
na homologação. Reinício normal ou `down` sem `-v` mantém o volume nomeado.

**Compatibilidade:** a versão original não entende cotas inativas de V2. Após
uma edição com V2, não retornar à imagem anterior à estabilização: ela calcularia
com cotas antigas. Usar uma imagem compatível com V2 ou restaurar o backup anterior
em um volume novo, avaliar escritas posteriores e aprovar a troca de destino.
O script de verificação não faz essa troca e não oferece restauração destrutiva.
Ver [ADR 0004](../architecture/adr/0004-transacoes-e-historico-de-cotas.md).

## Roteiro após publicação futura

1. Conferir digests e revisão aprovada, volume esperado e todos os health checks.
2. Conferir HTTPS/certificado, redirecionamento HTTP e ausência de portas públicas do banco/API.
3. Cadastrar usuários sintéticos, criar casa, entrar por convite e testar login/logout.
4. Dividir R$ 100,00 em três partes, rejeitar personalizada inválida e registrar pagamento/reembolso.
5. Repetir confirmação, verificar um único registro, saldos, filtros e histórico; outra casa deve receber recusa.
6. Recarregar e reiniciar serviços preservando registros. Confirmar backup e restauração isolada.
7. Registrar resultado e anomalias com correlation ID, sem segredos. Só então liberar a homologação aos testadores.
