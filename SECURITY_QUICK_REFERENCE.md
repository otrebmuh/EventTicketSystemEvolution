# Security Quick Reference Guide

## For Developers

### Backend: Using InputSanitizer

```java
@Service
public class MyService {
    @Autowired
    private InputSanitizer sanitizer;
    
    public void processUserInput(String email, String name, String url) {
        // Validate email
        if (!sanitizer.isValidEmail(email)) {
            throw new ValidationException("Invalid email");
        }
        
        // Sanitize and validate name
        String safeName = sanitizer.validateAndSanitize(name, "name");
        
        // Sanitize URL
        String safeUrl = sanitizer.sanitizeUrl(url);
        
        // Check for specific threats
        if (sanitizer.containsXss(input)) {
            throw new SecurityException("XSS detected");
        }
    }
}
```

### Frontend: Using Sanitizer

```typescript
import { 
    validateAndSanitize, 
    isValidEmail, 
    sanitizeHtml 
} from './utils/sanitizer';

function handleUserInput(email: string, name: string) {
    // Validate email
    if (!isValidEmail(email)) {
        throw new Error('Invalid email');
    }
    
    // Sanitize name
    const safeName = validateAndSanitize(name, 'name');
    
    // Sanitize HTML content
    const safeHtml = sanitizeHtml(userContent);
}
```

## Security Headers Reference

### Automatically Applied Headers

All responses include these security headers:

```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'; script-src 'self'; ...
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

### Rate Limit Headers

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
```

## Rate Limits

| Endpoint | Limit | Period |
|----------|-------|--------|
| Default | 100 | 1 minute |
| /auth/login | 5 | 1 minute |
| /auth/register | 5 | 1 minute |
| /auth/forgot-password | 3 | 1 hour |
| /auth/reset-password | 3 | 1 hour |
| /payment/* | 20 | 1 minute |

## Common Security Patterns

### 1. Validate All User Input

```java
// Always validate before processing
String safe = sanitizer.validateAndSanitize(userInput, "fieldName");
```

### 2. Use Parameterized Queries

```java
// Good - prevents SQL injection
@Query("SELECT u FROM User u WHERE u.email = :email")
User findByEmail(@Param("email") String email);

// Bad - vulnerable to SQL injection
// String query = "SELECT * FROM users WHERE email = '" + email + "'";
```

### 3. Escape Output

```java
// HTML escape before displaying
String safe = sanitizer.sanitizeHtml(userContent);
```

### 4. Validate File Uploads

```java
// Check file type and size
if (!allowedTypes.contains(file.getContentType())) {
    throw new ValidationException("Invalid file type");
}
if (file.getSize() > MAX_SIZE) {
    throw new ValidationException("File too large");
}
```

## Testing Security

### Test XSS Prevention

```bash
curl -X POST http://localhost:8080/api/test \
  -H "Content-Type: application/json" \
  -d '{"input":"<script>alert(\"XSS\")</script>"}'
```

Expected: Input should be sanitized or rejected

### Test Rate Limiting

```bash
# Send 10 rapid requests
for i in {1..10}; do
  curl http://localhost:8080/api/auth/login
done
```

Expected: Should receive 429 after limit exceeded

### Test Security Headers

```bash
curl -I http://localhost:8080/api/health
```

Expected: Should see security headers in response

## Production Checklist

- [ ] Enable HTTPS (uncomment SSL config)
- [ ] Set strong JWT_SECRET
- [ ] Set strong SERVICE_AUTH_SECRET
- [ ] Configure production CORS origins
- [ ] Remove development origins
- [ ] Set up SSL certificates
- [ ] Configure monitoring alerts
- [ ] Run security audit
- [ ] Test rate limiting
- [ ] Verify security headers

## Environment Variables

```bash
# Required for production
export SSL_KEY_STORE_PASSWORD=<strong-password>
export JWT_SECRET=<strong-secret-key>
export SERVICE_AUTH_SECRET=<strong-service-secret>
export ALLOWED_ORIGINS=https://app.eventbooking.com
```

## Common Issues

### Issue: CORS Error
**Solution**: Check CORS configuration in SecurityConfig.java and ensure origin is allowed

### Issue: Rate Limit Too Strict
**Solution**: Adjust limits in RateLimitingFilter.java for specific endpoints

### Issue: CSP Blocking Resources
**Solution**: Update Content-Security-Policy in SecurityConfig.java to allow required resources

### Issue: Input Rejected as Malicious
**Solution**: Review InputSanitizer patterns and adjust if legitimate input is being blocked

## Resources

- Full Documentation: `SECURITY_IMPLEMENTATION.md`
- Implementation Summary: `SECURITY_IMPLEMENTATION_SUMMARY.md`
- OWASP Top 10: https://owasp.org/www-project-top-ten/
- Spring Security: https://spring.io/projects/spring-security
