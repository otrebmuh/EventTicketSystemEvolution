import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchUserOrders, setPage } from '../store/slices/orderSlice';
import type { RootState } from '../store/store';

const DashboardPage = () => {
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state: RootState) => state.auth);
  const { orders, loading, error, pagination } = useAppSelector((state: RootState) => state.order);

  const [activeTab, setActiveTab] = useState<'all' | 'upcoming' | 'past'>('all');

  useEffect(() => {
    if (user?.id) {
      dispatch(fetchUserOrders({ userId: user.id, page: pagination.page, size: pagination.size }));
    }
  }, [user?.id, pagination.page, dispatch]);

  const handlePageChange = (newPage: number) => {
    dispatch(setPage(newPage));
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
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

  const getTotalTickets = () => {
    return orders.reduce((total, order) => {
      return total + order.orderItems.reduce((sum, item) => sum + item.quantity, 0);
    }, 0);
  };

  const getUpcomingOrders = () => {
    return orders.filter(order => 
      order.paymentStatus === 'COMPLETED' || order.paymentStatus === 'CONFIRMED'
    );
  };

  const getPastOrders = () => {
    return orders.filter(order => 
      order.paymentStatus === 'CANCELLED' || order.paymentStatus === 'FAILED'
    );
  };

  const getFilteredOrders = () => {
    switch (activeTab) {
      case 'upcoming':
        return getUpcomingOrders();
      case 'past':
        return getPastOrders();
      default:
        return orders;
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">My Dashboard</h1>
        <p className="text-gray-600">Welcome back, {user?.firstName}!</p>
      </div>
      
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-700 mb-2">Total Orders</h3>
          <p className="text-3xl font-bold text-blue-600">{pagination.totalElements}</p>
        </div>
        
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-700 mb-2">Total Tickets</h3>
          <p className="text-3xl font-bold text-blue-600">{getTotalTickets()}</p>
        </div>
        
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-700 mb-2">Upcoming Events</h3>
          <p className="text-3xl font-bold text-blue-600">{getUpcomingOrders().length}</p>
        </div>
      </div>
      
      {/* Orders Section */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-semibold">My Orders</h2>
          <Link
            to="/events"
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
          >
            Browse Events
          </Link>
        </div>

        {/* Tabs */}
        <div className="flex gap-4 mb-6 border-b">
          <button
            onClick={() => setActiveTab('all')}
            className={`pb-3 px-4 font-medium transition ${
              activeTab === 'all'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            All Orders ({orders.length})
          </button>
          <button
            onClick={() => setActiveTab('upcoming')}
            className={`pb-3 px-4 font-medium transition ${
              activeTab === 'upcoming'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Upcoming ({getUpcomingOrders().length})
          </button>
          <button
            onClick={() => setActiveTab('past')}
            className={`pb-3 px-4 font-medium transition ${
              activeTab === 'past'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Past ({getPastOrders().length})
          </button>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6">
            {error}
          </div>
        )}

        {/* Loading State */}
        {loading && (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            <p className="mt-4 text-gray-600">Loading orders...</p>
          </div>
        )}

        {/* Orders List */}
        {!loading && getFilteredOrders().length === 0 && (
          <div className="text-center py-12 text-gray-500">
            <div className="text-6xl mb-4">🎫</div>
            <p className="text-lg">You don't have any orders yet.</p>
            <p className="mt-2">Browse events to get started!</p>
            <Link
              to="/events"
              className="inline-block mt-4 bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition"
            >
              Explore Events
            </Link>
          </div>
        )}

        {!loading && getFilteredOrders().length > 0 && (
          <div className="space-y-4">
            {getFilteredOrders().map((order) => (
              <div
                key={order.id}
                className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition"
              >
                <div className="flex justify-between items-start mb-3">
                  <div>
                    <h3 className="font-semibold text-lg">Order #{order.orderNumber}</h3>
                    <p className="text-sm text-gray-600">{formatDate(order.createdAt)}</p>
                  </div>
                  <span
                    className={`px-3 py-1 rounded-full text-sm font-semibold ${getStatusColor(
                      order.paymentStatus
                    )}`}
                  >
                    {order.paymentStatus}
                  </span>
                </div>

                <div className="space-y-2 mb-3">
                  {order.orderItems.map((item) => (
                    <div key={item.id} className="flex justify-between text-sm">
                      <span className="text-gray-700">
                        {item.quantity} × Ticket (${item.unitPrice.toFixed(2)})
                      </span>
                      <span className="font-medium">${item.totalPrice.toFixed(2)}</span>
                    </div>
                  ))}
                </div>

                <div className="flex justify-between items-center pt-3 border-t">
                  <div className="text-lg font-bold">
                    Total: ${order.totalAmount.toFixed(2)} {order.currency}
                  </div>
                  <Link
                    to={`/order-confirmation/${order.id}`}
                    className="text-blue-600 hover:underline font-medium"
                  >
                    View Details →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {!loading && pagination.totalPages > 1 && (
          <div className="flex justify-center items-center gap-2 mt-6">
            <button
              onClick={() => handlePageChange(pagination.page - 1)}
              disabled={pagination.page === 0}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <span className="text-gray-600">
              Page {pagination.page + 1} of {pagination.totalPages}
            </span>
            <button
              onClick={() => handlePageChange(pagination.page + 1)}
              disabled={pagination.page >= pagination.totalPages - 1}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardPage;
