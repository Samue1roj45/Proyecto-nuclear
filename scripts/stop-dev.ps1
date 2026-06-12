# Detiene backend (8080) y frontend (4200).

$ErrorActionPreference = "SilentlyContinue"
$Root = Split-Path $PSScriptRoot -Parent
$PidFile = Join-Path $Root ".dev-pids.json"

function Stop-Port([int]$Port) {
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($conn in $conns) {
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
        Write-Host "Detenido proceso $($conn.OwningProcess) en puerto $Port" -ForegroundColor Yellow
    }
}

if (Test-Path $PidFile) {
    $saved = Get-Content $PidFile -Raw | ConvertFrom-Json
    foreach ($item in $saved.processes) {
        Stop-Process -Id $item.pid -Force -ErrorAction SilentlyContinue
    }
    Remove-Item $PidFile -Force
}

Stop-Port 8080
Stop-Port 4200

# Cierra procesos hijos de npm/node/java del proyecto si quedaron vivos
Get-CimInstance Win32_Process |
    Where-Object { $_.CommandLine -match 'simulador-1\.0\.0\.jar|Proyecto-nuclear\\frontend.*ng serve' } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }

Write-Host "Servicios detenidos." -ForegroundColor Green
