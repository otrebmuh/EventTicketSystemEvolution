# Frontend Testing Guide

This document describes the testing setup and how to run tests for the Event Ticket Booking System frontend.

## Testing Stack

- **Vitest**: Fast unit test framework for Vite projects
- **React Testing Library**: Testing utilities for React components
- **@testing-library/user-event**: User interaction simulation
- **@testing-library/jest-dom**: Custom matchers for DOM assertions
- **jsdom**: DOM implementation for Node.js

## Test Structure

```
frontend/src/
├── test/
│   ├── setup.ts                    # Global test setup and configuration
│   ├── test-utils.tsx              # Custom render utilities with Redux/Router
│   └── integration/
│       └── userFlow.test.tsx       # Integration tests for user flows
├── pages/
│   ├── LoginPage.test.tsx          # Login page component tests
│   └── RegisterPage.test.tsx       # Registration page component tests
├── services/
│   └── authService.test.ts         # Auth service unit tests
├── store/slices/
│   └── authSlice.test.ts           # Redux slice unit tests
└── utils/
    └── validation.test.ts          # Validation utility tests
```

## Running Tests

### Run all tests once
```bash
npm test
```

### Run tests in watch mode
```bash
npm run test:watch
```

### Run tests with UI
```bash
npm run test:ui
```

### Run tests with coverage
```bash
npm run test:coverage
```

## Test Coverage

The test suite covers:

### Unit Tests
- **Validation utilities** (19 tests)
  - Email validation
  - Password complexity validation
  - Required field validation
  - Date of birth validation
  - Password match validation

- **Auth service** (7 tests)
  - Registration API calls
  - Login API calls
  - Email verification
  - Password reset flows
  - Token refresh

- **Auth Redux slice** (10 tests)
  - State management for authentication
  - Async thunk handling
  - Error and success states
  - Token management

### Component Tests
- **LoginPage** (9 tests)
  - Form rendering
  - Validation errors
  - Form submission
  - Loading states
  - Error messages
  - Navigation links

- **RegisterPage** (9 tests)
  - Form rendering
  - Field validation
  - Password complexity
  - Password matching
  - Form submission
  - Success/error messages

### Integration Tests
- **User authentication flow** (7 tests)
  - Complete registration flow
  - Login with credentials
  - Remember me functionality
  - Form validation
  - Password complexity checks
  - Password matching

## Writing New Tests

### Component Tests

Use the custom `renderWithProviders` utility to render components with Redux store and Router:

```typescript
import { renderWithProviders } from '../test/test-utils';
import MyComponent from './MyComponent';

it('should render component', () => {
  renderWithProviders(<MyComponent />);
  expect(screen.getByText('Hello')).toBeInTheDocument();
});
```

### Testing with Preloaded State

```typescript
const preloadedState = {
  auth: {
    user: null,
    token: null,
    isAuthenticated: false,
    loading: false,
    error: 'Some error',
    successMessage: null,
  },
};

renderWithProviders(<MyComponent />, { preloadedState });
```

### User Interactions

```typescript
import userEvent from '@testing-library/user-event';

it('should handle user input', async () => {
  const user = userEvent.setup();
  renderWithProviders(<MyComponent />);
  
  await user.type(screen.getByLabelText(/email/i), 'test@example.com');
  await user.click(screen.getByRole('button', { name: /submit/i }));
  
  await waitFor(() => {
    expect(screen.getByText(/success/i)).toBeInTheDocument();
  });
});
```

### Mocking API Calls

```typescript
import { vi } from 'vitest';
import { authService } from './authService';

vi.mock('./authService');

it('should call API', async () => {
  vi.mocked(authService.login).mockResolvedValue({
    token: 'mock-token',
    user: { /* user data */ },
  });
  
  // Test code that calls authService.login
});
```

## Best Practices

1. **Focus on user behavior**: Test what users see and do, not implementation details
2. **Use semantic queries**: Prefer `getByRole`, `getByLabelText` over `getByTestId`
3. **Async operations**: Always use `waitFor` for async state changes
4. **Clean up**: Tests automatically clean up after each test via setup.ts
5. **Mock external dependencies**: Mock API calls and external services
6. **Test error states**: Include tests for error handling and edge cases

## Continuous Integration

Tests run automatically on:
- Pre-commit hooks (if configured)
- Pull request creation
- Merge to main branch

All tests must pass before code can be merged.

## Troubleshooting

### Tests timing out
- Increase timeout in `waitFor`: `waitFor(() => {...}, { timeout: 5000 })`
- Check for missing `await` keywords

### Element not found
- Use `screen.debug()` to see current DOM
- Check if element is rendered conditionally
- Verify query selectors match actual elements

### Mock not working
- Ensure mock is defined before importing component
- Clear mocks between tests with `vi.clearAllMocks()`
- Check mock implementation matches expected signature

## Future Enhancements

Potential areas for additional test coverage:
- Event browsing and search components
- Ticket purchase flow components
- Payment integration tests
- Ticket management components
- Error boundary tests
- Accessibility tests
- Visual regression tests
