import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
  fetchEventsStart,
  fetchEventsFailure,
  setSelectedEvent,
  setTicketTypes,
} from '../store/slices/eventSlice';
import { eventService, TicketType } from '../services/eventService';
import type { RootState } from '../store/store';

const EventDetailsPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { selectedEvent, ticketTypes, loading, error } = useAppSelector((state: RootState) => state.event);
  const { isAuthenticated } = useAppSelector((state: RootState) => state.auth);

  const [selectedTickets, setSelectedTickets] = useState<{ [key: string]: number }>({});

  useEffect(() => {
    const loadEventDetails = async () => {
      if (!id) return;

      dispatch(fetchEventsStart());
      try {
        const event = await eventService.getEventById(id);
        dispatch(setSelectedEvent(event));

        const tickets = await eventService.getTicketTypes(id);
        dispatch(setTicketTypes(tickets));
      } catch (err: any) {
        dispatch(fetchEventsFailure(err.message || 'Failed to load event details'));
      }
    };

    loadEventDetails();
  }, [id, dispatch]);

  const handleTicketQuantityChange = (ticketTypeId: string, quantity: number) => {
    setSelectedTickets((prev) => ({
      ...prev,
      [ticketTypeId]: Math.max(0, quantity),
    }));
  };

  const getTotalPrice = () => {
    return ticketTypes.reduce((total: number, ticketType: TicketType) => {
      const quantity = selectedTickets[ticketType.id] || 0;
      return total + ticketType.price * quantity;
    }, 0);
  };

  const getTotalTickets = () => {
    return Object.values(selectedTickets).reduce((sum, qty) => sum + qty, 0);
  };

  const handleProceedToCheckout = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/events/${id}` } });
      return;
    }

    if (!selectedEvent) {
      alert('Event information is not available');
      return;
    }

    // Prepare checkout data
    const selectedTicketsList = ticketTypes
      .filter((ticketType: TicketType) => selectedTickets[ticketType.id] > 0)
      .map((ticketType: TicketType) => ({
        ticketType,
        quantity: selectedTickets[ticketType.id],
      }));

    if (selectedTicketsList.length === 0) {
      alert('Please select at least one ticket');
      return;
    }

    // Navigate to checkout with state
    navigate('/checkout', {
      state: {
        eventId: selectedEvent.id,
        eventName: selectedEvent.name,
        eventDate: selectedEvent.eventDate,
        venueName: selectedEvent.venueName,
        selectedTickets: selectedTicketsList,
      },
    });
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const isTicketAvailable = (ticketType: TicketType) => {
    const now = new Date();
    const saleStart = ticketType.saleStartDate ? new Date(ticketType.saleStartDate) : null;
    const saleEnd = ticketType.saleEndDate ? new Date(ticketType.saleEndDate) : null;

    if (saleStart && now < saleStart) return false;
    if (saleEnd && now > saleEnd) return false;
    if (ticketType.quantityAvailable - ticketType.quantitySold <= 0) return false;

    return true;
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <p className="mt-4 text-gray-600">Loading event details...</p>
        </div>
      </div>
    );
  }

  if (error || !selectedEvent) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-8">
          {error || 'Event not found'}
        </div>
        <Link to="/events" className="text-blue-600 hover:underline">
          ← Back to Events
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Back Button */}
      <Link to="/events" className="text-blue-600 hover:underline mb-4 inline-block">
        ← Back to Events
      </Link>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        {/* Event Image */}
        <div className="h-64 md:h-96 bg-gradient-to-br from-blue-400 to-purple-500 relative">
          {selectedEvent.imageUrl ? (
            <img
              src={selectedEvent.imageUrl}
              alt={selectedEvent.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-white text-6xl">
              🎫
            </div>
          )}
          <div className="absolute top-4 right-4 bg-white px-3 py-1 rounded-lg text-sm font-semibold text-gray-700">
            {selectedEvent.category}
          </div>
        </div>

        {/* Event Details */}
        <div className="p-4 md:p-8">
          <h1 className="text-3xl md:text-4xl font-bold mb-4">{selectedEvent.name}</h1>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-8">
            {/* Left Column - Event Information */}
            <div className="lg:col-span-2 space-y-6">
              <div>
                <h2 className="text-2xl font-semibold mb-4">Event Information</h2>
                <div className="space-y-3 text-gray-700">
                  <div className="flex items-start gap-3">
                    <span className="text-2xl">📅</span>
                    <div>
                      <p className="font-semibold">Date & Time</p>
                      <p>{formatDate(selectedEvent.eventDate)}</p>
                      <p>{formatTime(selectedEvent.eventDate)}</p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <span className="text-2xl">📍</span>
                    <div>
                      <p className="font-semibold">Venue</p>
                      <p>{selectedEvent.venueName}</p>
                      <p className="text-sm text-gray-600">{selectedEvent.venueAddress}</p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <span className="text-2xl">🎭</span>
                    <div>
                      <p className="font-semibold">Category</p>
                      <p>{selectedEvent.category}</p>
                    </div>
                  </div>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-semibold mb-4">Description</h2>
                <p className="text-gray-700 whitespace-pre-line">{selectedEvent.description}</p>
              </div>
            </div>

            {/* Right Column - Ticket Selection */}
            <div className="lg:col-span-1">
              <div className="bg-gray-50 p-4 md:p-6 rounded-lg sticky top-4">
                <h2 className="text-2xl font-semibold mb-4">Select Tickets</h2>

                {ticketTypes.length === 0 ? (
                  <p className="text-gray-600">No tickets available for this event.</p>
                ) : (
                  <div className="space-y-4">
                    {ticketTypes.map((ticketType: TicketType) => {
                      const available = isTicketAvailable(ticketType);
                      const remainingTickets =
                        ticketType.quantityAvailable - ticketType.quantitySold;
                      const selectedQty = selectedTickets[ticketType.id] || 0;

                      return (
                        <div
                          key={ticketType.id}
                          className={`border rounded-lg p-4 ${
                            available ? 'border-gray-300 bg-white' : 'border-gray-200 bg-gray-100'
                          }`}
                        >
                          <div className="flex justify-between items-start mb-2">
                            <div className="flex-1">
                              <h3 className="font-semibold text-lg">{ticketType.name}</h3>
                              {ticketType.description && (
                                <p className="text-sm text-gray-600 mt-1">
                                  {ticketType.description}
                                </p>
                              )}
                              {ticketType.venueZone && (
                                <p className="text-sm text-gray-500 mt-1">
                                  Zone: {ticketType.venueZone}
                                </p>
                              )}
                            </div>
                            <div className="text-right ml-4">
                              <p className="text-xl font-bold text-blue-600">
                                ${ticketType.price.toFixed(2)}
                              </p>
                            </div>
                          </div>

                          {available ? (
                            <>
                              <p className="text-sm text-gray-500 mb-3">
                                {remainingTickets} tickets available
                                {ticketType.perPersonLimit && (
                                  <span> • Max {ticketType.perPersonLimit} per person</span>
                                )}
                              </p>
                              <div className="flex items-center gap-3">
                                <button
                                  onClick={() =>
                                    handleTicketQuantityChange(ticketType.id, selectedQty - 1)
                                  }
                                  disabled={selectedQty === 0}
                                  className="w-10 h-10 border border-gray-300 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                  −
                                </button>
                                <span className="w-12 text-center font-semibold">
                                  {selectedQty}
                                </span>
                                <button
                                  onClick={() =>
                                    handleTicketQuantityChange(ticketType.id, selectedQty + 1)
                                  }
                                  disabled={
                                    selectedQty >= remainingTickets ||
                                    selectedQty >= ticketType.perPersonLimit
                                  }
                                  className="w-10 h-10 border border-gray-300 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                  +
                                </button>
                              </div>
                            </>
                          ) : (
                            <p className="text-sm text-red-600 font-semibold">Not Available</p>
                          )}
                        </div>
                      );
                    })}

                    {/* Total and Checkout */}
                    {getTotalTickets() > 0 && (
                      <div className="border-t pt-4 mt-4">
                        <div className="flex justify-between items-center mb-4">
                          <span className="font-semibold">Total ({getTotalTickets()} tickets)</span>
                          <span className="text-2xl font-bold text-blue-600">
                            ${getTotalPrice().toFixed(2)}
                          </span>
                        </div>
                        <button
                          onClick={handleProceedToCheckout}
                          className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition"
                        >
                          {isAuthenticated ? 'Proceed to Checkout' : 'Login to Purchase'}
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EventDetailsPage;
