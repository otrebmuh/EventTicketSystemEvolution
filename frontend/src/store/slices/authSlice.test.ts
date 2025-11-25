import { describe, it, expect, vi, beforeEach } from 'vitest';
import authReducer, {
  registerUser,
  loginUser,
  logoutUser,
  clearError,
  clearSuccessMessage,
  setToken,
} from './authSlice';


vi.mock('../../services/authService');

describe('authSlice', () => {
  const initialState = {
    user: null,
    token: null,
    isAuthenticated: false,
    loading: false,
    error: null,
    successMessage: null,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('reducers', () => {
    it('should handle clearError', () => {
      const stateWithError = { ...initialState, error: 'Some error' };
      const newState = authReducer(stateWithError, clearError());
      expect(newState.error).toBeNull();
    });

    it('should handle clearSuccessMessage', () => {
      const stateWithSuccess = { ...initialState, successMessage: 'Success!' };
      const newState = authReducer(stateWithSuccess, clearSuccessMessage());
      expect(newState.successMessage).toBeNull();
    });

    it('should handle setToken', () => {
      const token = 'test-token';
      const newState = authReducer(initialState, setToken(token));
      expect(newState.token).toBe(token);
      expect(newState.isAuthenticated).toBe(true);
    });
  });

  describe('registerUser', () => {
    it('should handle pending state', () => {
      const action = { type: registerUser.pending.type };
      const newState = authReducer(initialState, action);
      expect(newState.loading).toBe(true);
      expect(newState.error).toBeNull();
      expect(newState.successMessage).toBeNull();
    });

    it('should handle fulfilled state', () => {
      const message = 'Registration successful';
      const action = {
        type: registerUser.fulfilled.type,
        payload: { message },
      };
      const newState = authReducer(initialState, action);
      expect(newState.loading).toBe(false);
      expect(newState.successMessage).toBe(message);
    });

    it('should handle rejected state', () => {
      const error = 'Registration failed';
      const action = {
        type: registerUser.rejected.type,
        payload: error,
      };
      const newState = authReducer(initialState, action);
      expect(newState.loading).toBe(false);
      expect(newState.error).toBe(error);
    });
  });

  describe('loginUser', () => {
    it('should handle pending state', () => {
      const action = { type: loginUser.pending.type };
      const newState = authReducer(initialState, action);
      expect(newState.loading).toBe(true);
      expect(newState.error).toBeNull();
    });

    it('should handle fulfilled state', () => {
      const payload = {
        token: 'test-token',
        user: {
          id: '1',
          email: 'test@example.com',
          firstName: 'John',
          lastName: 'Doe',
          emailVerified: true,
        },
      };
      const action = {
        type: loginUser.fulfilled.type,
        payload,
      };
      const newState = authReducer(initialState, action);
      expect(newState.loading).toBe(false);
      expect(newState.isAuthenticated).toBe(true);
      expect(newState.user).toEqual(payload.user);
      expect(newState.token).toBe(payload.token);
    });

    it('should handle rejected state', () => {
      const error = 'Login failed';
      const action = {
        type: loginUser.rejected.type,
        payload: error,
      };
      const newState = authReducer(initialState, action);
      expect(newState.loading).toBe(false);
      expect(newState.error).toBe(error);
    });
  });

  describe('logoutUser', () => {
    it('should clear user data on logout', () => {
      const authenticatedState = {
        ...initialState,
        user: {
          id: '1',
          email: 'test@example.com',
          firstName: 'John',
          lastName: 'Doe',
          emailVerified: true,
        },
        token: 'test-token',
        isAuthenticated: true,
      };
      const action = { type: logoutUser.fulfilled.type };
      const newState = authReducer(authenticatedState, action);
      expect(newState.user).toBeNull();
      expect(newState.token).toBeNull();
      expect(newState.isAuthenticated).toBe(false);
    });
  });
});
