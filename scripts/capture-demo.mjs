import { spawnSync } from 'node:child_process'
import { copyFileSync, mkdirSync, readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const result = spawnSync(process.execPath, [path.join(root, 'scripts/test-system.mjs')], {
  cwd: root,
  stdio: 'inherit',
  shell: false,
})
if (result.error || result.status !== 0) {
  throw new Error('A jornada real deve passar antes de atualizar as screenshots: ' +
    (result.error?.message ?? result.status))
}

function files(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(directory, entry.name)
    return entry.isDirectory() ? files(full) : [full]
  })
}

const screenshots = files(path.join(root, 'frontend/test-results'))
const destination = path.join(root, 'docs/screenshots')
mkdirSync(destination, { recursive: true })
for (const name of ['dashboard', 'expense']) {
  const matching = screenshots.filter((file) => path.basename(file) === 'demo-' + name + '.png')
  if (matching.length !== 1) throw new Error('Screenshot única ausente: ' + name)
  const png = readFileSync(matching[0])
  if (!png.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) {
    throw new Error('Arquivo de screenshot não é PNG: ' + name)
  }
  const width = png.readUInt32BE(16)
  const height = png.readUInt32BE(20)
  if (width < 1024 || height < 640) throw new Error('Screenshot incompleta: ' + name)
  copyFileSync(matching[0], path.join(destination, name + '.png'))
  console.log('Screenshot real atualizada: ' + name + ' (' + width + 'x' + height + ')')
}
