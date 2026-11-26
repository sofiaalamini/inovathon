# Sobe o PanoCerto para o dia da apresentacao, com link publico para a banca testar no celular.
#   powershell -ExecutionPolicy Bypass -File .\evento.ps1            (com link publico)
#   powershell -ExecutionPolicy Bypass -File .\evento.ps1 -SemTunel  (so neste computador)
# Ctrl+C desliga tudo. (Arquivo sem acentos de proposito: o PowerShell 5.1 os estraga.)
param([switch]$SemTunel)

$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$logs = Join-Path $env:TEMP 'panocerto-evento'
New-Item -ItemType Directory -Force $logs | Out-Null

foreach ($porta in 8080, 4173) {
    if (Get-NetTCPConnection -LocalPort $porta -State Listen -ErrorAction SilentlyContinue) {
        throw "A porta $porta ja esta em uso. Feche o outro servidor e rode de novo."
    }
}

function New-Segredo([int]$tamanho) {
    $letras = 'abcdefghijkmnpqrstuvwxyz23456789'.ToCharArray()
    $bytes = New-Object byte[] $tamanho
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    -join ($bytes | ForEach-Object { $letras[$_ % $letras.Length] })
}

# Valem so para esta execucao
$env:MODO_BANCA = 'true'
$env:JWT_SECRET = New-Segredo 48
$env:SENHA_PAINEL = New-Segredo 8
$env:ORIGENS_PERMITIDAS = 'http://localhost:*,https://*.trycloudflare.com'
# Banco separado para o evento: nao mexe nos dados de desenvolvimento
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:file:./dados/evento;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH'

Write-Host 'Compilando o backend...'
Push-Location "$raiz\backend"
.\mvnw.cmd -q package -DskipTests
if ($LASTEXITCODE -ne 0) { Pop-Location; throw 'Falhou o build do backend.' }
Pop-Location

Write-Host 'Compilando o painel...'
Push-Location "$raiz\frontend"
npm run build
if ($LASTEXITCODE -ne 0) { Pop-Location; throw 'Falhou o build do painel.' }
Pop-Location

$processos = @()
try {
    $jar = Get-ChildItem "$raiz\backend\target\panocerto-*.jar" | Select-Object -First 1
    $processos += Start-Process java -ArgumentList '-jar', "`"$($jar.FullName)`"", '--spring.profiles.active=dev' `
        -WorkingDirectory "$raiz\backend" -PassThru -NoNewWindow `
        -RedirectStandardOutput "$logs\backend.log" -RedirectStandardError "$logs\backend-erro.log"
    $processos += Start-Process node -ArgumentList 'node_modules\vite\bin\vite.js', 'preview', '--port', '4173', '--strictPort' `
        -WorkingDirectory "$raiz\frontend" -PassThru -NoNewWindow `
        -RedirectStandardOutput "$logs\painel.log" -RedirectStandardError "$logs\painel-erro.log"

    Write-Host 'Ligando o servidor...'
    $pronto = $false
    foreach ($i in 1..60) {
        Start-Sleep -Seconds 2
        try { Invoke-RestMethod 'http://localhost:4173/api/banca' | Out-Null; $pronto = $true; break } catch {}
    }
    if (-not $pronto) { throw "O servidor nao respondeu. Veja $logs\backend.log" }

    $endereco = 'http://localhost:4173'
    if (-not $SemTunel) {
        Write-Host 'Abrindo o link publico...'
        $logTunel = "$logs\tunel.log"
        $processos += Start-Process cloudflared -ArgumentList 'tunnel', '--url', 'http://localhost:4173' `
            -PassThru -NoNewWindow -RedirectStandardOutput "$logs\tunel-saida.log" -RedirectStandardError $logTunel
        $publico = $null
        foreach ($i in 1..30) {
            Start-Sleep -Seconds 2
            $achado = Select-String -Path $logTunel -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' -ErrorAction SilentlyContinue |
                Select-Object -First 1
            if ($achado) { $publico = $achado.Matches[0].Value; break }
        }
        if (-not $publico) { throw "O link publico nao saiu. Veja $logTunel" }
        $endereco = $publico
    }

    Write-Host ''
    Write-Host "QR code para a banca : $endereco/convite"
    Write-Host "Pagina de teste      : $endereco/testar"
    Write-Host "Painel do tecnico    : $endereco"
    Write-Host "  e-mail: tecnico@panocerto.com   senha de hoje: $($env:SENHA_PAINEL)"
    Write-Host "Logs em $logs"
    Write-Host 'Deixe esta janela aberta. Ctrl+C desliga.'
    while ($true) {
        Start-Sleep -Seconds 5
        if ($processos | Where-Object { $_.HasExited }) { throw "Um dos servidores caiu. Veja os logs em $logs" }
    }
}
finally {
    $processos | Where-Object { $_ -and -not $_.HasExited } | ForEach-Object { Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue }
}
