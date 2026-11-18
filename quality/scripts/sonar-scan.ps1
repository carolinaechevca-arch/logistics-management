# Compila y prueba shipment-service y notification-service (JaCoCo) y envía el análisis a SonarQube.
# Uso (desde la carpeta quality):  .\scripts\sonar-scan.ps1 -Token <TOKEN>
# Requiere Docker activo (las pruebas usan Testcontainers) y SonarQube en http://localhost:9000.
param([Parameter(Mandatory = $true)][string]$Token)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$services = @('shipment-service', 'notification-service')
$env:SONAR_TOKEN = $Token

foreach ($s in $services) {
  Write-Host "== Pruebas y cobertura de $s ==" -ForegroundColor Cyan
  Push-Location (Join-Path $root $s)
  try {
    .\gradlew.bat clean test jacocoTestReport --no-daemon
    if ($LASTEXITCODE -ne 0) { Write-Warning "Fallaron pruebas en $s; se continúa con el análisis." }

    Write-Host "== Análisis de $s en SonarQube ==" -ForegroundColor Cyan
    .\gradlew.bat sonar -x test -x jacocoTestReport --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Falló el análisis de SonarQube en $s" }
  } finally { Pop-Location }
}
Write-Host "Resultados: http://localhost:9000/projects" -ForegroundColor Green
