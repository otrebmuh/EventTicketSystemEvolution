import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { Event, TicketType, SearchSuggestion } from '../../services/eventService'

interface EventState {
  events: Event[]
  selectedEvent: Event | null
  ticketTypes: TicketType[]
  suggestions: SearchSuggestion[]
  loading: boolean
  error: string | null
  searchQuery: string
  filters: {
    category?: string
    startDate?: string
    endDate?: string
    city?: string
    sortBy?: 'date' | 'price' | 'popularity'
  }
  pagination: {
    page: number
    size: number
    totalPages: number
    totalElements: number
  }
}

const initialState: EventState = {
  events: [],
  selectedEvent: null,
  ticketTypes: [],
  suggestions: [],
  loading: false,
  error: null,
  searchQuery: '',
  filters: {},
  pagination: {
    page: 0,
    size: 12,
    totalPages: 0,
    totalElements: 0,
  },
}

const eventSlice = createSlice({
  name: 'event',
  initialState,
  reducers: {
    fetchEventsStart: (state) => {
      state.loading = true
      state.error = null
    },
    fetchEventsSuccess: (state, action: PayloadAction<{
      events: Event[]
      totalPages: number
      totalElements: number
      page: number
    }>) => {
      state.loading = false
      state.events = action.payload.events
      state.pagination.totalPages = action.payload.totalPages
      state.pagination.totalElements = action.payload.totalElements
      state.pagination.page = action.payload.page
    },
    fetchEventsFailure: (state, action: PayloadAction<string>) => {
      state.loading = false
      state.error = action.payload
    },
    setSelectedEvent: (state, action: PayloadAction<Event | null>) => {
      state.selectedEvent = action.payload
    },
    setTicketTypes: (state, action: PayloadAction<TicketType[]>) => {
      state.ticketTypes = action.payload
    },
    setSuggestions: (state, action: PayloadAction<SearchSuggestion[]>) => {
      state.suggestions = action.payload
    },
    setSearchQuery: (state, action: PayloadAction<string>) => {
      state.searchQuery = action.payload
    },
    setFilters: (state, action: PayloadAction<EventState['filters']>) => {
      state.filters = action.payload
    },
    setPage: (state, action: PayloadAction<number>) => {
      state.pagination.page = action.payload
    },
    clearError: (state) => {
      state.error = null
    },
    clearSuggestions: (state) => {
      state.suggestions = []
    },
  },
})

export const {
  fetchEventsStart,
  fetchEventsSuccess,
  fetchEventsFailure,
  setSelectedEvent,
  setTicketTypes,
  setSuggestions,
  setSearchQuery,
  setFilters,
  setPage,
  clearError,
  clearSuggestions,
} = eventSlice.actions
export default eventSlice.reducer
