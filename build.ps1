# Builds build\TowerRogue.jar with nothing but a JDK (21 or newer).
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (Test-Path build) { Remove-Item -Recurse -Force build }
New-Item -ItemType Directory -Force build\classes | Out-Null
Get-ChildItem -Recurse src\main\java -Filter *.java |
    ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' } |
    Set-Content -Encoding ascii build\sources.txt
javac --release 21 -encoding UTF-8 -d build\classes '@build\sources.txt'
if ($LASTEXITCODE -ne 0) { throw "javac failed" }
Copy-Item -Recurse src\main\resources\* build\classes\
if (-not (Get-Command jar -ErrorAction SilentlyContinue)) {
    Write-Host "Compiled to build\classes ('jar' tool not on PATH, skipping the jar)."
    Write-Host "Play: java -cp build\classes io.github.lucasstml.towerrogue.Main"
    exit 0
}
jar --create --file build\TowerRogue.jar --main-class io.github.lucasstml.towerrogue.Main -C build\classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

Write-Host "Built build\TowerRogue.jar"
Write-Host "Play:                java -jar build\TowerRogue.jar"
Write-Host "Determinism check:   java -jar build\TowerRogue.jar --verify-determinism"
Write-Host "Balance report:      java -jar build\TowerRogue.jar --balance-report 20"
