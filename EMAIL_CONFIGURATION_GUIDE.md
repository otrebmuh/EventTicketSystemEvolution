# Guía de Configuración de Email Real

Este documento explica cómo configurar el sistema para enviar emails REALES a las direcciones de correo de los usuarios.

## Estado Actual

- **Desarrollo Local**: El sistema usa MailHog (servidor SMTP de prueba local)
- **Producción**: Necesita configuración de servidor SMTP real

## Opciones de Configuración

### Opción 1: Gmail SMTP (Recomendado para desarrollo/pruebas)

#### Paso 1: Crear una Contraseña de Aplicación de Gmail

1. Ve a tu cuenta de Google: https://myaccount.google.com/
2. Navega a **Seguridad** → **Verificación en dos pasos** (debes habilitarla primero)
3. Busca **Contraseñas de aplicaciones**
4. Selecciona **Correo** y **Otro (nombre personalizado)**
5. Escribe "Event Booking System" y haz clic en **Generar**
6. Copia la contraseña de 16 caracteres generada

#### Paso 2: Configurar Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto con:

```bash
# Email Configuration (Gmail)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=tu-contraseña-de-aplicacion-de-16-caracteres
MAIL_FROM=tu-email@gmail.com
```

#### Paso 3: Actualizar docker-compose.yml

Agrega las variables de entorno al servicio auth-service:

```yaml
auth-service:
  environment:
    MAIL_HOST: smtp.gmail.com
    MAIL_PORT: 587
    MAIL_USERNAME: ${MAIL_USERNAME}
    MAIL_PASSWORD: ${MAIL_PASSWORD}
    MAIL_FROM: ${MAIL_FROM}
```

#### Paso 4: Reiniciar el Servicio

```bash
docker-compose up -d --build auth-service
```

### Opción 2: SendGrid (Recomendado para producción)

SendGrid ofrece 100 emails gratis por día.

#### Paso 1: Crear Cuenta en SendGrid

1. Regístrate en https://sendgrid.com/
2. Verifica tu email
3. Crea una API Key en **Settings** → **API Keys**

#### Paso 2: Configurar Variables de Entorno

```bash
# Email Configuration (SendGrid)
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=tu-sendgrid-api-key
MAIL_FROM=noreply@tudominio.com
```

#### Paso 3: Verificar Dominio (Opcional pero recomendado)

Para evitar que los emails caigan en spam:
1. Ve a **Settings** → **Sender Authentication**
2. Verifica tu dominio o email individual

### Opción 3: AWS SES (Para producción a escala)

AWS SES es muy económico para grandes volúmenes.

#### Paso 1: Configurar AWS SES

1. Ve a la consola de AWS SES
2. Verifica tu dominio o email
3. Solicita salir del "sandbox mode" para enviar a cualquier dirección

#### Paso 2: Crear Credenciales SMTP

1. En AWS SES, ve a **SMTP Settings**
2. Crea credenciales SMTP
3. Guarda el username y password

#### Paso 3: Configurar Variables de Entorno

```bash
# Email Configuration (AWS SES)
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=tu-aws-ses-smtp-username
MAIL_PASSWORD=tu-aws-ses-smtp-password
MAIL_FROM=noreply@tudominio.com
```

## Verificación

### 1. Verificar Configuración

```bash
# Ver logs del auth-service
docker logs auth-service --tail 50

# Buscar mensajes de conexión SMTP
docker logs auth-service 2>&1 | grep -i "smtp\|mail"
```

### 2. Probar Envío de Email

Registra un usuario con tu email real desde el frontend:
- URL: http://localhost:3001/register
- Usa tu email personal
- Verifica que recibas el email de verificación

### 3. Verificar en Logs

```bash
# Ver si el email se envió exitosamente
docker logs auth-service 2>&1 | grep "Email verification sent successfully"
```

## Solución de Problemas

### Error: "Authentication failed"

- Verifica que la contraseña de aplicación sea correcta
- Para Gmail, asegúrate de tener habilitada la verificación en dos pasos
- Verifica que el username sea el email completo

### Error: "Connection timeout"

- Verifica que el puerto sea correcto (587 para TLS, 465 para SSL)
- Verifica que no haya firewall bloqueando la conexión
- Intenta con otro servidor SMTP

### Emails caen en spam

- Verifica tu dominio con el proveedor de email (SendGrid, AWS SES)
- Configura registros SPF, DKIM y DMARC en tu DNS
- Usa un dominio verificado en lugar de Gmail

### Error: "Could not parse mail"

- Verifica que `MAIL_FROM` tenga un formato válido de email
- Asegúrate de que todas las variables de entorno estén configuradas

## Configuración Actual del Sistema

### Desarrollo (MailHog)
```yaml
MAIL_HOST: mailhog
MAIL_PORT: 1025
MAIL_USERNAME: (vacío)
MAIL_PASSWORD: (vacío)
```

### Producción (Gmail ejemplo)
```yaml
MAIL_HOST: smtp.gmail.com
MAIL_PORT: 587
MAIL_USERNAME: tu-email@gmail.com
MAIL_PASSWORD: tu-contraseña-de-aplicacion
```

## Cambiar entre MailHog y Email Real

### Usar MailHog (desarrollo local)
```bash
# En docker-compose.yml, usa:
MAIL_HOST: mailhog
MAIL_PORT: 1025
```

### Usar Email Real
```bash
# En docker-compose.yml, usa:
MAIL_HOST: smtp.gmail.com
MAIL_PORT: 587
MAIL_USERNAME: ${MAIL_USERNAME}
MAIL_PASSWORD: ${MAIL_PASSWORD}
```

## Recomendaciones

1. **Desarrollo**: Usa MailHog para no enviar emails reales durante pruebas
2. **Staging**: Usa Gmail SMTP con un email de prueba
3. **Producción**: Usa SendGrid o AWS SES para mejor deliverability

## Seguridad

⚠️ **IMPORTANTE**: 
- Nunca subas credenciales de email al repositorio
- Usa variables de entorno o secretos
- Rota las contraseñas regularmente
- Usa contraseñas de aplicación, no tu contraseña principal
