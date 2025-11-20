import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authService } from './authService';
import * as api from './api';

vi.mock('./api');

describe('AuthService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('register', () => {
    it('should call apiRequest with correct parameters', async () => {
      const mockResponse = { message: 'Registration successful' };
      vi.spyOn(api, 'apiRequest').mockResolvedValue(mockResponse);

      const registerData = {
        email: 'test@example.com',
        password: 'ValidPass123!',
        firstName: 'John',
        lastName: 'Doe',
        dateOfBirth: '1990-01-01',
      };

      const result = await authService.register(registerData);

      expect(api.apiRequest).toHaveBeenCalledWith('/auth/register', {
        method: 'POST',
        body: JSON.stringify(registerData),
      });
      expect(result).toEqual(mockResponse);
    });
  });

  describe('login', () => {
    it('should call apiRequest with correct parameters', async () => {
      const mockResponse = {
        token: 'mock-token',
        user: {
          id: '1',
          email: 'test@example.com',
          firstName: 'John',
          lastName: 'Doe',
          emailVerified: true,
        },
      };
      vi.spyOn(api, 'apiRequest').mockResolvedValue(mockResponse);

      const loginData = {
        email: 'test@example.com',
        password: 'ValidPass123!',
        rememberMe: true,
      };

      const result = await authService.login(loginData);

      expect(api.apiRequest).toHaveBeenCalledWith('/auth/login', {
        method: 'POST',
        body: JSON.stringify(loginData),
      });
      expect(result).toEqual(mockResponse);
    });
  });

  describe('verifyEmail', () => {
    it('should call apiRequest with correct parameters', async () => {
      const mockResponse = { message: 'Email verified successfully' };
      vi.spyOn(api, 'apiRequest').mockResolvedValue(mockResponse);

      const verifyData = { token: 'verify-token' };
      const result = await authService.verifyEmail(verifyData);

      expect(api.apiRequest).toHaveBeenCalledWith('/auth/verify-email', {
        method: 'POST',
        body: JSON.stringify(verifyData),
      });
      expect(result).toEqual(mockResponse);
    });
  });

  describe('forgotPassword', () => {
    it('should call apiRequest with correct parameters', async () => {
      const mockResponse = { message: 'Password reset email sent' };
      vi.spyOn(api, 'apiRequest').mockResolvedValue(mockResponse);

      const forgotData = { email: 'test@example.com' };
      const result = await authService.forgotPassword(forgotData);

      expect(api.apiRequest).toHaveBeenCalledWith('/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify(forgotData),
      });
      expect(result).toEqual(mockResponse);
    });
  });

  describe('resetPassword', () => {
    it('should call apiRequest with correct parameters', async () => {
      const mockResponse = { message: 'Password reset successful' };
      vi.spyOn(api, 'apiRequest').mockResolvedValue(mockResponse);

      const resetData = {
        token: 'reset-token',
        newPassword: 'NewPass123!',
      };
      const result = await authService.resetPassword(resetData);

      expect(api.apiRequest).toHaveBeenCalledWith('/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify(resetData),
      });
      expect(result).toEqual(mockResponse);
    });
  });

  describe('logout', () => {
    it('should call apiRequest with correct parameters', async () => {
      vi.spyOn(api, 'apiRequest').mockResolvedValue(undefined);

      await authService.logout();

      expect(api.apiRequest).toHaveBeenCalledWith('/auth/logout', {
        method: 'POST',
      });
    });
  });

  describe('refreshToken', () => {
    it('should call apiRequest with correct parameters', async () => {
      const mockResponse = { token: 'new-token' };
      vi.spyOn(api, 'apiRequest').mockResolvedValue(mockResponse);

      const result = await authService.refreshToken();

      expect(api.apiRequest).toHaveBeenCalledWith('/auth/refresh', {
        method: 'POST',
      });
      expect(result).toEqual(mockResponse);
    });
  });
});
