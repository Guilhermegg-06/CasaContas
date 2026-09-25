import { spawnSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
import { backupDatabase, verifyRestore } from "./database-backup.mjs";
import { verifyHomolog } from "./test-homolog.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const project = `casacontas-e2e-${Date.now()}`;
const evidence = path.join(root, ".data", "evidence", project);
mkdirSync(evidence, { recursive: true });
const compose = [
  "compose",
  "-f",
  path.join(root, "compose.test.yaml"),
  "-p",
  project,
];
const env = {
  ...process.env,
  CASACONTAS_TEST_PROJECT: project,
  E2E_BASE_URL: `http://127.0.0.1:${process.env.E2E_APP_PORT ?? "3100"}`,
};
function run(command, args, cwd = root) {
  const result = spawnSync(command, args, {
    cwd,
    env,
    stdio: "inherit",
    shell: false,
  });
  if (result.error || result.status !== 0)
    throw new Error(
      `${command} failed: ${result.error?.message ?? result.status}`,
    );
}
console.log(`Banco isolado: ${project}; evidências: ${evidence}`);
try {
  run("docker", [
    ...compose,
    "up",
    "--build",
    "-d",
    "--wait",
    "--wait-timeout",
    "240",
  ]);
  run(
    process.execPath,
    [
      path.join(root, "frontend/node_modules/@playwright/test/cli.js"),
      "test",
      "--config",
      "playwright.real.config.ts",
    ],
    path.join(root, "frontend"),
  );
  run("docker", [...compose, "stop", "backend"]);
  const backup = path.join(evidence, "backup");
  backupDatabase(path.join(root, "compose.test.yaml"), project, backup);
  await verifyRestore(backup);
  await verifyHomolog(evidence);
  writeFileSync(
    path.join(evidence, "result.txt"),
    "PASS: jornada real, reinício preservando volume e restauração isolada\n",
  );
} finally {
  const logs = spawnSync("docker", [...compose, "logs", "--no-color"], {
    cwd: root,
    env,
    encoding: "utf8",
    maxBuffer: 16 * 1024 * 1024,
  });
  writeFileSync(path.join(evidence, "services.log"), logs.stdout ?? "");
  run("docker", [...compose, "down"]);
  console.log(`Volume preservado: ${project}_test-postgres-data`);
}
