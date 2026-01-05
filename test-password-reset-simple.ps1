Write-Host "=== Probando sistema de reseteo de contraseña ===" -ForegroundColor Green

$headers = @{ 'Content-Type' = 'application/json' }
$testEmail = "mjrbxui@gmail.com"

Write-Host "`n1. Solicitando reseteo de contraseña para: $testEmail" -ForegroundColor Yellow

$forgotPasswordData = @{
    email = $testEmail
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8091/api/auth/forgot-password" -Method POST -Body $forgotPasswordData -Headers $headers -TimeoutSec 10 -UseBasicParsing
    Write-Host "✓ Solicitud de reseteo exitosa - Status: $($response.StatusCode)" -ForegroundColor Green
    
    if ($response.Headers["X-RateLimit-Limit"]) {
        Write-Host "  Rate Limit: $($response.Headers["X-RateLimit-Limit"]) requests" -ForegroundColor Cyan
        Write-Host "  Remaining: $($response.Headers["X-RateLimit-Remaining"]) requests" -ForegroundColor Cyan
    }
} catch {
    Write-Host "✗ Error en solicitud de reseteo - Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n2. Segundo intento..." -ForegroundColor Yellow

try {
    $response2 = Invoke-WebRequest -Uri "http://localhost:8091/api/auth/forgot-password" -Method POST -Body $forgotPasswordData -Headers $headers -TimeoutSec 10 -UseBasicParsing
    Write-Host "✓ Segundo intento exitoso - Status: $($response2.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "✗ Segundo intento falló - Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n3. Tercer intento..." -ForegroundColor Yellow

try {
    $response3 = Invoke-WebRequest -Uri "http://localhost:8091/api/auth/forgot-password" -Method POST -Body $forgotPasswordData -Headers $headers -TimeoutSec 10 -UseBasicParsing
    Write-Host "✓ Tercer intento exitoso - Status: $($response3.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "✗ Tercer intento falló - Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n4. Cuarto intento (debería fallar)..." -ForegroundColor Yellow

try {
    $response4 = Invoke-WebRequest -Uri "http://localhost:8091/api/auth/forgot-password" -Method POST -Body $forgotPasswordData -Headers $headers -TimeoutSec 10 -UseBasicParsing
    Write-Host "✗ Cuarto intento exitoso (no esperado) - Status: $($response4.StatusCode)" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "✓ Cuarto intento falló como esperado - Status: $statusCode" -ForegroundColor Green
    if ($statusCode -eq 429) {
        Write-Host "  ⚠️  Rate limit del filtro alcanzado" -ForegroundColor Yellow
    } elseif ($statusCode -eq 400) {
        Write-Host "  ⚠️  Rate limit del servicio alcanzado" -ForegroundColor Yellow
    }
}

Write-Host "`n=== Pruebas completadas ===" -ForegroundColor Green