function Get-ProjectRoot {
    Split-Path $PSScriptRoot -Parent
}

function Find-JavaExe {
    $candidates = @()

    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME "bin\java.exe")
    }

    $regPaths = @(
        "HKLM:\SOFTWARE\JavaSoft\JDK",
        "HKLM:\SOFTWARE\Eclipse Adoptium\JDK"
    )
    foreach ($base in $regPaths) {
        if (Test-Path $base) {
            Get-ChildItem $base -ErrorAction SilentlyContinue | ForEach-Object {
                $javaHome = (Get-ItemProperty $_.PSPath -ErrorAction SilentlyContinue).JavaHome
                if (-not $javaHome) {
                    $javaHome = (Get-ItemProperty $_.PSPath -ErrorAction SilentlyContinue).Path
                }
                if ($javaHome) { $candidates += (Join-Path $javaHome "bin\java.exe") }
            }
        }
    }

    $candidates += @(
        "C:\Program Files\Java\jdk-24\bin\java.exe",
        "C:\Program Files\Java\jdk-17\bin\java.exe",
        "C:\Program Files\Eclipse Adoptium\jdk-17*\bin\java.exe"
    )

    foreach ($pattern in $candidates) {
        $resolved = Resolve-Path $pattern -ErrorAction SilentlyContinue
        if ($resolved) { return $resolved.Path }
    }

    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd -and (Test-Path $javaCmd.Source)) { return $javaCmd.Source }

    return $null
}

function Test-PortListening([int]$Port) {
    [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Test-BackendHealthy {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method Options -UseBasicParsing -TimeoutSec 3
        return $r.StatusCode -ge 200
    } catch {
        return $false
    }
}

function Test-FrontendHealthy {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:4200" -UseBasicParsing -TimeoutSec 3
        return $r.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Wait-ForService {
    param(
        [scriptblock]$Test,
        [string]$Name,
        [int]$TimeoutSec = 90
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (& $Test) {
            Write-Host "$Name listo." -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 2
    }
    Write-Host "$Name no respondió a tiempo." -ForegroundColor Yellow
    return $false
}

function Test-BackendNeedsRebuild {
    param([string]$Jar, [string]$BackendDir)
    if (-not (Test-Path $Jar)) { return $true }

    $jarTime = (Get-Item $Jar).LastWriteTimeUtc
    $sources = @(
        (Join-Path $BackendDir "pom.xml")
    )
    $sources += Get-ChildItem (Join-Path $BackendDir "src") -Recurse -File -ErrorAction SilentlyContinue

    foreach ($file in $sources) {
        if ($file.LastWriteTimeUtc -gt $jarTime) { return $true }
    }
    return $false
}

function Get-LanIPv4 {
    Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object {
            $_.IPAddress -notlike "127.*" -and
            $_.IPAddress -notlike "169.254.*" -and
            $_.PrefixOrigin -ne "WellKnown"
        } |
        Sort-Object -Property InterfaceMetric |
        Select-Object -First 1 -ExpandProperty IPAddress
}

function Write-PublicNetworkConfig {
    param(
        [string]$Root,
        [int]$Port = 4200
    )
    $ip = Get-LanIPv4
    if (-not $ip) { return $null }

    $baseUrl = "http://${ip}:${Port}"
    $payload = @{
        baseUrl = $baseUrl
        infoUrl = "$baseUrl/info"
        updatedAt = (Get-Date).ToString("o")
    } | ConvertTo-Json

    $outPath = Join-Path $Root "frontend\public\network-url.json"
    $payload | Set-Content $outPath -Encoding UTF8
    return $payload | ConvertFrom-Json
}

function Write-DevBanner {
    param([string]$NetworkInfoUrl = "")
    Write-Host ""
    Write-Host "Mision Psicosocial - entorno de desarrollo" -ForegroundColor Green
    Write-Host "  Frontend: http://localhost:4200"
    if ($NetworkInfoUrl) {
        Write-Host "  QR / otros dispositivos: $NetworkInfoUrl" -ForegroundColor Cyan
    }
    Write-Host "  Backend:  http://localhost:8080/api"
    Write-Host ""
    Write-Host "Usuarios de prueba:"
    Write-Host "  Admin:      admin@simulador.com / admin123"
    Write-Host "  Estudiante: estudiante@simulador.com / estudiante123"
    Write-Host ""
}
