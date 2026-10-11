param([switch]$Build, [switch]$Test)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkPath = 'C:\Program Files\Java\jdk-17'
if (Test-Path -LiteralPath (Join-Path $jdkPath 'bin\java.exe')) { $env:JAVA_HOME = $jdkPath }
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK 17 installation.' }
$envPath = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $envPath)) { throw 'Run infra/init-local.ps1 first.' }
Get-Content -LiteralPath $envPath | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process') }
}
Push-Location $projectRoot
try {
    if ($Test) {
        & .\gradlew.bat test --console=plain
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    if ($Build) {
        & .\gradlew.bat bootWar --console=plain
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    & (Join-Path $env:JAVA_HOME 'bin\java.exe') -jar build/libs/budget.war
    exit $LASTEXITCODE
} finally { Pop-Location }
