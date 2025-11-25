import { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
  fetchEventsStart,
  fetchEventsSuccess,
  fetchEventsFailure,
  setSearchQuery,
  setFilters,
  setPage,
  setSuggestions,
  clearSuggestions,
} from '../store/slices/eventSlice';
import { eventService } from '../services/eventService';
import type { RootState } from '../store/store';

const EventsPage = () => {
  const dispatch = useAppDispatch();
  const { events, loading, error, searchQuery, filters, pagination, suggestions } = useAppSelector(
    (state: RootState) => state.event
  );

  const [localSearchQuery, setLocalSearchQuery] = useState(searchQuery);
  const [localCategory, setLocalCategory] = useState(filters.category || '');
  const [localCity, setLocalCity] = useState(filters.city || '');
  const [localStartDate, setLocalStartDate] = useState(filters.startDate || '');
  const [localEndDate, setLocalEndDate] = useState(filters.endDate || '');
  const [localSortBy, setLocalSortBy] = useState(filters.sortBy || 'date');
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [categories, setCategories] = useState<string[]>([]);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Load categories on mount
  useEffect(() => {
    const loadCategories = async () => {
      try {
        const cats = await eventService.getCategories();
        setCategories(cats);
      } catch (err) {
        console.error('Failed to load categories:', err);
      }
    };
    loadCategories();
  }, []);

  // Load events based on URL params or filters
  useEffect(() => {
    const loadEvents = async () => {
      dispatch(fetchEventsStart());
      try {
        const criteria = {
          query: searchQuery || undefined,
          category: filters.category || undefined,
          city: filters.city || undefined,
          startDate: filters.startDate || undefined,
          endDate: filters.endDate || undefined,
          sortBy: filters.sortBy || 'date',
          page: pagination.page,
          size: pagination.size,
        };

        const response = await eventService.getEvents(criteria);
        dispatch(
          fetchEventsSuccess({
            events: response.content,
            totalPages: response.totalPages,
            totalElements: response.totalElements,
            page: response.number,
          })
        );
      } catch (err: any) {
        dispatch(fetchEventsFailure(err.message || 'Failed to load events'));
      }
    };

    loadEvents();
  }, [dispatch, searchQuery, filters, pagination.page, pagination.size]);

  // Handle search suggestions
  useEffect(() => {
    const fetchSuggestions = async () => {
      if (localSearchQuery.length >= 2) {
        try {
          const suggestions = await eventService.getSearchSuggestions(localSearchQuery);
          dispatch(setSuggestions(suggestions));
          setShowSuggestions(true);
        } catch (err) {
          console.error('Failed to fetch suggestions:', err);
        }
      } else {
        dispatch(clearSuggestions());
        setShowSuggestions(false);
      }
    };

    const debounceTimer = setTimeout(fetchSuggestions, 300);
    return () => clearTimeout(debounceTimer);
  }, [localSearchQuery, dispatch]);

  const handleSearch = () => {
    dispatch(setSearchQuery(localSearchQuery));
    dispatch(
      setFilters({
        category: localCategory || undefined,
        city: localCity || undefined,
        startDate: localStartDate || undefined,
        endDate: localEndDate || undefined,
        sortBy: localSortBy as 'date' | 'price' | 'popularity',
      })
    );
    dispatch(setPage(0));
    setShowSuggestions(false);
  };

  const handleSuggestionClick = (suggestion: string) => {
    setLocalSearchQuery(suggestion);
    dispatch(setSearchQuery(suggestion));
    setShowSuggestions(false);
  };

  const handlePageChange = (newPage: number) => {
    dispatch(setPage(newPage));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
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

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl md:text-4xl font-bold mb-8">Browse Events</h1>

      {/* Search and Filter Section */}
      <div className="bg-white rounded-lg shadow-md p-4 md:p-6 mb-8">
        <div className="space-y-4">
          {/* Search Bar with Suggestions */}
          <div className="relative">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="flex-1 relative">
                <input
                  ref={searchInputRef}
                  type="text"
                  placeholder="Search events, venues, or cities..."
                  value={localSearchQuery}
                  onChange={(e) => setLocalSearchQuery(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                  onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                {/* Search Suggestions Dropdown */}
                {showSuggestions && suggestions.length > 0 && (
                  <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                    {suggestions.map((suggestion: any, index: number) => (
                      <button
                        key={index}
                        onClick={() => handleSuggestionClick(suggestion.value)}
                        className="w-full px-4 py-2 text-left hover:bg-gray-100 flex items-center gap-2"
                      >
                        <span className="text-gray-500 text-sm capitalize">
                          {suggestion.type}:
                        </span>
                        <span>{suggestion.label}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <button
                onClick={handleSearch}
                className="bg-blue-600 text-white px-6 py-2 rounded-lg font-semibold hover:bg-blue-700 transition whitespace-nowrap"
              >
                Search
              </button>
            </div>
          </div>

          {/* Filters */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <select
              value={localCategory}
              onChange={(e) => setLocalCategory(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">All Categories</option>
              {categories.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>

            <input
              type="text"
              placeholder="City"
              value={localCity}
              onChange={(e) => setLocalCity(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />

            <input
              type="date"
              placeholder="Start Date"
              value={localStartDate}
              onChange={(e) => setLocalStartDate(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />

            <input
              type="date"
              placeholder="End Date"
              value={localEndDate}
              onChange={(e) => setLocalEndDate(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Sort Options */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-gray-700">Sort by:</label>
              <select
                value={localSortBy}
                onChange={(e) => {
                  const newSortBy = e.target.value as 'date' | 'price' | 'popularity';
                  setLocalSortBy(newSortBy);
                  dispatch(
                    setFilters({
                      ...filters,
                      sortBy: newSortBy,
                    })
                  );
                }}
                className="px-3 py-1 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="date">Date</option>
                <option value="price">Price</option>
                <option value="popularity">Popularity</option>
              </select>
            </div>
            <div className="text-sm text-gray-600">
              {pagination.totalElements} events found
            </div>
          </div>
        </div>
      </div>

      {/* Loading State */}
      {loading && (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <p className="mt-4 text-gray-600">Loading events...</p>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-8">
          {error}
        </div>
      )}

      {/* Events Grid */}
      {!loading && !error && events.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-600 text-lg">No events found. Try adjusting your search criteria.</p>
        </div>
      )}

      {!loading && !error && events.length > 0 && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {events.map((event: any) => (
              <Link
                key={event.id}
                to={`/events/${event.id}`}
                className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-xl transition-shadow"
              >
                <div className="h-48 bg-gradient-to-br from-blue-400 to-purple-500 relative">
                  {event.imageUrl ? (
                    <img
                      src={event.imageUrl}
                      alt={event.name}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-white text-4xl">
                      🎫
                    </div>
                  )}
                  <div className="absolute top-2 right-2 bg-white px-2 py-1 rounded text-xs font-semibold text-gray-700">
                    {event.category?.name || 'Uncategorized'}
                  </div>
                </div>
                <div className="p-4">
                  <h3 className="text-xl font-semibold mb-2 line-clamp-2">{event.name}</h3>
                  <p className="text-gray-600 mb-1 flex items-center gap-1">
                    <span>📍</span>
                    {event.venue?.name || 'Unknown Venue'}
                  </p>
                  <p className="text-gray-500 text-sm mb-3 flex items-center gap-1">
                    <span>📅</span>
                    {formatDate(event.eventDate)} at {formatTime(event.eventDate)}
                  </p>
                  <div className="flex justify-between items-center">
                    <span className="text-blue-600 font-semibold">View Details</span>
                    <span className="text-gray-400">→</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>

          {/* Pagination */}
          {pagination.totalPages > 1 && (
            <div className="mt-8 flex justify-center items-center gap-2">
              <button
                onClick={() => handlePageChange(pagination.page - 1)}
                disabled={pagination.page === 0}
                className="px-4 py-2 border border-gray-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Previous
              </button>
              <div className="flex gap-2">
                {Array.from({ length: Math.min(5, pagination.totalPages) }, (_, i) => {
                  let pageNum;
                  if (pagination.totalPages <= 5) {
                    pageNum = i;
                  } else if (pagination.page < 3) {
                    pageNum = i;
                  } else if (pagination.page > pagination.totalPages - 3) {
                    pageNum = pagination.totalPages - 5 + i;
                  } else {
                    pageNum = pagination.page - 2 + i;
                  }
                  return (
                    <button
                      key={pageNum}
                      onClick={() => handlePageChange(pageNum)}
                      className={`px-4 py-2 border rounded-lg ${pagination.page === pageNum
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'border-gray-300 hover:bg-gray-50'
                        }`}
                    >
                      {pageNum + 1}
                    </button>
                  );
                })}
              </div>
              <button
                onClick={() => handlePageChange(pagination.page + 1)}
                disabled={pagination.page >= pagination.totalPages - 1}
                className="px-4 py-2 border border-gray-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default EventsPage;
