import { describe, it, expect } from 'vitest';
import {
  validateEmail,
  validatePassword,
  validateRequired,
  validateDateOfBirth,
  validatePasswordMatch,
} from './validation';

describe('Validation Utils', () => {
  describe('validateEmail', () => {
    it('should return null for valid email', () => {
      expect(validateEmail('test@example.com')).toBeNull();
      expect(validateEmail('user.name+tag@example.co.uk')).toBeNull();
    });

    it('should return error for empty email', () => {
      expect(validateEmail('')).toBe('Email is required');
    });

    it('should return error for invalid email format', () => {
      expect(validateEmail('invalid')).toBe('Please enter a valid email address');
      expect(validateEmail('test@')).toBe('Please enter a valid email address');
      expect(validateEmail('@example.com')).toBe('Please enter a valid email address');
    });
  });

  describe('validatePassword', () => {
    it('should return null for valid password', () => {
      expect(validatePassword('ValidPass123!')).toBeNull();
      expect(validatePassword('Str0ng!P@ssw0rd')).toBeNull();
    });

    it('should return error for empty password', () => {
      expect(validatePassword('')).toBe('Password is required');
    });

    it('should return error for password less than 12 characters', () => {
      expect(validatePassword('Short1!')).toContain('at least 12 characters');
    });

    it('should return error for password without uppercase', () => {
      expect(validatePassword('lowercase123!')).toContain('uppercase letter');
    });

    it('should return error for password without lowercase', () => {
      expect(validatePassword('UPPERCASE123!')).toContain('lowercase letter');
    });

    it('should return error for password without number', () => {
      expect(validatePassword('NoNumbersHere!')).toContain('number');
    });

    it('should return error for password without special character', () => {
      expect(validatePassword('NoSpecial123')).toContain('special character');
    });
  });

  describe('validateRequired', () => {
    it('should return null for non-empty value', () => {
      expect(validateRequired('value', 'Field')).toBeNull();
    });

    it('should return error for empty value', () => {
      expect(validateRequired('', 'Field')).toBe('Field is required');
    });

    it('should return error for whitespace-only value', () => {
      expect(validateRequired('   ', 'Field')).toBe('Field is required');
    });
  });

  describe('validateDateOfBirth', () => {
    it('should return null for valid date', () => {
      expect(validateDateOfBirth('1990-01-01')).toBeNull();
    });

    it('should return error for empty date', () => {
      expect(validateDateOfBirth('')).toBe('Date of birth is required');
    });

    it('should return error for future date', () => {
      const futureDate = new Date();
      futureDate.setFullYear(futureDate.getFullYear() + 1);
      const futureDateStr = futureDate.toISOString().split('T')[0];
      const error = validateDateOfBirth(futureDateStr);
      expect(error).not.toBeNull();
      expect(error).toContain('13 years old');
    });
  });

  describe('validatePasswordMatch', () => {
    it('should return null for matching passwords', () => {
      expect(validatePasswordMatch('password123', 'password123')).toBeNull();
    });

    it('should return error for non-matching passwords', () => {
      expect(validatePasswordMatch('password123', 'different')).toBe('Passwords do not match');
    });

    it('should return error for empty confirm password', () => {
      expect(validatePasswordMatch('password123', '')).toBe('Passwords do not match');
    });
  });
});
