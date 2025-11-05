# User Story: User Login (US003)

**As a** registered user  
**I want to** log in to my account  
**So that** I can access my tickets and account features

## Acceptance Criteria

1. User can access the login form from any page
2. Login form includes:
   - Email address
   - Password
   - "Remember me" option
3. System validates credentials
4. After 3 failed attempts:
   - Account is temporarily locked
   - User receives an email notification
   - User must wait before trying again
5. Upon successful login:
   - User is redirected to their dashboard
   - If "Remember me" was selected, session persists 