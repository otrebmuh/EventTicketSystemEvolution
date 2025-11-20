import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { verifyEmail, clearError, clearSuccessMessage } from '../store/slices/authSlice';

const VerifyEmailPage = () => {
  const dispatch = useAppDispatch();
  const [searchParams] = useSearchParams();
  const { loading, error, successMessage } = useAppSelector((state) => state.auth);
  const [verificationAttempted, setVerificationAttempted] = useState(false);

  useEffect(() => {
    const token = searchParams.get('token');
    
    if (token && !verificationAttempted) {
      setVerificationAttempted(true);
      dispatch(verifyEmail({ token }));
    }

    return () => {
      dispatch(clearError());
      dispatch(clearSuccessMessage());
    };
  }, [searchParams, dispatch, verificationAttempted]);

  return (
    <div className="container mx-auto px-4 py-16">
      <div className="max-w-md mx-auto bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-center mb-8">Email Verification</h1>
        
        {loading && (
          <div className="text-center py-8">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            <p className="mt-4 text-gray-600">Verifying your email...</p>
          </div>
        )}

        {!loading && successMessage && (
          <div className="text-center">
            <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg">
              <svg className="w-12 h-12 text-green-500 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              <p className="text-green-800 text-sm">{successMessage}</p>
            </div>
            <Link
              to="/login"
              className="inline-block bg-blue-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700 transition"
            >
              Go to Login
            </Link>
          </div>
        )}

        {!loading && error && (
          <div className="text-center">
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <svg className="w-12 h-12 text-red-500 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
              <p className="text-red-800 text-sm">{error}</p>
            </div>
            <div className="space-y-4">
              <Link
                to="/register"
                className="inline-block bg-blue-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700 transition"
              >
                Register Again
              </Link>
              <p className="text-gray-600 text-sm">
                Already have an account?{' '}
                <Link to="/login" className="text-blue-600 hover:text-blue-700 font-semibold">
                  Login here
                </Link>
              </p>
            </div>
          </div>
        )}

        {!loading && !error && !successMessage && !verificationAttempted && (
          <div className="text-center">
            <p className="text-gray-600 mb-6">
              No verification token found. Please check your email for the verification link.
            </p>
            <Link
              to="/login"
              className="inline-block bg-blue-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700 transition"
            >
              Go to Login
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default VerifyEmailPage;
