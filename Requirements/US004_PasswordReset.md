# User Story: Password Reset (US004)

**As a** registered user who forgot their password  
**I want to** reset my password  
**So that** I can regain access to my account

## Acceptance Criteria

1. User can request a password reset via "Forgot Password" link
2. Password reset email includes a time-limited link (expires in 15 minutes)
3. New password must meet the following security requirements:
   - Minimum length of 12 characters
   - Must contain at least:
     - One uppercase letter
     - One lowercase letter
     - One number
     - One special character
   - Cannot be one of the user's last 5 passwords
4. User is logged out of all devices after password change
5. User receives an email confirmation of password change
6. System logs all password reset attempts (successful and failed)
7. System implements rate limiting:
   - Maximum 3 password reset requests per hour per email
   - Maximum 5 password reset attempts per day per IP address
8. Password reset link can only be used once 