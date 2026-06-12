# Compila el backend con Maven dentro de Docker (JDK 17).
# Solo necesario cuando cambias código Java o no existe el JAR.

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$Backend = Join-Path $Root "backend"
$Jar = Join-Path $Backend "target\simulador-1.0.0.jar"

Write-Host "Compilando backend (Docker + Maven)..." -ForegroundColor Cyan
docker run --rm `
  -v "${Backend}:/app" `
  -w /app `
  maven:3.9-eclipse-temurin-17 `
  mvn -q clean package -DskipTests

if (-not (Test-Path $Jar)) {
  throw "No se generó $Jar"
}

Write-Host "Listo: $Jar" -ForegroundColor Green
