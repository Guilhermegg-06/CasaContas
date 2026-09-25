import { spawnSync } from "node:child_process";
import { randomUUID, randomBytes } from "node:crypto";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { setTimeout } from "node:timers/promises";

function docker(args, options = {}) {
  const result = spawnSync("docker", args, {
    encoding: "utf8",
    maxBuffer: 8 * 1024 * 1024,
    ...options,
  });
  if (result.error || result.status !== 0)
    throw new Error(`Docker: ${result.error?.message ?? result.stderr}`);
  return result.stdout.trim();
}

const tables = [
  "users",
  "refresh_sessions",
  "households",
  "household_members",
  "invitations",
  "expenses",
  "expense_shares",
  "settlements",
  "idempotency_records",
  "audit_events",
  "flyway_schema_history",
];
const fingerprintSql = `SELECT jsonb_build_object(${tables
  .map((table) => {
    const key = table === "flyway_schema_history" ? "installed_rank" : "id";
    return `'${table}', (SELECT jsonb_build_object('rows', count(*), 'digest',
    md5(coalesce(string_agg(md5(to_jsonb(t)::text), '' ORDER BY ${key}), ''))) FROM ${table} t)`;
  })
  .join(",")})::text;`;

function fingerprint(container) {
  return JSON.parse(
    docker(
      [
        "exec",
        "-i",
        container,
        "sh",
        "-ceu",
        'exec psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -v ON_ERROR_STOP=1',
      ],
      { input: fingerprintSql },
    ),
  );
}

export function backupDatabase(composeFile, project, destination, envFile) {
  if (!/^[a-z0-9][a-z0-9_-]*$/.test(project))
    throw new Error("Nome de projeto inválido");
  mkdirSync(destination, { recursive: false }); // Não sobrescrever backup existente.
  const compose = ["compose", "-f", path.resolve(composeFile), "-p", project];
  if (envFile) compose.push("--env-file", path.resolve(envFile));
  const container = docker([...compose, "ps", "-q", "postgres"]);
  if (!/^[a-f0-9]{12,64}$/.test(container))
    throw new Error("Um único PostgreSQL ativo é obrigatório");
  const temporary = `/tmp/casacontas-backup-${randomUUID()}.dump`;
  try {
    const manifest = {
      format: 1,
      capturedAt: new Date().toISOString(),
      tables: fingerprint(container),
    };
    docker([
      "exec",
      container,
      "sh",
      "-ceu",
      'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --file="$1"',
      "--",
      temporary,
    ]);
    docker(["exec", container, "pg_restore", "--list", temporary]);
    docker([
      "cp",
      `${container}:${temporary}`,
      path.join(destination, "database.dump"),
    ]);
    writeFileSync(
      path.join(destination, "manifest.json"),
      JSON.stringify(manifest, null, 2) + "\n",
    );
    console.log(
      `Backup salvo em ${destination}. Validar com restauração isolada.`,
    );
  } finally {
    docker(["exec", container, "rm", "-f", temporary]);
  }
}

export async function verifyRestore(destination) {
  const manifest = JSON.parse(
    readFileSync(path.join(destination, "manifest.json"), "utf8"),
  );
  if (manifest.format !== 1)
    throw new Error("Formato de manifesto desconhecido");
  const container = `casacontas-restore-${randomUUID()}`;
  // Somente este contêiner novo é removido; nenhum destino existente é aceito.
  const env = {
    ...process.env,
    POSTGRES_PASSWORD: randomBytes(32).toString("hex"),
  };
  try {
    docker(
      [
        "run",
        "-d",
        "--name",
        container,
        "--network",
        "none",
        "-e",
        "POSTGRES_USER=postgres",
        "-e",
        "POSTGRES_DB=casacontas_restore",
        "-e",
        "POSTGRES_PASSWORD",
        "postgres:17.6-alpine",
      ],
      { env },
    );
    let ready = false;
    for (let attempt = 0; attempt < 60; attempt++) {
      const check = spawnSync(
        "docker",
        [
          "exec",
          container,
          "sh",
          "-ceu",
          // O servidor temporário do entrypoint usa socket antes de criar o banco.
          // Exigir TCP e uma consulta confirma a inicialização do destino correto.
          'PGPASSWORD="$POSTGRES_PASSWORD" exec psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -X -v ON_ERROR_STOP=1 -Atc "SELECT 1"',
        ],
        { stdio: "ignore" },
      );
      if (check.status === 0) {
        ready = true;
        break;
      }
      await setTimeout(1000);
    }
    if (!ready) throw new Error("PostgreSQL de restauração não ficou saudável");
    docker([
      "cp",
      path.join(destination, "database.dump"),
      `${container}:/tmp/database.dump`,
    ]);
    docker([
      "exec",
      container,
      "pg_restore",
      "--exit-on-error",
      "--no-owner",
      "--no-privileges",
      "-U",
      "postgres",
      "-d",
      "casacontas_restore",
      "/tmp/database.dump",
    ]);
    const restored = fingerprint(container);
    for (const table of tables) {
      if (
        JSON.stringify(restored[table]) !==
        JSON.stringify(manifest.tables[table])
      ) {
        throw new Error(
          `Restauração divergente na tabela ${table}; confira se havia escritas durante o backup`,
        );
      }
    }
    writeFileSync(
      path.join(destination, "restore-result.json"),
      JSON.stringify(
        {
          result: "PASS",
          verifiedAt: new Date().toISOString(),
          tables: restored,
        },
        null,
        2,
      ) + "\n",
    );
    console.log(
      "PASS: restauração isolada preservou todas as linhas, valores e históricos.",
    );
  } finally {
    docker(["rm", "-f", "-v", container]);
  }
}

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  const [operation, ...args] = process.argv.slice(2);
  if (operation === "backup" && args.length >= 3)
    backupDatabase(args[0], args[1], path.resolve(args[2]), args[3]);
  else if (operation === "verify" && args.length === 1)
    await verifyRestore(path.resolve(args[0]));
  else
    throw new Error(
      "Uso: database-backup.mjs backup <compose> <projeto> <diretório novo> [env-file] | verify <diretório>",
    );
}
