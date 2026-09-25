# Runbook do CasaContas

## Subir do zero

1. Instale Docker Desktop com Compose v2.
2. Copie `.env.example` para `.env` e troque senha do banco e `JWT_SECRET`.
3. Execute `docker compose up --build`.
4. Abra `http://localhost:3000`.

Os serviços `postgres`, `backend` e `frontend` possuem health checks. A API só inicia depois do banco saudável, e a interface só inicia depois da API saudável.

## Diagnóstico rápido

```bash
docker compose ps
docker compose logs backend --tail=200
docker compose logs postgres --tail=200
curl http://localhost:3000/healthz
```

- `postgres` não saudável: confira credenciais e espaço do volume.
- `backend` não saudável: procure falha Flyway, conexão JDBC ou `JWT_SECRET` curto.
- interface responde, API não: confira o proxy `/api/` no Nginx e a saúde do backend.
- `401`: renove a sessão; não registre tokens completos em logs ou chamados.

## Migration

Flyway executa migrations antes de o backend receber tráfego. Nunca altere uma migration já aplicada; crie a próxima versão e valide em cópia recente do banco. Faça backup antes de mudanças destrutivas de schema.

## Backup e restauração

Siga [o procedimento de homologação](HOMOLOGACAO.md): interrompa escritas,
capture o dump com `scripts/database-backup.mjs` e valide a restauração em um
PostgreSQL novo e isolado. O script preserva o banco original e funciona no
PowerShell sem redirecionamento de conteúdo binário. Nunca restaure sobre o
banco em uso nem remova seu volume como parte de uma atualização.

## Rollback

1. Preserve o banco e colete logs com o correlation ID.
2. Pare o tráfego da versão defeituosa.
3. Confirme a compatibilidade da imagem anterior com o schema atual antes de trocar o digest.
4. A versão anterior à V2 não entende cotas arquivadas; siga o plano de retorno em [HOMOLOGACAO.md](HOMOLOGACAO.md).
5. Verifique `/actuator/health`, cadastro/login e leitura do painel.

O workflow de publicação de imagens depende dos checks da mesma revisão da
`main`. Construção de imagens e testes em PR não publicam uma aplicação.
Provedor, domínio, HTTPS e armazenamento externo de backups continuam pendentes
para a futura hospedagem. Veja a revisão comprovada em [STATUS.md](../implementation/STATUS.md).
