import { spawnSync } from 'node:child_process'
import { randomUUID } from 'node:crypto'
import { readdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const container = `casacontas-ui-check-${randomUUID()}`
function docker(args) {
  const result = spawnSync('docker', args, { cwd: root, stdio: 'inherit', shell: false })
  if (result.error || result.status !== 0) throw new Error(`Docker: ${result.error?.message ?? result.status}`)
}
try {
  const args = process.argv.slice(2)
  docker(['create', '--name', container, '-w', '/workspace', '-e', 'NODE_OPTIONS=--max-old-space-size=1024',
    '-v', 'casacontas-npm:/root/.npm', 'node:22.20.0-bookworm-slim', 'sh', '-c',
    'npm ci && exec "$@"', '--', 'npm', ...(args.length ? args : ['run', 'verify'])])
  const excluded = new Set(['node_modules', 'dist', 'test-results', 'playwright-report', 'playwright-report-real', '.env', '.env.local'])
  for (const entry of readdirSync(path.join(root, 'frontend'))) {
    if (!excluded.has(entry) && !entry.startsWith('.env.')) docker(['cp', path.join(root, 'frontend', entry), `${container}:/workspace/${entry}`])
  }
  docker(['start', '-a', container])
} finally {
  docker(['rm', '-f', container])
}
