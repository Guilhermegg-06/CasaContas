import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const project = `casacontas-render-test-${Date.now()}`;
const evidence = path.join(root, ".data/evidence", project);
const compose = [
  "compose",
  "-f",
  path.join(root, "compose.render-test.yaml"),
  "-p",
  project,
];
const base = `http://127.0.0.1:${process.env.RENDER_TEST_PORT ?? "3110"}`;
mkdirSync(evidence, { recursive: true });

function docker(args) {
  const result = spawnSync("docker", args, {
    cwd: root,
    encoding: "utf8",
    maxBuffer: 32 * 1024 * 1024,
  });
  if (result.error || result.status !== 0) {
    writeFileSync(
      path.join(evidence, "command-error.log"),
      `${result.stdout ?? ""}\n${result.stderr ?? ""}`,
    );
    throw new Error(
      `Comando Docker falhou (${result.error?.message ?? result.status}); veja ${evidence}`,
    );
  }
  return result.stdout.trim();
}

async function request(route, options = {}) {
  return fetch(`${base}${route}`, {
    signal: AbortSignal.timeout(45000),
    ...options,
  });
}

console.log(`Ensaio Render com PostgreSQL/TLS isolado: ${project}`);
try {
  docker([
    ...compose,
    "up",
    "--build",
    "-d",
    "--wait",
    "--wait-timeout",
    "240",
  ]);
  const health = await request("/actuator/health");
  assert.equal(health.status, 200);
  const details = await health.json();
  assert.equal(details.status, "UP");
  assert.equal(details.components, undefined);

  const allowed = "https://frontend.example.test";
  for (const origin of [
    allowed,
    `${allowed}.evil.example`,
    "https://preview.example.test",
    "http://frontend.example.test",
  ]) {
    const response = await request("/api/v1/auth/login", {
      method: "OPTIONS",
      headers: {
        Origin: origin,
        "Access-Control-Request-Method": "POST",
        "Access-Control-Request-Headers":
          "content-type,authorization,idempotency-key",
      },
    });
    assert.equal(
      response.status,
      origin === allowed ? 200 : 403,
      `CORS: ${origin}`,
    );
    assert.equal(
      response.headers.get("access-control-allow-origin"),
      origin === allowed ? allowed : null,
    );
  }
  const registration = await request("/api/v1/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json", Origin: allowed },
    body: JSON.stringify({
      name: "Teste Render",
      email: "render@casacontas.test",
      password: "Senha-sintetica-123!",
    }),
  });
  assert.equal(registration.status, 201);
  const sql = `select count(*) > 0 and count(*) <= 5 and bool_and(s.ssl)
    from pg_stat_activity a join pg_stat_ssl s on s.pid = a.pid
    where a.pid <> pg_backend_pid() and a.usename = 'casacontas_test'
    and a.datname = current_database()`;
  const encrypted = docker([
    ...compose,
    "exec",
    "-T",
    "postgres",
    "psql",
    "-X",
    "-U",
    "casacontas_test",
    "-d",
    "casacontas_render_test",
    "-At",
    "-v",
    "ON_ERROR_STOP=1",
    "-c",
    sql,
  ]);
  assert.equal(
    encrypted,
    "t",
    "Conexões reais da aplicação devem usar TLS, com até cinco conexões",
  );
  for (const route of ["/docs", "/api-docs"])
    assert.equal((await request(route)).status, 404);

  docker([...compose, "stop", "postgres"]);
  const unavailable = await request("/actuator/health");
  assert.equal(
    unavailable.status,
    503,
    "Healthcheck deve detectar banco indisponível",
  );
  writeFileSync(
    path.join(evidence, "result.txt"),
    "PASS: PORT=10000, PostgreSQL com TLS verificado, pool <=5, CORS exato e saúde 200/503\n",
  );
  console.log(
    "PASS: porta do Render, conexão TLS real, CORS exato, autenticação e healthcheck com banco disponível/indisponível.",
  );
} finally {
  const logs = spawnSync("docker", [...compose, "logs", "--no-color"], {
    cwd: root,
    encoding: "utf8",
    maxBuffer: 32 * 1024 * 1024,
  });
  writeFileSync(path.join(evidence, "services.log"), logs.stdout ?? "");
  docker([...compose, "down"]);
  console.log(`Volumes sintéticos preservados: ${project}`);
}
