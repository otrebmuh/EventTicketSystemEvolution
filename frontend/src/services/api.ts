// API configuration and utilities
const API_BASE_URL = '/api';

export interface ApiError {
  code: string;
  message: string;
  details?: Array<{
    field: string;
    message: string;
  }>;
}

export class ApiException extends Error {
  constructor(
    public statusCode: number,
    public error: ApiError
  ) {
    super(error.message);
    this.name = 'ApiException';
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  const responseData = await response.json().catch(() => ({
    success: false,
    message: 'An unexpected error occurred',
  }));

  if (!response.ok) {
    // Handle different error response formats
    let errorMessage = 'An unexpected error occurred';
    
    if (responseData.message) {
      errorMessage = responseData.message;
    } else if (responseData.error && responseData.error.message) {
      errorMessage = responseData.error.message;
    }
    
    const apiError = {
      code: responseData.code || 'API_ERROR',
      message: errorMessage,
      details: responseData.details
    };
    
    throw new ApiException(response.status, apiError);
  }

  // For successful responses, return the data or the whole response
  if (responseData.data !== undefined) {
    return responseData.data;
  }
  
  return responseData;
}

export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('token');
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    // Add custom header to help prevent CSRF attacks
    'X-Requested-With': 'XMLHttpRequest',
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      ...headers,
      ...(options.headers as Record<string, string>),
    },
    // Include credentials for CORS requests
    credentials: 'include',
  });

  return handleResponse<T>(response);
}
