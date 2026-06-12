# Alias de arranque — usa dev.ps1 start
param(
    [switch]$Build,
    [switch]$Rebuild
)

$args = @("start")
if ($Build -or $Rebuild) { $args += "-ForceBuild" }
& (Join-Path $PSScriptRoot "dev.ps1") @args
