import { useState, FormEvent, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { registerUser, clearError, clearSuccessMessage } from '../store/slices/authSlice';
import { validateEmail, validatePassword, validateRequired, validateDateOfBirth, validatePasswordMatch, ValidationError } from '../utils/validation';
import Modal from '../components/common/Modal';

const RegisterPage = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { loading, error, successMessage } = useAppSelector((state) => state.auth);

  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    dateOfBirth: '',
    password: '',
    confirmPassword: '',
  });

  const [validationErrors, setValidationErrors] = useState<ValidationError>({});
  const [touched, setTouched] = useState<{ [key: string]: boolean }>({});

  useEffect(() => {
    return () => {
      dispatch(clearError());
      dispatch(clearSuccessMessage());
    };
  }, [dispatch]);



  useEffect(() => {
    // We no longer auto-redirect. The Modal will handle it.
    console.log('RegisterPage successMessage:', successMessage);
    console.log('Type of successMessage:', typeof successMessage);
  }, [successMessage]);

  const handleSuccessModalClose = () => {
    navigate('/login', { state: { message: 'Registration successful! Please check your email to verify your account.' } });
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));

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
      case 'firstName':
        error = validateRequired(formData.firstName, 'First name');
        break;
      case 'lastName':
        error = validateRequired(formData.lastName, 'Last name');
        break;
      case 'email':
        error = validateEmail(formData.email);
        break;
      case 'dateOfBirth':
        error = validateDateOfBirth(formData.dateOfBirth);
        break;
      case 'password':
        error = validatePassword(formData.password);
        break;
      case 'confirmPassword':
        error = validatePasswordMatch(formData.password, formData.confirmPassword);
        break;
    }

    if (error) {
      setValidationErrors((prev) => ({ ...prev, [field]: error }));
    }
  };

  const validateForm = (): boolean => {
    const errors: ValidationError = {};

    const firstNameError = validateRequired(formData.firstName, 'First name');
    if (firstNameError) errors.firstName = firstNameError;

    const lastNameError = validateRequired(formData.lastName, 'Last name');
    if (lastNameError) errors.lastName = lastNameError;

    const emailError = validateEmail(formData.email);
    if (emailError) errors.email = emailError;

    const dateOfBirthError = validateDateOfBirth(formData.dateOfBirth);
    if (dateOfBirthError) errors.dateOfBirth = dateOfBirthError;

    const passwordError = validatePassword(formData.password);
    if (passwordError) errors.password = passwordError;

    const confirmPasswordError = validatePasswordMatch(formData.password, formData.confirmPassword);
    if (confirmPasswordError) errors.confirmPassword = confirmPasswordError;

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    // Mark all fields as touched
    setTouched({
      firstName: true,
      lastName: true,
      email: true,
      dateOfBirth: true,
      password: true,
      confirmPassword: true,
    });

    if (!validateForm()) {
      return;
    }

    // Send all form data including confirmPassword for backend validation
    dispatch(registerUser(formData));
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
        <h1 className="text-3xl font-bold text-center mb-8">Create Account</h1>

        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-red-800 text-sm">{error}</p>
          </div>
        )}



        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-2">
                First Name
              </label>
              <input
                type="text"
                id="firstName"
                name="firstName"
                value={formData.firstName}
                onChange={handleChange}
                onBlur={() => handleBlur('firstName')}
                className={getInputClassName('firstName')}
                placeholder="First name"
              />
              {touched.firstName && validationErrors.firstName && (
                <p className="text-red-500 text-xs mt-1">{validationErrors.firstName}</p>
              )}
            </div>

            <div>
              <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-2">
                Last Name
              </label>
              <input
                type="text"
                id="lastName"
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                onBlur={() => handleBlur('lastName')}
                className={getInputClassName('lastName')}
                placeholder="Last name"
              />
              {touched.lastName && validationErrors.lastName && (
                <p className="text-red-500 text-xs mt-1">{validationErrors.lastName}</p>
              )}
            </div>
          </div>

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
            <label htmlFor="dateOfBirth" className="block text-sm font-medium text-gray-700 mb-2">
              Date of Birth
            </label>
            <input
              type="date"
              id="dateOfBirth"
              name="dateOfBirth"
              value={formData.dateOfBirth}
              onChange={handleChange}
              onBlur={() => handleBlur('dateOfBirth')}
              className={getInputClassName('dateOfBirth')}
            />
            {touched.dateOfBirth && validationErrors.dateOfBirth && (
              <p className="text-red-500 text-xs mt-1">{validationErrors.dateOfBirth}</p>
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
              placeholder="Create a password"
            />
            <p className="text-xs text-gray-500 mt-1">
              Must be at least 12 characters with uppercase, lowercase, number, and special character
            </p>
            {touched.password && validationErrors.password && (
              <p className="text-red-500 text-xs mt-1">{validationErrors.password}</p>
            )}
          </div>

          <div>
            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-2">
              Confirm Password
            </label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              onBlur={() => handleBlur('confirmPassword')}
              className={getInputClassName('confirmPassword')}
              placeholder="Confirm your password"
            />
            {touched.confirmPassword && validationErrors.confirmPassword && (
              <p className="text-red-500 text-xs mt-1">{validationErrors.confirmPassword}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {loading ? 'Registering...' : 'Register'}
          </button>
        </form>

        <p className="text-center mt-6 text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-600 hover:text-blue-700 font-semibold">
            Login here
          </Link>
        </p>
      </div>

      <Modal
        isOpen={!!successMessage}
        title="Registration Successful"
        message={typeof successMessage === 'string' ? successMessage : JSON.stringify(successMessage) || "Your account has been successfully created."}
        onClose={handleSuccessModalClose}
        buttonText="Go to Login"
        type="success"
      />
    </div>
  );
};

export default RegisterPage;
