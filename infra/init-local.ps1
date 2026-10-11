$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envPath) {
    Write-Output '.env already exists; preserved without changes.'
    exit 0
}
function New-LocalSecret {
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return 'Bgt' + ([BitConverter]::ToString($bytes).Replace('-', ''))
}
$content = @(
    "ORACLE_PASSWORD=$(New-LocalSecret)"
    'APP_USER=BUDGET_APP'
    "APP_USER_PASSWORD=$(New-LocalSecret)"
    'ORACLE_PORT=11521'
    'APP_PORT=8080'
    'DB_URL=jdbc:oracle:thin:@localhost:11521/FREEPDB1'
)
[IO.File]::WriteAllLines($envPath, $content, (New-Object Text.UTF8Encoding($false)))
Write-Output 'Created local .env with random credentials. Values are not printed.'
