import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchUserTickets } from '../store/slices/ticketSlice';
import type { RootState } from '../store/store';
import { Ticket } from '../services/ticketService';

const MyTicketsPage = () => {
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state: RootState) => state.auth);
  const { tickets, loading, error } = useAppSelector((state: RootState) => state.ticket);

  const [filter, setFilter] = useState<'all' | 'active' | 'used' | 'cancelled'>('all');

  useEffect(() => {
    if (user?.id) {
      dispatch(fetchUserTickets(user.id));
    }
  }, [user?.id, dispatch]);

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
      case 'ACTIVE':
        return 'text-green-600 bg-green-50';
      case 'USED':
        return 'text-gray-600 bg-gray-50';
      case 'CANCELLED':
        return 'text-red-600 bg-red-50';
      case 'EXPIRED':
        return 'text-orange-600 bg-orange-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };

  const getFilteredTickets = () => {
    if (filter === 'all') return tickets;
    return tickets.filter(ticket => ticket.status.toLowerCase() === filter);
  };

  const getTicketCount = (status: string) => {
    if (status === 'all') return tickets.length;
    return tickets.filter(ticket => ticket.status.toLowerCase() === status).length;
  };

  const groupTicketsByEvent = (ticketList: Ticket[]) => {
    const grouped: { [key: string]: Ticket[] } = {};
    ticketList.forEach(ticket => {
      const eventKey = ticket.eventName || 'Unknown Event';
      if (!grouped[eventKey]) {
        grouped[eventKey] = [];
      }
      grouped[eventKey].push(ticket);
    });
    return grouped;
  };

  const filteredTickets = getFilteredTickets();
  const groupedTickets = groupTicketsByEvent(filteredTickets);

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">My Tickets</h1>
        <p className="text-gray-600">View and manage all your event tickets</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="bg-white rounded-lg shadow-md p-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-1">Total Tickets</h3>
          <p className="text-2xl font-bold text-blue-600">{tickets.length}</p>
        </div>
        
        <div className="bg-white rounded-lg shadow-md p-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-1">Active</h3>
          <p className="text-2xl font-bold text-green-600">{getTicketCount('active')}</p>
        </div>
        
        <div className="bg-white rounded-lg shadow-md p-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-1">Used</h3>
          <p className="text-2xl font-bold text-gray-600">{getTicketCount('used')}</p>
        </div>
        
        <div className="bg-white rounded-lg shadow-md p-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-1">Cancelled</h3>
          <p className="text-2xl font-bold text-red-600">{getTicketCount('cancelled')}</p>
        </div>
      </div>

      {/* Tickets Section */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-semibold">All Tickets</h2>
          <Link
            to="/events"
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
          >
            Browse Events
          </Link>
        </div>

        {/* Filter Tabs */}
        <div className="flex gap-4 mb-6 border-b overflow-x-auto">
          <button
            onClick={() => setFilter('all')}
            className={`pb-3 px-4 font-medium transition whitespace-nowrap ${
              filter === 'all'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            All ({getTicketCount('all')})
          </button>
          <button
            onClick={() => setFilter('active')}
            className={`pb-3 px-4 font-medium transition whitespace-nowrap ${
              filter === 'active'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Active ({getTicketCount('active')})
          </button>
          <button
            onClick={() => setFilter('used')}
            className={`pb-3 px-4 font-medium transition whitespace-nowrap ${
              filter === 'used'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Used ({getTicketCount('used')})
          </button>
          <button
            onClick={() => setFilter('cancelled')}
            className={`pb-3 px-4 font-medium transition whitespace-nowrap ${
              filter === 'cancelled'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Cancelled ({getTicketCount('cancelled')})
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
            <p className="mt-4 text-gray-600">Loading tickets...</p>
          </div>
        )}

        {/* Empty State */}
        {!loading && filteredTickets.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            <div className="text-6xl mb-4">🎫</div>
            <p className="text-lg">
              {filter === 'all' 
                ? "You don't have any tickets yet."
                : `No ${filter} tickets found.`
              }
            </p>
            <p className="mt-2">Browse events to get started!</p>
            <Link
              to="/events"
              className="inline-block mt-4 bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition"
            >
              Explore Events
            </Link>
          </div>
        )}

        {/* Tickets List - Grouped by Event */}
        {!loading && filteredTickets.length > 0 && (
          <div className="space-y-6">
            {Object.entries(groupedTickets).map(([eventName, eventTickets]) => (
              <div key={eventName} className="border border-gray-200 rounded-lg overflow-hidden">
                {/* Event Header */}
                <div className="bg-gray-50 px-4 py-3 border-b">
                  <h3 className="font-semibold text-lg">{eventName}</h3>
                  {eventTickets[0].eventDate && (
                    <p className="text-sm text-gray-600">
                      {formatDate(eventTickets[0].eventDate)}
                    </p>
                  )}
                  {eventTickets[0].venueName && (
                    <p className="text-sm text-gray-600">
                      📍 {eventTickets[0].venueName}
                    </p>
                  )}
                </div>

                {/* Tickets for this event */}
                <div className="divide-y">
                  {eventTickets.map((ticket) => (
                    <div
                      key={ticket.id}
                      className="p-4 hover:bg-gray-50 transition"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-2">
                            <span
                              className={`px-3 py-1 rounded-full text-xs font-semibold ${getStatusColor(
                                ticket.status
                              )}`}
                            >
                              {ticket.status}
                            </span>
                            <span className="text-sm text-gray-600">
                              Ticket #{ticket.ticketNumber}
                            </span>
                          </div>
                          <div className="space-y-1 text-sm">
                            <p className="text-gray-700">
                              <span className="font-medium">Type:</span>{' '}
                              {ticket.ticketTypeName || 'General Admission'}
                            </p>
                            {ticket.venueZone && (
                              <p className="text-gray-700">
                                <span className="font-medium">Zone:</span> {ticket.venueZone}
                              </p>
                            )}
                            <p className="text-gray-700">
                              <span className="font-medium">Holder:</span> {ticket.holderName}
                            </p>
                          </div>
                        </div>

                        <div className="flex gap-2">
                          <Link
                            to={`/tickets/${ticket.id}`}
                            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition text-sm font-medium"
                          >
                            View Ticket
                          </Link>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default MyTicketsPage;
