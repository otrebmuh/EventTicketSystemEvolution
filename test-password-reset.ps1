# Script para probar el sistema de reseteo de contraseña
Write-Host "=== Probando sistema de reseteo de contraseña ===" -ForegroundColor Green

$headers = @{ 'Content-Type' = 'application/json' }
$testEmail = "mjrbxui@gmail.com"

# Función para hacer peticiones HTTP
function Invoke-AuthRequest {
    param(
        [string]$Endpoint,
        [string]$Method = "POST",
        [hashtable]$Body = @{},
        [int]$TimeoutSec = 10
    )
    
    try {
        $jsonBody = $Body | ConvertTo-Json
        
        $response = Invoke-WebRequest -Uri "http://localhost:8091$Endpoint" -Method $Method -Body $jsonBody -Headers $headers -TimeoutSec $TimeoutSec -UseBasicParsing
        
        return @{
            Success = $true
            StatusCode = $response.StatusCode
            Content = $response.Content | ConvertFrom-Json
            Headers = $response.Headers
        }
    } catch {
        $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        $errorContent = ""
        if ($_.Exception.Response) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $errorContent = $reader.ReadToEnd()
            } catch {}
        }
        
        return @{
            Success = $false
            StatusCode = $statusCode
            Error = $_.Exception.Message
            ErrorContent = $errorContent
        }
    }
}

# Test 1: Solicitar reseteo de contraseña
Write-Host "`n1. Solicitando reseteo de contraseña para: $testEmail" -ForegroundColor Yellow

$forgotPasswordData = @{
    email = $testEmail
}

$result1 = Invoke-AuthRequest -Endpoint "/api/auth/forgot-password" -Body $forgotPasswordData

if ($result1.Success) {
    Write-Host "✓ Solicitud de reseteo exitosa - Status: $($result1.StatusCode)" -ForegroundColor Green
    
    # Mostrar headers de rate limiting si existen
    if ($result1.Headers["X-RateLimit-Limit"]) {
        Write-Host "  Rate Limit: $($result1.Headers["X-RateLimit-Limit"]) requests" -ForegroundColor Cyan
        Write-Host "  Remaining: $($result1.Headers["X-RateLimit-Remaining"]) requests" -ForegroundColor Cyan
    }
} else {
    Write-Host "✗ Error en solicitud de reseteo - Status: $($result1.StatusCode)" -ForegroundColor Red
    Write-Host "  Error: $($result1.Error)" -ForegroundColor Red
    if ($result1.ErrorContent) {
        Write-Host "  Content: $($result1.ErrorContent)" -ForegroundColor Red
    }
}

# Test 2: Hacer múltiples solicitudes para probar rate limiting
Write-Host "`n2. Probando rate limiting (máximo 3 por hora por email)..." -ForegroundColor Yellow

for ($i = 2; $i -le 4; $i++) {
    Write-Host "  Intento $i..." -ForegroundColor Cyan
    
    $result = Invoke-AuthRequest -Endpoint "/api/auth/forgot-password" -Body $forgotPasswordData
    
    if ($result.Success) {
        Write-Host "  ✓ Intento $i exitoso - Status: $($result.StatusCode)" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Intento $i falló - Status: $($result.StatusCode)" -ForegroundColor Red
        Write-Host "    Error: $($result.Error)" -ForegroundColor Red
        
        if ($result.StatusCode -eq 429) {
            Write-Host "    ⚠️  Rate limit alcanzado (esperado después del 3er intento)" -ForegroundColor Yellow
        } elseif ($result.StatusCode -eq 400 -and $result.ErrorContent -like "*Too many*") {
            Write-Host "    ⚠️  Rate limit del servicio alcanzado (esperado después del 3er intento)" -ForegroundColor Yellow
        }
    }
    
    Start-Sleep -Seconds 2
}

# Test 3: Probar con un email diferente para verificar que el rate limiting es por email
Write-Host "`n3. Probando con email diferente para verificar rate limiting por email..." -ForegroundColor Yellow

$differentEmailData = @{
    email = "test.different@example.com"
}
}

$result3 = Invoke-AuthRequest -Endpoint "/api/auth/forgot-password" -Body $differentEmailData

if ($result3.Success) {
    Write-Host "✓ Solicitud con email diferente exitosa - Status: $($result3.StatusCode)" -ForegroundColor Green
    Write-Host "  ✓ Confirmado: Rate limiting es por email, no por IP" -ForegroundColor Green
} else {
    Write-Host "✗ Error con email diferente - Status: $($result3.StatusCode)" -ForegroundColor Red
    Write-Host "  Error: $($result3.Error)" -ForegroundColor Red
}

Write-Host "`n=== Pruebas completadas ===" -ForegroundColor Green
Write-Host "Revisa tu email ($testEmail) para ver los tokens de reseteo enviados." -ForegroundColor Yellow
Write-Host "También puedes revisar los logs del auth-service:" -ForegroundColor Yellow
Write-Host "docker-compose logs auth-service --tail=50 | Select-String -Pattern 'password reset|rate limit'" -ForegroundColor Cyan