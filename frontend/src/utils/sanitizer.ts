/**
 * Input sanitization utilities for frontend
 */

/**
 * Sanitize HTML to prevent XSS attacks
 */
export function sanitizeHtml(input: string): string {
  if (!input) return input;
  
  const div = document.createElement('div');
  div.textContent = input;
  return div.innerHTML;
}

/**
 * Sanitize string by removing potentially dangerous characters
 */
export function sanitizeString(input: string): string {
  if (!input) return input;
  
  // Remove null bytes
  let sanitized = input.replace(/\0/g, '');
  
  // Trim whitespace
  sanitized = sanitized.trim();
  
  return sanitized;
}

/**
 * Validate email format
 */
export function isValidEmail(email: string): boolean {
  if (!email) return false;
  
  const emailPattern = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
  return emailPattern.test(email);
}

/**
 * Validate phone number format
 */
export function isValidPhone(phone: string): boolean {
  if (!phone) return false;
  
  const phonePattern = /^\+?[1-9]\d{1,14}$/;
  return phonePattern.test(phone);
}

/**
 * Check for potential XSS patterns
 */
export function containsXss(input: string): boolean {
  if (!input) return false;
  
  const xssPattern = /<script|<iframe|javascript:|onerror=|onload=|onclick=/i;
  return xssPattern.test(input);
}

/**
 * Check for potential SQL injection patterns
 */
export function containsSqlInjection(input: string): boolean {
  if (!input) return false;
  
  const sqlPattern = /('.+--)|(--)|(;)|(\|\|)|(\*)|(<script>)|(select|insert|update|delete|drop|create|alter|exec|execute)/i;
  return sqlPattern.test(input);
}

/**
 * Validate and sanitize input
 */
export function validateAndSanitize(input: string, fieldName: string): string {
  if (!input) return input;
  
  if (containsXss(input)) {
    throw new Error(`Invalid input for ${fieldName}: potential XSS attack detected`);
  }
  
  if (containsSqlInjection(input)) {
    throw new Error(`Invalid input for ${fieldName}: potential SQL injection detected`);
  }
  
  return sanitizeString(input);
}

/**
 * Sanitize URL to prevent open redirect vulnerabilities
 */
export function sanitizeUrl(url: string): string {
  if (!url) return url;
  
  // Only allow relative URLs or URLs from trusted domains
  if (url.startsWith('/')) {
    return url;
  }
  
  // Block javascript: and data: URLs
  if (url.toLowerCase().startsWith('javascript:') || 
      url.toLowerCase().startsWith('data:')) {
    throw new Error('Invalid URL: potentially dangerous protocol');
  }
  
  return url;
}

/**
 * Validate string length
 */
export function isValidLength(input: string, minLength: number, maxLength: number): boolean {
  if (!input) return false;
  
  const length = input.length;
  return length >= minLength && length <= maxLength;
}

/**
 * Escape special characters for use in regular expressions
 */
export function escapeRegex(input: string): string {
  return input.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
