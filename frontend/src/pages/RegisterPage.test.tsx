import { describe, it, expect, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../test/test-utils';
import RegisterPage from './RegisterPage';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('RegisterPage', () => {
  it('should render registration form', () => {
    renderWithProviders(<RegisterPage />);
    
    expect(screen.getByRole('heading', { name: /create account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/date of birth/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
  });

  it('should show validation errors for empty fields', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);
    
    const submitButton = screen.getByRole('button', { name: /register/i });
    await user.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText(/first name is required/i)).toBeInTheDocument();
      expect(screen.getByText(/last name is required/i)).toBeInTheDocument();
      expect(screen.getByText(/email is required/i)).toBeInTheDocument();
    });
  });

  it('should show validation error for invalid email', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);
    
    const emailInput = screen.getByLabelText(/email address/i);
    await user.type(emailInput, 'invalid-email');
    await user.tab();
    
    await waitFor(() => {
      expect(screen.getByText(/please enter a valid email address/i)).toBeInTheDocument();
    });
  });

  it('should show validation error for weak password', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);
    
    const passwordInput = screen.getByLabelText(/^password$/i);
    await user.type(passwordInput, 'weak');
    await user.tab();
    
    await waitFor(() => {
      // Use getAllByText since there are multiple elements with this text
      const errors = screen.getAllByText(/at least 12 characters/i);
      expect(errors.length).toBeGreaterThan(0);
    });
  });

  it('should show validation error for non-matching passwords', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);
    
    const passwordInput = screen.getByLabelText(/^password$/i);
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i);
    
    await user.type(passwordInput, 'ValidPass123!');
    await user.type(confirmPasswordInput, 'DifferentPass123!');
    await user.tab();
    
    await waitFor(() => {
      expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
    });
  });

  it('should allow valid form submission', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);
    
    await user.type(screen.getByLabelText(/first name/i), 'John');
    await user.type(screen.getByLabelText(/last name/i), 'Doe');
    await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
    await user.type(screen.getByLabelText(/date of birth/i), '1990-01-01');
    await user.type(screen.getByLabelText(/^password$/i), 'ValidPass123!');
    await user.type(screen.getByLabelText(/confirm password/i), 'ValidPass123!');
    
    const submitButton = screen.getByRole('button', { name: /register/i });
    await user.click(submitButton);
    
    // Form should be submitted without validation errors
    expect(screen.queryByText(/is required/i)).not.toBeInTheDocument();
  });

  it('should display error message from API', () => {
    const preloadedState = {
      auth: {
        user: null,
        token: null,
        isAuthenticated: false,
        loading: false,
        error: 'Email already exists',
        successMessage: null,
      },
    };
    
    renderWithProviders(<RegisterPage />, { preloadedState });
    
    expect(screen.getByText(/email already exists/i)).toBeInTheDocument();
  });

  it('should display success message', () => {
    const preloadedState = {
      auth: {
        user: null,
        token: null,
        isAuthenticated: false,
        loading: false,
        error: null,
        successMessage: 'Registration successful! Please check your email.',
      },
    };
    
    renderWithProviders(<RegisterPage />, { preloadedState });
    
    expect(screen.getByText(/registration successful/i)).toBeInTheDocument();
  });

  it('should have link to login page', () => {
    renderWithProviders(<RegisterPage />);
    
    const loginLink = screen.getByRole('link', { name: /login here/i });
    expect(loginLink).toBeInTheDocument();
    expect(loginLink).toHaveAttribute('href', '/login');
  });
});
