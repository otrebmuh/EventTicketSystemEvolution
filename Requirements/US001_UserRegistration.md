# User Story: User Registration (US001)

**As a** potential user of the ticket booking system  
**I want to** create a new account  
**So that** I can access personalized features and make ticket purchases

## Acceptance Criteria

1. User can access the registration form from the homepage
2. Registration form includes:
   - Email address
   - Password
   - First name
   - Last name
   - Date of birth
3. System validates all input fields
4. System checks for existing email addresses
5. Upon successful registration:
   - System sends a verification email
   - User is redirected to a "Please verify your email" page
   - User cannot log in until email is verified 
6. Password must meet the following security requirements:
   - Minimum length of 12 characters
   - Must contain at least:
     - One uppercase letter
     - One lowercase letter
     - One number
     - One special character
   - Cannot be one of the user's last 5 passwords