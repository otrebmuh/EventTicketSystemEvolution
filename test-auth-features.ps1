# Script para probar las funcionalidades de autenticación
# 1. Bloqueo de cuenta después de 3 intentos fallidos
# 2. Funcionalidad "Remember me"

Write-Host "=== Probando funcionalidades de autenticación ===" -ForegroundColor Green

# Función para hacer peticiones HTTP
function Invoke-AuthRequest {
    param(
        [string]$Endpoint,
        [string]$Method = "POST",
        [hashtable]$Body = @{},
        [int]$TimeoutSec = 10
    )
    
    try {
        $headers = @{
            'Content-Type' = 'application/json'
        }
        
        $jsonBody = $Body | ConvertTo-Json
        
        $response = Invoke-WebRequest -Uri "http://localhost:8091$Endpoint" -Method $Method -Body $jsonBody -Headers $headers -TimeoutSec $TimeoutSec
        
        return @{
            Success = $true
            StatusCode = $response.StatusCode
            Content = $response.Content | ConvertFrom-Json
        }
    } catch {
        $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        return @{
            Success = $false
            StatusCode = $statusCode
            Error = $_.Exception.Message
        }
    }
}

# Test 1: Registrar un usuario de prueba
Write-Host "`n1. Registrando usuario de prueba..." -ForegroundColor Yellow

$registerData = @{
    email = "test.lockout@example.com"
    password = "TestPassword123!"
    confirmPassword = "TestPassword123!"
    firstName = "Test"
    lastName = "Lockout"
    dateOfBirth = "1990-01-01"
}

$registerResult = Invoke-AuthRequest -Endpoint "/api/auth/register" -Body $registerData
if ($registerResult.Success) {
    Write-Host "✓ Usuario registrado exitosamente" -ForegroundColor Green
} else {
    Write-Host "✗ Error al registrar usuario: $($registerResult.Error)" -ForegroundColor Red
    if ($registerResult.StatusCode -eq 400) {
        Write-Host "  (Probablemente el usuario ya existe)" -ForegroundColor Yellow
    }
}

# Test 2: Probar bloqueo de cuenta con intentos fallidos
Write-Host "`n2. Probando bloqueo de cuenta (3 intentos fallidos)..." -ForegroundColor Yellow

for ($i = 1; $i -le 4; $i++) {
    Write-Host "  Intento $i con contraseña incorrecta..." -ForegroundColor Cyan
    
    $loginData = @{
        email = "test.lockout@example.com"
        password = "WrongPassword123!"
        rememberMe = $false
    }
    
    $loginResult = Invoke-AuthRequest -Endpoint "/api/auth/login" -Body $loginData
    
    if ($loginResult.Success) {
        Write-Host "  ✗ Login exitoso (no esperado)" -ForegroundColor Red
    } else {
        Write-Host "  ✓ Login falló como esperado (Status: $($loginResult.StatusCode))" -ForegroundColor Green
        if ($i -ge 3 -and $loginResult.Error -like "*locked*") {
            Write-Host "  ✓ Cuenta bloqueada detectada!" -ForegroundColor Green
        }
    }
    
    Start-Sleep -Seconds 1
}

# Test 3: Probar login con Remember Me
Write-Host "`n3. Probando funcionalidad 'Remember Me'..." -ForegroundColor Yellow

# Primero, registrar otro usuario para pruebas de Remember Me
$rememberMeUser = @{
    email = "test.rememberme@example.com"
    password = "TestPassword123!"
    confirmPassword = "TestPassword123!"
    firstName = "Test"
    lastName = "RememberMe"
    dateOfBirth = "1990-01-01"
}

$registerResult2 = Invoke-AuthRequest -Endpoint "/api/auth/register" -Body $rememberMeUser
if ($registerResult2.Success) {
    Write-Host "✓ Usuario Remember Me registrado exitosamente" -ForegroundColor Green
} else {
    Write-Host "✗ Error al registrar usuario Remember Me: $($registerResult2.Error)" -ForegroundColor Red
}

# Login sin Remember Me
Write-Host "  Probando login SIN Remember Me..." -ForegroundColor Cyan
$loginWithoutRemember = @{
    email = "test.rememberme@example.com"
    password = "TestPassword123!"
    rememberMe = $false
}

$loginResult1 = Invoke-AuthRequest -Endpoint "/api/auth/login" -Body $loginWithoutRemember
if ($loginResult1.Success) {
    Write-Host "  ✓ Login sin Remember Me exitoso" -ForegroundColor Green
    Write-Host "  Token expiration: $($loginResult1.Content.expiresIn) segundos" -ForegroundColor Cyan
} else {
    Write-Host "  ✗ Login sin Remember Me falló: $($loginResult1.Error)" -ForegroundColor Red
}

# Login con Remember Me
Write-Host "  Probando login CON Remember Me..." -ForegroundColor Cyan
$loginWithRemember = @{
    email = "test.rememberme@example.com"
    password = "TestPassword123!"
    rememberMe = $true
}

$loginResult2 = Invoke-AuthRequest -Endpoint "/api/auth/login" -Body $loginWithRemember
if ($loginResult2.Success) {
    Write-Host "  ✓ Login con Remember Me exitoso" -ForegroundColor Green
    Write-Host "  Token expiration: $($loginResult2.Content.expiresIn) segundos" -ForegroundColor Cyan
    
    # Comparar duraciones
    if ($loginResult1.Success -and $loginResult2.Success) {
        $normalExpiration = $loginResult1.Content.expiresIn
        $rememberExpiration = $loginResult2.Content.expiresIn
        
        if ($rememberExpiration -gt $normalExpiration) {
            Write-Host "  ✓ Remember Me extiende la duración del token correctamente" -ForegroundColor Green
            Write-Host "    Normal: $normalExpiration seg, Remember Me: $rememberExpiration seg" -ForegroundColor Cyan
        } else {
            Write-Host "  ✗ Remember Me no extiende la duración del token" -ForegroundColor Red
        }
    }
} else {
    Write-Host "  ✗ Login con Remember Me falló: $($loginResult2.Error)" -ForegroundColor Red
}

Write-Host "`n=== Pruebas completadas ===" -ForegroundColor Green
Write-Host "Revisa los logs del auth-service para ver los emails de bloqueo de cuenta:" -ForegroundColor Yellow
Write-Host "docker-compose logs auth-service | Select-String -Pattern 'account lock|Account lock'" -ForegroundColor Cyan