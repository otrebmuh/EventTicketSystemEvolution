import { useState, FormEvent, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { loginUser, clearError } from '../store/slices/authSlice';
import { validateEmail, validateRequired, ValidationError } from '../utils/validation';

const LoginPage = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { loading, error, isAuthenticated } = useAppSelector((state) => state.auth);

  const [formData, setFormData] = useState({
    email: '',
    password: '',
    rememberMe: false,
  });

  const [validationErrors, setValidationErrors] = useState<ValidationError>({});
  const [touched, setTouched] = useState<{ [key: string]: boolean }>({});
  const [infoMessage, setInfoMessage] = useState<string | null>(null);
  const [showResendVerification, setShowResendVerification] = useState(false);
  const [resendingVerification, setResendingVerification] = useState(false);
  const [resendMessage, setResendMessage] = useState<string | null>(null);

  useEffect(() => {
    // Check for messages from registration or other pages
    if (location.state?.message) {
      setInfoMessage(location.state.message);
      // Clear the message from location state
      window.history.replaceState({}, document.title);
    }

    return () => {
      dispatch(clearError());
    };
  }, [dispatch, location]);

  useEffect(() => {
    if (isAuthenticated) {
      // Redirect to dashboard or intended page after successful login
      const from = location.state?.from?.pathname || '/dashboard';
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, location]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    
    // Clear validation error when user starts typing
    if (validationErrors[name]) {
      setValidationErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[name];
        return newErrors;
      });
    }
  };

  const handleBlur = (field: string) => {
    setTouched((prev) => ({ ...prev, [field]: true }));
    validateField(field);
  };

  const validateField = (field: string) => {
    let error: string | null = null;

    switch (field) {
      case 'email':
        error = validateEmail(formData.email);
        break;
      case 'password':
        error = validateRequired(formData.password, 'Password');
        break;
    }

    if (error) {
      setValidationErrors((prev) => ({ ...prev, [field]: error }));
    }
  };

  const validateForm = (): boolean => {
    const errors: ValidationError = {};

    const emailError = validateEmail(formData.email);
    if (emailError) errors.email = emailError;

    const passwordError = validateRequired(formData.password, 'Password');
    if (passwordError) errors.password = passwordError;

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    
    // Mark all fields as touched
    setTouched({
      email: true,
      password: true,
    });

    if (!validateForm()) {
      return;
    }

    try {
      const result = await dispatch(loginUser(formData)).unwrap();
      // Login successful
    } catch (err: any) {
      // Check if error is about unverified email
      if (err?.includes('verify your email') || err?.includes('email verification')) {
        setShowResendVerification(true);
      }
    }
  };

  const handleResendVerification = async () => {
    setResendingVerification(true);
    setResendMessage(null);
    
    try {
      const response = await fetch('/api/auth/resend-verification', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email: formData.email }),
      });
      
      const data = await response.json();
      
      if (data.success) {
        setResendMessage('Verification email sent! Please check your inbox.');
        setShowResendVerification(false);
      } else {
        setResendMessage(data.message || 'Failed to send verification email.');
      }
    } catch (error) {
      setResendMessage('An error occurred. Please try again.');
    } finally {
      setResendingVerification(false);
    }
  };

  const getInputClassName = (field: string) => {
    const baseClass = "w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent";
    if (touched[field] && validationErrors[field]) {
      return `${baseClass} border-red-500`;
    }
    return `${baseClass} border-gray-300`;
  };

  return (
    <div className="container mx-auto px-4 py-16">
      <div className="max-w-md mx-auto bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-center mb-8">Login</h1>
        
        {infoMessage && (
          <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-blue-800 text-sm">{infoMessage}</p>
          </div>
        )}

        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-red-800 text-sm">{error}</p>
            {showResendVerification && (
              <button
                onClick={handleResendVerification}
                disabled={resendingVerification}
                className="mt-3 text-sm text-blue-600 hover:text-blue-700 font-semibold underline disabled:text-gray-400"
              >
                {resendingVerification ? 'Sending...' : 'Resend verification email'}
              </button>
            )}
          </div>
        )}
        
        {resendMessage && (
          <div className={`mb-6 p-4 rounded-lg ${resendMessage.includes('sent') ? 'bg-green-50 border border-green-200' : 'bg-yellow-50 border border-yellow-200'}`}>
            <p className={`text-sm ${resendMessage.includes('sent') ? 'text-green-800' : 'text-yellow-800'}`}>{resendMessage}</p>
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
              Email Address
            </label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              onBlur={() => handleBlur('email')}
              className={getInputClassName('email')}
              placeholder="Enter your email"
            />
            {touched.email && validationErrors.email && (
              <p className="text-red-500 text-xs mt-1">{validationErrors.email}</p>
            )}
          </div>
          
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
              Password
            </label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              onBlur={() => handleBlur('password')}
              className={getInputClassName('password')}
              placeholder="Enter your password"
            />
            {touched.password && validationErrors.password && (
              <p className="text-red-500 text-xs mt-1">{validationErrors.password}</p>
            )}
          </div>
          
          <div className="flex items-center justify-between">
            <label className="flex items-center">
              <input
                type="checkbox"
                name="rememberMe"
                checked={formData.rememberMe}
                onChange={handleChange}
                className="mr-2"
              />
              <span className="text-sm text-gray-600">Remember me</span>
            </label>
            <Link to="/forgot-password" className="text-sm text-blue-600 hover:text-blue-700">
              Forgot password?
            </Link>
          </div>
          
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
        
        <p className="text-center mt-6 text-gray-600">
          Don't have an account?{' '}
          <Link to="/register" className="text-blue-600 hover:text-blue-700 font-semibold">
            Register here
          </Link>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
