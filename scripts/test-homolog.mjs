import { spawnSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

export async function verifyHomolog(evidence) {
  mkdirSync(evidence, { recursive: true });
  const project = `casacontas-homolog-test-${Date.now()}`;
  const port = process.env.HOMOLOG_TEST_PORT ?? "3101";
  const origin = `http://127.0.0.1:${port}`;
  const env = {
    ...process.env,
    POSTGRES_DB: "casacontas_homolog_test",
    POSTGRES_USER: "casacontas_test",
    POSTGRES_PASSWORD: "synthetic-homolog-password",
    JWT_SECRET: "synthetic-homolog-secret-with-at-least-32-bytes",
    POSTGRES_VOLUME: `${project}-postgres`,
    PUBLIC_ORIGIN: origin,
    HOMOLOG_APP_PORT: port,
    BACKEND_IMAGE: "casacontas-test-backend:local",
    FRONTEND_IMAGE: "casacontas-test-frontend:local",
  };
  const compose = [
    "compose",
    "-f",
    path.join(root, "compose.homolog.yaml"),
    "-p",
    project,
  ];
  function docker(args) {
    const result = spawnSync("docker", args, {
      cwd: root,
      env,
      encoding: "utf8",
      maxBuffer: 8 * 1024 * 1024,
    });
    if (result.error || result.status !== 0)
      throw new Error(
        `Homologação local: ${result.error?.message ?? result.stderr}`,
      );
    return result.stdout.trim();
  }
  try {
    const absent = { ...env, JWT_SECRET: "" };
    const rejected = spawnSync("docker", [...compose, "config", "--quiet"], {
      cwd: root,
      env: absent,
      encoding: "utf8",
    });
    if (rejected.status === 0 || !rejected.stderr.includes("JWT_SECRET"))
      throw new Error("Compose deveria recusar chave ausente");
    docker([...compose, "config", "--quiet"]);
    docker([
      ...compose,
      "up",
      "-d",
      "--pull",
      "never",
      "--wait",
      "--wait-timeout",
      "240",
    ]);
    const health = await fetch(`${origin}/healthz`);
    if (health.status !== 200)
      throw new Error("Frontend da homologação não está saudável");
    const preflight = await fetch(`${origin}/api/v1/auth/login`, {
      method: "OPTIONS",
      headers: {
        Origin: "https://origem-nao-permitida.example",
        "Access-Control-Request-Method": "POST",
      },
    });
    if (preflight.status !== 403)
      throw new Error("Origem não permitida deveria ser recusada");
    const registration = await fetch(`${origin}/api/v1/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Origin: origin },
      body: JSON.stringify({
        name: "Teste Homologação",
        email: "homolog@casacontas.test",
        password: "Senha-sintetica-123!",
      }),
    });
    if (registration.status !== 201)
      throw new Error(
        `Cadastro sintético na configuração de homologação: ${registration.status}`,
      );
    for (const route of ["/docs", "/api-docs"]) {
      const code = docker([
        ...compose,
        "exec",
        "-T",
        "backend",
        "curl",
        "-s",
        "-o",
        "/dev/null",
        "-w",
        "%{http_code}",
        `http://localhost:8080${route}`,
      ]);
      if (code !== "404")
        throw new Error(
          `${route} desativado deveria retornar 404, recebeu ${code}`,
        );
    }
    const backend = docker([...compose, "ps", "-q", "backend"]);
    const database = docker([...compose, "ps", "-q", "postgres"]);
    if (docker(["port", backend]) || docker(["port", database]))
      throw new Error("API e banco não devem publicar portas");
    const versions = docker([
      ...compose,
      "exec",
      "-T",
      "postgres",
      "psql",
      "-U",
      "casacontas_test",
      "-d",
      "casacontas_homolog_test",
      "-At",
      "-c",
      "select version from flyway_schema_history where success order by installed_rank",
    ]);
    if (versions.replaceAll("\r", "") !== "1\n2")
      throw new Error("Migrations esperadas não foram aplicadas");
    writeFileSync(
      path.join(evidence, "homolog-result.json"),
      JSON.stringify(
        {
          result: "PASS",
          project,
          volume: env.POSTGRES_VOLUME,
          migrations: ["1", "2"],
          https: "depende da hospedagem; ensaio apenas em loopback",
          backendImage: docker([
            "image",
            "inspect",
            env.BACKEND_IMAGE,
            "--format",
            "{{.Id}}",
          ]),
          frontendImage: docker([
            "image",
            "inspect",
            env.FRONTEND_IMAGE,
            "--format",
            "{{.Id}}",
          ]),
        },
        null,
        2,
      ) + "\n",
    );
    console.log(
      "PASS: configuração de homologação, saúde, autenticação, CORS, migrations e portas privadas.",
    );
  } finally {
    const logs = spawnSync("docker", [...compose, "logs", "--no-color"], {
      cwd: root,
      env,
      encoding: "utf8",
      maxBuffer: 8 * 1024 * 1024,
    });
    writeFileSync(
      path.join(evidence, "homolog-services.log"),
      logs.stdout ?? "",
    );
    docker([...compose, "down"]);
    console.log(`Volume de ensaio preservado: ${env.POSTGRES_VOLUME}`);
  }
}

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  await verifyHomolog(
    path.join(root, ".data/evidence", `homolog-${Date.now()}`),
  );
}
