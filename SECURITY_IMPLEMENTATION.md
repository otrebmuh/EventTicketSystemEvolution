# Security Implementation Guide

This document describes the comprehensive security measures implemented in the Event Ticket Booking System.

## Overview

The system implements multiple layers of security to protect against common web vulnerabilities and ensure compliance with security requirements (Requirements 9.1-9.5).

## 1. HTTPS Enforcement and Security Headers

### HTTPS Configuration

All services are configured to support HTTPS in production:

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEY_STORE_PASSWORD}
    key-store-type: PKCS12
```

### Security Headers

The following security headers are automatically added to all responses:

- **Strict-Transport-Security (HSTS)**: Enforces HTTPS connections
  - `max-age=31536000; includeSubDomains; preload`
  
- **X-Content-Type-Options**: Prevents MIME type sniffing
  - `nosniff`
  
- **X-Frame-Options**: Prevents clickjacking attacks
  - `DENY`
  
- **X-XSS-Protection**: Enables browser XSS filtering
  - `1; mode=block`
  
- **Content-Security-Policy**: Restricts resource loading
  - Prevents inline scripts and restricts external resources
  
- **Referrer-Policy**: Controls referrer information
  - `strict-origin-when-cross-origin`
  
- **Permissions-Policy**: Controls browser features
  - Disables geolocation, microphone, camera

### Implementation

Security headers are implemented in:
- `SecurityConfig.java` - Spring Security configuration
- `SecurityHeadersFilter.java` - Custom filter for additional headers

## 2. Input Validation and Sanitization

### Backend Validation

The `InputSanitizer` utility class provides comprehensive input validation:

```java
@Component
public class InputSanitizer {
    // HTML sanitization
    public String sanitizeHtml(String input);
    
    // String sanitization
    public String sanitizeString(String input);
    
    // Email validation
    public boolean isValidEmail(String email);
    
    // SQL injection detection
    public boolean containsSqlInjection(String input);
    
    // XSS detection
    public boolean containsXss(String input);
    
    // URL sanitization
    public String sanitizeUrl(String url);
}
```

### Frontend Validation

The `sanitizer.ts` utility provides client-side validation:

```typescript
// Sanitize HTML
sanitizeHtml(input: string): string

// Validate email
isValidEmail(email: string): boolean

// Check for XSS
containsXss(input: string): boolean

// Validate and sanitize
validateAndSanitize(input: string, fieldName: string): string
```

### Usage Example

```java
@Service
public class UserService {
    @Autowired
    private InputSanitizer sanitizer;
    
    public void createUser(String email, String name) {
        // Validate email
        if (!sanitizer.isValidEmail(email)) {
            throw new ValidationException("Invalid email format");
        }
        
        // Sanitize name
        String safeName = sanitizer.validateAndSanitize(name, "name");
        
        // Process user creation...
    }
}
```

## 3. Rate Limiting

### Implementation

Rate limiting is implemented using Resilience4j:

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    // Default: 100 requests per minute
    // Auth endpoints: 5 requests per minute
    // Password reset: 3 requests per hour
}
```

### Rate Limit Configurations

| Endpoint Type | Limit | Period |
|--------------|-------|--------|
| Default | 100 requests | 1 minute |
| Authentication | 5 requests | 1 minute |
| Password Reset | 3 requests | 1 hour |
| Payment | 20 requests | 1 minute |

### Response Headers

Rate limit information is included in response headers:
- `X-RateLimit-Limit`: Maximum requests allowed
- `X-RateLimit-Remaining`: Remaining requests in current period

### Rate Limit Exceeded Response

```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later.",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

## 4. CSRF Protection

### JWT-Based Protection

The system uses JWT tokens for authentication, which provides inherent CSRF protection:

1. **Token Storage**: JWT tokens are stored in localStorage (not cookies)
2. **Custom Headers**: All requests include `X-Requested-With: XMLHttpRequest`
3. **Origin Validation**: Server validates Origin and Referer headers

### CSRF Token Service

For additional protection, a CSRF token service is available:

```java
@Service
public class CsrfTokenService {
    public String generateToken(String sessionId);
    public boolean validateToken(String sessionId, String token);
}
```

### Frontend Implementation

```typescript
// All API requests include custom header
headers: {
  'X-Requested-With': 'XMLHttpRequest',
  'Authorization': `Bearer ${token}`
}
```

## 5. XSS Prevention

### Content Security Policy

Strict CSP headers prevent inline script execution:

```
Content-Security-Policy: default-src 'self'; 
  script-src 'self'; 
  style-src 'self' 'unsafe-inline'; 
  img-src 'self' data: https:;
