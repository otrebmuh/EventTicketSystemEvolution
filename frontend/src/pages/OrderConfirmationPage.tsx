import { useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchOrderById, clearCurrentOrder } from '../store/slices/orderSlice';
import type { RootState } from '../store/store';

const OrderConfirmationPage = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const dispatch = useAppDispatch();
  
  const { currentOrder, loading, error } = useAppSelector((state: RootState) => state.order);

  useEffect(() => {
    if (orderId) {
      dispatch(fetchOrderById(orderId));
    }

    return () => {
      dispatch(clearCurrentOrder());
    };
  }, [orderId, dispatch]);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
      case 'CONFIRMED':
        return 'text-green-600 bg-green-50';
      case 'PENDING':
        return 'text-yellow-600 bg-yellow-50';
      case 'FAILED':
      case 'CANCELLED':
        return 'text-red-600 bg-red-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <p className="mt-4 text-gray-600">Loading order details...</p>
        </div>
      </div>
    );
  }

  if (error || !currentOrder) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-2xl mx-auto">
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-8">
            {error || 'Order not found'}
          </div>
          <Link to="/dashboard" className="text-blue-600 hover:underline">
            ← Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-4xl mx-auto">
        {/* Success Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-green-100 rounded-full mb-4">
            <svg
              className="w-12 h-12 text-green-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
          <h1 className="text-3xl font-bold mb-2">Order Confirmed!</h1>
          <p className="text-gray-600">
            Your tickets have been purchased successfully
          </p>
        </div>

        {/* Order Details Card */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h2 className="text-xl font-semibold mb-2">Order Details</h2>
              <p className="text-gray-600">Order #{currentOrder.orderNumber}</p>
              <p className="text-sm text-gray-500">
                Placed on {formatDate(currentOrder.createdAt)}
              </p>
            </div>
            <span
              className={`px-4 py-2 rounded-full text-sm font-semibold ${getStatusColor(
                currentOrder.paymentStatus
              )}`}
            >
              {currentOrder.paymentStatus}
            </span>
          </div>

          {/* Order Items */}
          <div className="border-t pt-6">
            <h3 className="font-semibold mb-4">Tickets</h3>
            <div className="space-y-4">
              {currentOrder.orderItems.map((item) => (
                <div
                  key={item.id}
                  className="flex justify-between items-center py-3 border-b last:border-b-0"
                >
                  <div className="flex-1">
                    <p className="font-medium">Ticket Type ID: {item.ticketTypeId}</p>
                    <p className="text-sm text-gray-600">
                      Quantity: {item.quantity} × ${item.unitPrice.toFixed(2)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-semibold">${item.totalPrice.toFixed(2)}</p>
                    <p className="text-xs text-gray-500">
                      (incl. ${item.fees.toFixed(2)} fees)
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Price Breakdown */}
          <div className="border-t pt-6 mt-6">
            <div className="space-y-2">
              <div className="flex justify-between text-gray-700">
                <span>Subtotal</span>
                <span>${currentOrder.subtotalAmount.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-gray-700">
                <span>Service Fee</span>
                <span>${currentOrder.serviceFee.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-gray-700">
                <span>Tax</span>
                <span>${currentOrder.taxAmount.toFixed(2)}</span>
              </div>
              <div className="border-t pt-2 flex justify-between items-center text-lg font-bold">
                <span>Total</span>
                <span className="text-blue-600">
                  ${currentOrder.totalAmount.toFixed(2)} {currentOrder.currency}
                </span>
              </div>
            </div>
          </div>

          {/* Payment Method */}
          {currentOrder.paymentMethod && (
            <div className="border-t pt-6 mt-6">
              <div className="flex justify-between items-center">
                <span className="text-gray-700">Payment Method</span>
                <span className="font-medium">{currentOrder.paymentMethod}</span>
              </div>
            </div>
          )}
        </div>

        {/* Next Steps */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
          <h3 className="font-semibold text-blue-900 mb-3">What's Next?</h3>
          <ul className="space-y-2 text-blue-800">
            <li className="flex items-start gap-2">
              <span className="text-blue-600 mt-1">✓</span>
              <span>
                A confirmation email has been sent to your email address with your tickets
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-blue-600 mt-1">✓</span>
              <span>
                You can view and download your tickets from your dashboard
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-blue-600 mt-1">✓</span>
              <span>
                Present your QR code at the event entrance for quick check-in
              </span>
            </li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4">
          <Link
            to="/my-tickets"
            className="flex-1 bg-blue-600 text-white py-3 rounded-lg font-semibold text-center hover:bg-blue-700 transition"
          >
            View My Tickets
          </Link>
          <Link
            to="/events"
            className="flex-1 bg-white border-2 border-gray-300 text-gray-700 py-3 rounded-lg font-semibold text-center hover:border-gray-400 transition"
          >
            Browse More Events
          </Link>
        </div>

        {/* Support */}
        <div className="text-center mt-8 text-gray-600">
          <p>
            Need help?{' '}
            <a href="/support" className="text-blue-600 hover:underline">
              Contact Support
            </a>
          </p>
        </div>
      </div>
    </div>
  );
};

export default OrderConfirmationPage;
