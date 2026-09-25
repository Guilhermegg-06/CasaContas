param([string[]]$Goals = @('verify'), [string]$Evidence = 'backend', [switch]$Format)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$containerName = 'casacontas-check-' + [Guid]::NewGuid().ToString('N').Substring(0, 10)
$outputDir = Join-Path $repoRoot ('.data/evidence/' + $Evidence)
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
function Invoke-Docker([string[]]$Arguments) {
  & docker @Arguments
  if ($LASTEXITCODE -ne 0) { throw "Docker failed ($LASTEXITCODE)" }
}
$exitCode = 1
try {
  Invoke-Docker (@('create', '--name', $containerName, '-w', '/workspace',
    '-v', 'casacontas-m2:/root/.m2', '-v', '/var/run/docker.sock:/var/run/docker.sock',
    '-e', 'TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal',
    '-e', 'JAVA_TOOL_OPTIONS=-Xmx512m -XX:ActiveProcessorCount=2',
    'eclipse-temurin:21-jdk', './mvnw', '-B', '-ntp') + $Goals)
  foreach ($item in @('src', '.mvn', 'mvnw', 'pom.xml', 'config')) {
    Invoke-Docker @('cp', (Join-Path $repoRoot "backend/$item"), "${containerName}:/workspace/$item")
  }
  # Windows PowerShell trata stderr de processos nativos como ErrorRecord.
  # Avisos do Java não determinam o resultado: o código de saída do Maven determina.
  $ErrorActionPreference = 'Continue'
  & docker start -a $containerName
  $buildExit = $LASTEXITCODE
  $ErrorActionPreference = 'Stop'
  if ($Format -and $buildExit -eq 0) {
    Invoke-Docker @('cp', "${containerName}:/workspace/src/.", (Join-Path $repoRoot 'backend/src'))
    Invoke-Docker @('cp', "${containerName}:/workspace/pom.xml", (Join-Path $repoRoot 'backend/pom.xml'))
  }
  & docker cp "${containerName}:/workspace/target/." $outputDir 2>$null
  $exitCode = $buildExit
} catch {
  Write-Error -ErrorAction Continue $_
} finally {
  & docker rm -f $containerName | Out-Null
}
exit $exitCode
