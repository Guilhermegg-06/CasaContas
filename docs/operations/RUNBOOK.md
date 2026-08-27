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

```bash
docker compose exec -T postgres pg_dump -U casacontas -Fc casacontas > casacontas.dump
docker compose exec -T postgres pg_restore -U casacontas -d casacontas --clean --if-exists < casacontas.dump
```

A restauração é destrutiva para o banco de destino e exige janela aprovada. Valide primeiro em ambiente isolado.

## Rollback

1. Preserve o banco e colete logs com o correlation ID.
2. Pare o tráfego da versão defeituosa.
3. Suba a imagem anterior conhecida pelo mesmo digest.
4. Não reverta migration incompatível sem plano de dados.
5. Verifique `/actuator/health`, cadastro/login e leitura do painel.

O workflow de publicação de imagens não foi criado enquanto não houver autorização explícita para publicar no GHCR.
