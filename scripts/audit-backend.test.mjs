import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

function workflowCommand() {
  const lines = readFileSync(path.join(root, '.github/workflows/security.yml'), 'utf8').split(/\r?\n/)
  const step = lines.findIndex((line) => line.trim() === '- name: Analisar CVEs')
  assert.notEqual(step, -1, 'O step de auditoria deve existir')
  const run = lines.findIndex((line, index) => index > step && line.startsWith('        run:'))
  assert.notEqual(run, -1, 'O step deve executar a auditoria')
  const inline = lines[run].slice('        run:'.length).trim()
  if (inline !== '|') return inline
  const body = []
  for (let index = run + 1; index < lines.length && lines[index].startsWith('          '); index++) {
    body.push(lines[index].slice(10))
  }
  return body.join('\n')
}

function simulatedAudit(scenario) {
  const fixture = mkdtempSync(path.join(tmpdir(), 'casacontas-audit-test-'))
  try {
    for (const directory of ['backend', 'scripts', 'bin']) mkdirSync(path.join(fixture, directory))
    const auditor = path.join(root, 'scripts/audit-backend.sh')
    if (existsSync(auditor)) copyFileSync(auditor, path.join(fixture, 'scripts/audit-backend.sh'))
    const stub = [
      '#!/usr/bin/env bash',
      'if ! printf "%s\\n" "$@" | grep -Fxq -- "-Psecurity" || ! printf "%s\\n" "$@" | grep -Fxq -- "dependency-check:check"; then',
      '  echo "[ERROR] O perfil e o goal de auditoria são obrigatórios."; exit 2',
      'fi',
      'count=0',
      'if [ -f .audit-count ]; then count=$(cat .audit-count); fi',
      'count=$((count + 1))',
      'printf "%s" "$count" > .audit-count',
      'case "$AUDIT_SCENARIO:$count" in',
      '  mixed:*)',
      '    echo "[ERROR] UpdateException: https://nvd.nist.gov/feeds/json/cve/2.0/nvdcve-2.0-2024.json.gz - Server status: 404"',
      '    echo "[ERROR] One or more dependencies were identified with vulnerabilities above the CVSS threshold."',
      '    exit 1 ;;',
      '  vulnerability:*)',
      '    echo "[ERROR] One or more dependencies were identified with vulnerabilities above the CVSS threshold."',
      '    exit 1 ;;',
      '  unavailable:*|recover:1)',
      '    echo "[ERROR] UpdateException: NVD download failed: https://nvd.nist.gov/feeds/json/cve/2.0/nvdcve-2.0-2024.json.gz - Server status: 404"',
      '    exit 1 ;;',
      '  *) echo "[INFO] BUILD SUCCESS"; exit 0 ;;',
      'esac',
    ].join('\n')
    writeFileSync(path.join(fixture, 'backend/mvnw'), stub, { mode: 0o755 })
    writeFileSync(path.join(fixture, 'bin/sleep'), '#!/usr/bin/env bash\nexit 0\n', { mode: 0o755 })
    const result = spawnSync('bash', ['-c', workflowCommand()], {
      cwd: path.join(fixture, 'backend'),
      encoding: 'utf8',
      timeout: 15_000,
      env: { PATH: path.join(fixture, 'bin') + path.delimiter + process.env.PATH, AUDIT_SCENARIO: scenario, NVD_API_KEY: '' },
    })
    assert.equal(result.error, undefined, 'O comando do workflow deve executar na fixture')
    const attempts = Number(readFileSync(path.join(fixture, 'backend/.audit-count'), 'utf8'))
    return { status: result.status, attempts }
  } finally {
    rmSync(fixture, { recursive: true, force: true })
  }
}

test('repete uma falha transitória NVD e exige sucesso da auditoria completa', () => {
  assert.deepEqual(simulatedAudit('recover'), { status: 0, attempts: 2 })
})

test('uma vulnerabilidade reprova a execução sem repetir ou mascarar o resultado', () => {
  assert.deepEqual(simulatedAudit('vulnerability'), { status: 1, attempts: 1 })
})

test('indisponibilidade persistente continua reprovada após no máximo três tentativas', () => {
  assert.deepEqual(simulatedAudit('unavailable'), { status: 1, attempts: 3 })
})

test('uma auditoria aprovada executa apenas uma vez', () => {
  assert.deepEqual(simulatedAudit('success'), { status: 0, attempts: 1 })
})

test('um resultado com vulnerabilidade e falha NVD continua bloqueado sem repetição', () => {
  assert.deepEqual(simulatedAudit('mixed'), { status: 1, attempts: 1 })
})
