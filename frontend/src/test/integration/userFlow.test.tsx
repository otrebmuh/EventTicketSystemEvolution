import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../test-utils';
import RegisterPage from '../../pages/RegisterPage';
import LoginPage from '../../pages/LoginPage';
import { authService } from '../../services/authService';

vi.mock('../../services/authService');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({ state: null }),
  };
});

describe('User Authentication Flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('Registration to Login Flow', () => {
    it('should complete registration successfully', async () => {
      const user = userEvent.setup();
      
      // Mock successful registration
      vi.mocked(authService.register).mockResolvedValue({
        message: 'Registration successful! Please check your email to verify your account.',
      });

      renderWithProviders(<RegisterPage />);

      // Fill out registration form
      await user.type(screen.getByLabelText(/first name/i), 'John');
      await user.type(screen.getByLabelText(/last name/i), 'Doe');
      await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
      await user.type(screen.getByLabelText(/date of birth/i), '1990-01-01');
      await user.type(screen.getByLabelText(/^password$/i), 'ValidPass123!');
      await user.type(screen.getByLabelText(/confirm password/i), 'ValidPass123!');

      // Submit form
      const submitButton = screen.getByRole('button', { name: /register/i });
      await user.click(submitButton);

      // Verify API was called with correct data
      await waitFor(() => {
        expect(authService.register).toHaveBeenCalledWith({
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@example.com',
          dateOfBirth: '1990-01-01',
          password: 'ValidPass123!',
        });
      });
    });
  });

  describe('Login Flow', () => {
    it('should complete login successfully', async () => {
      const user = userEvent.setup();
      
      // Mock successful login
      const mockLoginResponse = {
        token: 'mock-jwt-token',
        user: {
          id: '1',
          email: 'john@example.com',
          firstName: 'John',
          lastName: 'Doe',
          emailVerified: true,
        },
      };
      vi.mocked(authService.login).mockResolvedValue(mockLoginResponse);

      renderWithProviders(<LoginPage />);

      // Fill out login form
      await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
      await user.type(screen.getByLabelText(/password/i), 'ValidPass123!');

      // Submit form
      await user.click(screen.getByRole('button', { name: /login/i }));

      // Verify API was called
      await waitFor(() => {
        expect(authService.login).toHaveBeenCalledWith({
          email: 'john@example.com',
          password: 'ValidPass123!',
          rememberMe: false,
        });
      });
    });

    it('should handle login with remember me', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.login).mockResolvedValue({
        token: 'mock-jwt-token',
        user: {
          id: '1',
          email: 'john@example.com',
          firstName: 'John',
          lastName: 'Doe',
          emailVerified: true,
        },
      });

      renderWithProviders(<LoginPage />);

      // Fill out form and check remember me
      await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
      await user.type(screen.getByLabelText(/password/i), 'ValidPass123!');
      await user.click(screen.getByRole('checkbox', { name: /remember me/i }));

      // Submit form
      await user.click(screen.getByRole('button', { name: /login/i }));

      // Verify remember me was included
      await waitFor(() => {
        expect(authService.login).toHaveBeenCalledWith({
          email: 'john@example.com',
          password: 'ValidPass123!',
          rememberMe: true,
        });
      });
    });

    it('should call login API with credentials', async () => {
      const user = userEvent.setup();
      
      // Mock login
      vi.mocked(authService.login).mockResolvedValue({
        token: 'mock-jwt-token',
        user: {
          id: '1',
          email: 'test@example.com',
          firstName: 'Test',
          lastName: 'User',
          emailVerified: true,
        },
      });

      renderWithProviders(<LoginPage />);

      // Fill out form
      await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
      await user.type(screen.getByLabelText(/password/i), 'ValidPass123!');

      // Submit form
      await user.click(screen.getByRole('button', { name: /login/i }));

      // Verify API was called
      await waitFor(() => {
        expect(authService.login).toHaveBeenCalledWith({
          email: 'test@example.com',
          password: 'ValidPass123!',
          rememberMe: false,
        });
      });
    });
  });

  describe('Form Validation', () => {
    it('should prevent submission with invalid data', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterPage />);

      // Try to submit empty form
      await user.click(screen.getByRole('button', { name: /register/i }));

      // Should show validation errors
      await waitFor(() => {
        expect(screen.getByText(/first name is required/i)).toBeInTheDocument();
        expect(screen.getByText(/last name is required/i)).toBeInTheDocument();
        expect(screen.getByText(/email is required/i)).toBeInTheDocument();
      });

      // API should not be called
      expect(authService.register).not.toHaveBeenCalled();
    });

    it('should validate password complexity', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterPage />);

      // Enter weak password
      const passwordInput = screen.getByLabelText(/^password$/i);
      await user.type(passwordInput, 'weak');
      await user.tab();

      // Should show password requirements error (using getAllByText since there are multiple matches)
      await waitFor(() => {
        const errors = screen.getAllByText(/at least 12 characters/i);
        expect(errors.length).toBeGreaterThan(0);
      });
    });

    it('should validate password match', async () => {
      const user = userEvent.setup();
      renderWithProviders(<RegisterPage />);

      // Enter non-matching passwords
      await user.type(screen.getByLabelText(/^password$/i), 'ValidPass123!');
      await user.type(screen.getByLabelText(/confirm password/i), 'DifferentPass123!');
      await user.tab();

      // Should show password mismatch error
      await waitFor(() => {
        expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
      });
    });
  });
});
