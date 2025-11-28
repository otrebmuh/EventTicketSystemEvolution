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

// Backend wraps all responses in this format
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string | number[];
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

async function handleResponse<T>(response: Response, unwrap: boolean = true): Promise<T> {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({
      code: 'UNKNOWN_ERROR',
      message: 'An unexpected error occurred',
    }));
    throw new ApiException(response.status, errorData.error || errorData);
  }

  const responseData = await response.json();

  // Backend wraps responses in ApiResponse<T>, extract the data field
  if (unwrap && responseData && typeof responseData === 'object' && 'data' in responseData) {
    return responseData.data as T;
  }

  return responseData;
}

export interface RequestOptions extends RequestInit {
  unwrap?: boolean;
}

export async function apiRequest<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const token = localStorage.getItem('token');
  const { unwrap = true, ...fetchOptions } = options;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    // Add custom header to help prevent CSRF attacks
    'X-Requested-With': 'XMLHttpRequest',
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...fetchOptions,
    headers: {
      ...headers,
      ...(fetchOptions.headers as Record<string, string>),
    },
    // Include credentials for CORS requests
    credentials: 'include',
  });

  return handleResponse<T>(response, unwrap);
}
