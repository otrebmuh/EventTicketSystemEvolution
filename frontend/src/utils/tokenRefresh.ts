import { store } from '../store/store';
import { refreshToken, logoutUser } from '../store/slices/authSlice';

let refreshTimer: NodeJS.Timeout | null = null;

// Decode JWT to get expiration time
const decodeToken = (token: string): { exp: number } | null => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Error decoding token:', error);
    return null;
  }
};

// Calculate time until token expires (in milliseconds)
const getTimeUntilExpiry = (token: string): number => {
  const decoded = decodeToken(token);
  if (!decoded || !decoded.exp) {
    return 0;
  }
  
  const expiryTime = decoded.exp * 1000; // Convert to milliseconds
  const currentTime = Date.now();
  const timeUntilExpiry = expiryTime - currentTime;
  
  return timeUntilExpiry;
};

// Schedule token refresh 5 minutes before expiration
export const scheduleTokenRefresh = (token: string) => {
  // Clear any existing timer
  if (refreshTimer) {
    clearTimeout(refreshTimer);
  }

  const timeUntilExpiry = getTimeUntilExpiry(token);
  
  if (timeUntilExpiry <= 0) {
    // Token already expired, logout
    store.dispatch(logoutUser());
    return;
  }

  // Refresh 5 minutes before expiration (or immediately if less than 5 minutes remaining)
  const refreshTime = Math.max(timeUntilExpiry - 5 * 60 * 1000, 0);

  refreshTimer = setTimeout(async () => {
    try {
      const result = await store.dispatch(refreshToken()).unwrap();
      // Schedule next refresh with the new token
      scheduleTokenRefresh(result.token);
    } catch (error) {
      console.error('Token refresh failed:', error);
      // Logout on refresh failure
      store.dispatch(logoutUser());
    }
  }, refreshTime);
};

// Cancel scheduled token refresh
export const cancelTokenRefresh = () => {
  if (refreshTimer) {
    clearTimeout(refreshTimer);
    refreshTimer = null;
  }
};

// Initialize token refresh on app load
export const initializeTokenRefresh = () => {
  const token = localStorage.getItem('token');
  if (token) {
    scheduleTokenRefresh(token);
  }
};
