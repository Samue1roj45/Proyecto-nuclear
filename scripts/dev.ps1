# CLI unificado: start | stop | restart | status | build
param(
    [Parameter(Position = 0)]
    [ValidateSet("start", "stop", "restart", "status", "build")]
    [string]$Action = "start",

    [switch]$ForceBuild,
    [switch]$NoWait
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib.ps1")

$Root = Get-ProjectRoot
$Backend = Join-Path $Root "backend"
$Frontend = Join-Path $Root "frontend"
$Jar = Join-Path $Backend "target\simulador-1.0.0.jar"
$PidFile = Join-Path $Root ".dev-pids.json"
$LogDir = Join-Path $Root ".dev\logs"

function Show-Status {
    $backendUp = Test-PortListening 8080
    $frontendUp = Test-PortListening 4200
    $backendOk = if ($backendUp) { Test-BackendHealthy } else { $false }
    $frontendOk = if ($frontendUp) { Test-FrontendHealthy } else { $false }

    Write-Host "Estado del entorno:" -ForegroundColor Cyan
    Write-Host ("  Backend  (8080): " + $(if ($backendOk) { "OK" } elseif ($backendUp) { "puerto abierto, sin respuesta" } else { "detenido" }))
    Write-Host ("  Frontend (4200): " + $(if ($frontendOk) { "OK" } elseif ($frontendUp) { "puerto abierto, compilando..." } else { "detenido" }))
    Write-Host ("  JAR backend:     " + $(if (Test-Path $Jar) { "si" } else { "no (ejecuta: npm run build)" }))
    if (Test-Path $PidFile) {
        Write-Host "  PIDs guardados en .dev-pids.json"
    }
}

switch ($Action) {
    "status" {
        Show-Status
        exit 0
    }
    "stop" {
        & (Join-Path $PSScriptRoot "stop-dev.ps1")
        exit 0
    }
    "build" {
        & (Join-Path $PSScriptRoot "build-backend.ps1")
        exit 0
    }
    "restart" {
        & (Join-Path $PSScriptRoot "stop-dev.ps1")
        Start-Sleep -Seconds 2
        $Action = "start"
    }
}

if ($Action -eq "start") {
    $needsBuild = $ForceBuild -or (Test-BackendNeedsRebuild -Jar $Jar -BackendDir $Backend)
    if ($needsBuild) {
        if ($ForceBuild) {
            Write-Host "Recompilando backend (forzado)..." -ForegroundColor Cyan
        } else {
            Write-Host "Cambios en backend detectados. Recompilando..." -ForegroundColor Cyan
        }
        & (Join-Path $PSScriptRoot "build-backend.ps1")
    } elseif (-not (Test-Path $Jar)) {
        & (Join-Path $PSScriptRoot "build-backend.ps1")
    } else {
        Write-Host "Backend: usando JAR existente (sin recompilar)." -ForegroundColor DarkGray
    }

    $javaExe = Find-JavaExe
    if (-not $javaExe) {
        throw "No se encontró Java. Instala JDK 17+ o define JAVA_HOME."
    }

    if (-not (Test-Path (Join-Path $Frontend "node_modules"))) {
        Write-Host "Instalando dependencias del frontend..." -ForegroundColor Cyan
        Push-Location $Frontend
        npm install --no-fund --no-audit
        Pop-Location
    }

    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
    $network = Write-PublicNetworkConfig -Root $Root

    $state = @{
        backendPort = 8080
        frontendPort = 4200
        startedAt = (Get-Date).ToString("o")
        processes = @()
    }

    if (Test-PortListening 8080) {
        Write-Host "Backend ya en ejecución (8080)." -ForegroundColor Yellow
    } else {
        Write-Host "Iniciando backend..." -ForegroundColor Cyan
        $backendLog = Join-Path $LogDir "backend.log"
        $backendErrLog = Join-Path $LogDir "backend.err.log"
        $backendProc = Start-Process `
            -FilePath $javaExe `
            -ArgumentList @("-jar", $Jar) `
            -WorkingDirectory $Backend `
            -RedirectStandardOutput $backendLog `
            -RedirectStandardError $backendErrLog `
            -PassThru `
            -WindowStyle Hidden
        $state.processes += @{ role = "backend"; pid = $backendProc.Id }
    }

    if (Test-PortListening 4200) {
        Write-Host "Frontend ya en ejecución (4200)." -ForegroundColor Yellow
    } else {
        Write-Host "Iniciando frontend..." -ForegroundColor Cyan
        $frontendLog = Join-Path $LogDir "frontend.log"
        $frontendErrLog = Join-Path $LogDir "frontend.err.log"
        $frontendProc = Start-Process `
            -FilePath "cmd.exe" `
            -ArgumentList @("/c", "npm start") `
            -WorkingDirectory $Frontend `
            -RedirectStandardOutput $frontendLog `
            -RedirectStandardError $frontendErrLog `
            -PassThru `
            -WindowStyle Hidden
        $state.processes += @{ role = "frontend"; pid = $frontendProc.Id }
    }

    $state | ConvertTo-Json | Set-Content $PidFile -Encoding UTF8

    if (-not $NoWait) {
        Write-Host "Esperando servicios..." -ForegroundColor DarkGray
        Wait-ForService -Test { Test-BackendHealthy } -Name "Backend" -TimeoutSec 60 | Out-Null
        Wait-ForService -Test { Test-FrontendHealthy } -Name "Frontend" -TimeoutSec 120 | Out-Null
    }

    $infoUrl = if ($network) { $network.infoUrl } else { "" }
    Write-DevBanner -NetworkInfoUrl $infoUrl
    if ($infoUrl) {
        Write-Host "El QR usa esa URL para abrir la guia desde el celular (misma red WiFi)." -ForegroundColor DarkGray
        Write-Host ""
    }
    Write-Host "Comandos:" -ForegroundColor DarkGray
    Write-Host "  npm run dev:status   - ver estado"
    Write-Host "  npm run stop         - detener todo"
    Write-Host "  npm run restart      - reiniciar"
    Write-Host "  npm run build        - recompilar backend"
    Write-Host "  Logs: .dev/logs/"
}