```

### Input Sanitization

All user input is sanitized before:
- Storing in database
- Displaying in UI
- Including in API responses

### Output Encoding

- HTML entities are escaped
- JSON responses are properly encoded
- URL parameters are validated

## 6. Request Validation

### RequestValidationFilter

Validates all incoming requests:

```java
@Component
public class RequestValidationFilter extends OncePerRequestFilter {
    // Validates HTTP methods
    // Checks Content-Length
    // Validates Content-Type
    // Checks for suspicious URI patterns
    // Validates Origin header
}
```

### Validation Rules

- **HTTP Methods**: Only GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD allowed
- **Request Size**: Maximum 10MB
- **Content-Type**: Only application/json, multipart/form-data, etc.
- **URI Patterns**: Blocks path traversal, null bytes, script injection

## 7. CORS Configuration

### Allowed Origins

```java
configuration.setAllowedOriginPatterns(Arrays.asList(
    "http://localhost:*",
    "https://localhost:*",
    "https://*.eventbooking.com"
));
```

### Allowed Methods

- GET, POST, PUT, DELETE, PATCH, OPTIONS

### Allowed Headers

- Authorization, Content-Type, X-Requested-With, Accept, Origin

### Credentials

- Credentials (cookies, authorization headers) are allowed
- Preflight responses are cached for 1 hour

## 8. Session Management

### Stateless Authentication

- JWT-based authentication (no server-side sessions)
- Tokens expire after 24 hours
- Refresh tokens for extended sessions

### Redis Session Storage

For services requiring session state:
- Sessions stored in Redis
- Automatic expiration
- Distributed session management

## 9. Error Handling

### Secure Error Responses

Production configuration hides sensitive information:

```yaml
server:
  error:
    include-message: never
    include-binding-errors: never
    include-stacktrace: never
    include-exception: false
```

### Error Response Format

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "User-friendly message",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

## 10. Compliance

### PCI DSS Compliance

- Payment data never stored directly
- Stripe tokenization for card data
- TLS 1.2+ for all communications
- Regular security audits

### GDPR Compliance

- Data encryption at rest and in transit
- User consent management
- Right to deletion
- Data portability

## Security Checklist

- [x] HTTPS enforcement
- [x] Security headers (HSTS, CSP, X-Frame-Options, etc.)
- [x] Input validation and sanitization
- [x] Rate limiting on all endpoints
- [x] CSRF protection via JWT and custom headers
- [x] XSS prevention via CSP and output encoding
- [x] SQL injection prevention via parameterized queries
- [x] CORS configuration
- [x] Secure session management
- [x] Error message sanitization
- [x] Request size limits
- [x] Content-Type validation
- [x] Origin validation
- [x] Path traversal prevention

## Testing Security

### Manual Testing

1. **HTTPS**: Verify all production endpoints use HTTPS
2. **Headers**: Check security headers in browser DevTools
3. **Rate Limiting**: Test with multiple rapid requests
4. **Input Validation**: Try malicious inputs (XSS, SQL injection)
5. **CORS**: Test cross-origin requests

### Automated Testing

```bash
# Run security tests
mvn test -Dtest=SecurityTest

# Check for vulnerabilities
mvn dependency-check:check

# OWASP ZAP scanning
zap-cli quick-scan http://localhost:8080
```

## Production Deployment

### Pre-Deployment Checklist

1. Enable HTTPS with valid SSL certificates
2. Configure environment-specific CORS origins
3. Set strong JWT secrets
4. Enable rate limiting
5. Configure WAF rules
6. Set up monitoring and alerting
7. Review and update security headers
8. Perform security audit

### Environment Variables

```bash
# SSL Configuration
SSL_KEY_STORE_PASSWORD=<strong-password>

# JWT Configuration
JWT_SECRET=<strong-secret-key>

# Service Authentication
SERVICE_AUTH_SECRET=<strong-service-secret>

# CORS Origins
ALLOWED_ORIGINS=https://app.eventbooking.com
```

## Monitoring and Alerts

### Security Metrics

- Failed authentication attempts
- Rate limit violations
- Suspicious request patterns
- Invalid input attempts

### Alerting

Configure alerts for:
- High rate of 401/403 responses
- Rate limit exceeded events
- Suspicious URI patterns
- Large request payloads

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [PCI DSS Requirements](https://www.pcisecuritystandards.org/)
- [GDPR Compliance](https://gdpr.eu/)
